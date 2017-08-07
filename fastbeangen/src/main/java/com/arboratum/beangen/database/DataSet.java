package com.arboratum.beangen.database;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.AtomicBitSet;
import com.arboratum.beangen.util.RandomSequence;
import com.google.common.primitives.Bytes;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.TopicProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.Function;

/**
 * Created by gpicron on 15/12/2016.
 */
public class DataSet<ENTRY> implements DataView<ENTRY> {
    private final Generator<ENTRY> entryGenerator;
    private final UpdateGenerator<ENTRY> updateGenerator;
    private final CreateTrigger<ENTRY>[] createTriggers;
    private final UpdateTrigger<ENTRY>[] updateTriggers;
    private final Scheduler scheduler;
    private final int offset;


    public static <T> DataSetBuilder<T> builder() {
        return new DataSetBuilder<T>();
    }

    @Override
    public Class<ENTRY> getEntryType() {
        return entryGenerator.getType();
    }

    @Override
    public Entry selectOne(RandomSequence r) {
        byte version;
        int elementIndex;
        do {
            if (size == 0) return null;
            elementIndex = r.nextInt(lastIndex + 1);
            version = versions[elementIndex];
        } while (version < 0);

        return new Entry(elementIndex, version);
    }

    public Entry get(int elementIndex) {
        return new Entry( elementIndex, versions[elementIndex]);
    }

    @Override
    public Generator<ENTRY> random() {
        return new Generator<ENTRY>(getEntryType()) {
            @Override
            public ENTRY generate(RandomSequence register) {
                return selectOne(register).lastVersion().block();
            }
        };
    }

    // marker op when not able to generate a valid op
    private final Operation NON_GENERATABLE_FOR_ID = new Operation( -1, null);

    public class Entry {
        private final int elementIndex;
        private final byte elementVersion;

        private Entry(int elementIndex, byte elementVersion) {
            this.elementIndex = elementIndex;
            this.elementVersion = elementVersion;
        }

        public OpCode getLastOperation() {
            if (elementVersion < 0) return OpCode.DELETE;
            if (elementVersion == 1) return OpCode.CREATE;
            return OpCode.UPDATE;
        }

        @Override
        public String toString() {
            return "DataSetEntry{" +
                    "elementIndex=" + elementIndex +
                    ", elementVersion=" + elementVersion +
                    '}';
        }

        public Flux<ENTRY> allVersions() {
            int v = Math.abs(elementVersion);
            final int elementIndex = this.elementIndex;
            final Generator<ENTRY> entryGenerator = DataSet.this.entryGenerator;
            final CreateTrigger<ENTRY>[] createTriggers = DataSet.this.createTriggers;

            return Flux.create(entryFluxSink -> {
                final ENTRY value = entryGenerator.generate(elementIndex+offset);
                if (createTriggers != null) {
                    for (CreateTrigger<ENTRY> trigger : createTriggers) {
                        trigger.apply(elementIndex, value);
                    }
                }

                entryFluxSink.next(value);
                if (v > 1) {
                    final RandomSequence seq = new RandomSequence(elementIndex+offset);

                    for (byte i = 2; i <= v; i++) {
                        final UpdateOf<ENTRY> update = updateGenerator.generate(value, seq);

                        if (updateTriggers != null) {
                            for (UpdateTrigger<ENTRY> trigger : updateTriggers) {
                                trigger.apply(elementIndex, i, update);
                            }
                        }

                        update.apply(value);

                        entryFluxSink.next(value);
                    }
                }

                entryFluxSink.complete();
            });


        }

        public Mono<ENTRY> lastVersion() {
            return allVersions().last();
        }

        public boolean isLive() {
            return elementVersion > 0;
        }
        public boolean isDeleted() {
            return elementVersion <= 0;
        }

        public int getElementIndex() {
            return elementIndex;
        }

        public byte getElementVersion() {
            return elementVersion;
        }

    }

    public class Operation {
        private final int sequenceId;
        private final Entry entry;

        public Operation(int sequenceId, Entry entry) {
            this.sequenceId = sequenceId;
            this.entry = entry;
        }

        public int getSequenceId() {
            return sequenceId;
        }

        public Entry getEntry() {
            return entry;
        }

        public void ack() {
            operationAcks.onNext(this);
        }

        public void synchronousAck() {
            final Entry entry = this.getEntry();

            if (locks.clear(entry.elementIndex)) {
                final OpCode opCode = entry.getLastOperation();
                versions[entry.elementIndex] = entry.elementVersion;
                switch (opCode) {
                    case CREATE:
                        size++;
                        break;
                    case DELETE: {
                        size--;
                        break;
                    }
                }
            } else {
                throw new IllegalStateException("The operation was acked 2 times :" + this);
            }
        }
    }

    private final TopicProcessor<Operation> operationAcks = TopicProcessor.share("Operation-Acks", 8);

    private final Generator<OpCode> operationGenerator;

    private volatile byte[] versions;
    private AtomicBitSet locks = new AtomicBitSet();
    private volatile int lastIndex = -1;
    private volatile int size;

    private boolean feedBuilt =  false;

    DataSet(Generator<OpCode> operationGenerator, byte[] versions, int lastIndex, Generator<ENTRY> entryGenerator, UpdateGenerator<ENTRY> updateGenerator, CreateTrigger<ENTRY>[] createTriggers, UpdateTrigger<ENTRY>[] updateTriggers, Scheduler scheduler, int offset) {
        this.entryGenerator = entryGenerator;
        this.versions = versions;
        this.lastIndex = lastIndex;
        this.size = countActive(versions);
        this.operationGenerator = operationGenerator;
        this.updateGenerator = updateGenerator;
        this.createTriggers = createTriggers;
        this.updateTriggers = updateTriggers;
        this.scheduler = scheduler;
        this.offset = offset;
    }

    private int countActive(byte[] versions) {
        int c = 0;
        for (byte v : versions) {
            if (v > 0) c++;
        }
        return c;
    }

    DataSet(Generator<OpCode> operationGenerator) {
        this(operationGenerator, new byte[0], -1, null, null, null, null, Schedulers.single(), 0);
    }


    @Override
    public Flux<Entry> traverseDataSet(boolean includeDeleted) {
        Flux<Entry> entryFlux = Flux.range(0, lastIndex + 1).map(i -> {
            byte v = versions[i];

            return new Entry( i, v);
        });
        if (!includeDeleted) entryFlux = entryFlux.filter(Entry::isLive);
        return entryFlux;
    }


    @Override
    public Flux<Operation> buildOperationFeed(boolean autoAck) {
        if (feedBuilt) throw new IllegalStateException("A feed can be built only once");
        feedBuilt =  true;
        if (!autoAck) operationAcks.subscribe(Operation::synchronousAck);  // this guaranty to have a single thread for this

        Flux<Operation> operationFlux = Flux.range(0, Integer.MAX_VALUE)
                .map(new Function<Integer, Operation>() {

                         @Override
                         public Operation apply(Integer id) {
                             final OpCode opCode = operationGenerator.generate(id);
                             switch (opCode) {
                                 case CREATE:
                                     final int index = lastIndex + 1;
                                     versions = Bytes.ensureCapacity(versions, index + 1, 1024);
                                     lastIndex = index;

                                     locks.set(index);

                                     return new Operation(id, new Entry(index, (byte) 1));
                                 case UPDATE:
                                 case DELETE: {
                                     if (lastIndex == -1 || size == 0) return NON_GENERATABLE_FOR_ID;  // no entry yet or no more entry

                                     // search random existing entry
                                     final RandomSequence randomSequence = new RandomSequence(id);
                                     int select = randomSequence.nextInt(lastIndex + 1);
                                     locks.waitClear(select);
                                     while (versions[select] <= 0) {
                                         if (size == 0) return NON_GENERATABLE_FOR_ID;  // no more entry
                                         select = randomSequence.nextInt(lastIndex + 1); // skip deleted
                                         locks.waitClear(select);
                                     }

                                     byte version;
                                     if (opCode == OpCode.UPDATE) {
                                         version = (byte) (versions[select] + 1);
                                     } else {
                                         version = (byte) -versions[select];
                                     }

                                     locks.waitClearAndSet(select);

                                     return new Operation(id, new Entry(select, version));
                                 }
                                 default:
                                     throw new RuntimeException("This should never occur");
                             }
                         }
                     }
                )
                .filter(op -> op != NON_GENERATABLE_FOR_ID)
                .subscribeOn(scheduler);
        if (autoAck) operationFlux = operationFlux.doOnNext(Operation::synchronousAck);
        return operationFlux;
    }

    private Mono<ENTRY> getEntry(int id) {
        final byte version = versions[id];
        if (version < 0) return Mono.empty();

        return new Entry( id, version).lastVersion();
    }

    byte[] getVersions() {
        return versions;
    }

    int getLastIndex() {
        return lastIndex;
    }

    @Override
    public int getSize() {
        return size;
    }

}
