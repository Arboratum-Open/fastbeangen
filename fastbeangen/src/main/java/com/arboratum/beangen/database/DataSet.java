package com.arboratum.beangen.database;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import com.google.common.primitives.Bytes;
import org.roaringbitmap.RoaringBitmap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.publisher.TopicProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static com.arboratum.beangen.BaseBuilders.enumerated;

/**
 * Created by gpicron on 15/12/2016.
 */
public class DataSet<ENTRY> implements DataView<ENTRY> {
    private final Generator<ENTRY> entryGenerator;
    private final Predicate<ENTRY> creationCheck;
    private final Predicate<ENTRY> deletionCheck;
    private final UpdateGenerator<ENTRY>[] updateGenerators;
    private final Generator<Integer> updateGeneratorsIndexGenerator;
    private final CreateTrigger<ENTRY>[] createTriggers;
    private final UpdateTrigger<ENTRY>[] updateTriggers;
    private final Scheduler scheduler;
    private final int offset;
    private final int numUpdateGenerators;
    private FilteredDataView[] filteredViews;


    public static <T> DataSetBuilder<T> builder() {
        return new DataSetBuilder<T>();
    }

    @Override
    public Class<ENTRY> getEntryType() {
        return entryGenerator.getType();
    }

    @Override
    public EntryImpl selectOne(RandomSequence r) {
        byte version;
        int elementIndex;
        do {
            if (size == 0) return null;
            elementIndex = r.nextInt(lastIndex + 1);
            version = versions[elementIndex];
        } while (version < 0);

        return new EntryImpl(elementIndex, version);
    }

    public Entry get(int elementIndex) {
        return new EntryImpl( elementIndex, versions[elementIndex]);
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

    public void registerFilteredDataView(FilteredDataView<ENTRY, ?> filteredDataView) {
        if (this.filteredViews == null) {
            this.filteredViews = new FilteredDataView[1];
            this.filteredViews[0] = filteredDataView;
        } else {
            int length = filteredViews.length;
            this.filteredViews = Arrays.copyOf(filteredViews, length + 1);
            filteredViews[length] = filteredDataView;
        }

    }


    public static class EntryRef<ENTRY> {
        private final int elementIndex;
        private final DataSet<ENTRY> dataSet;

        EntryRef(int elementIndex, DataSet dataSet) {
            this.elementIndex = elementIndex;
            this.dataSet = dataSet;
        }

        public Entry<ENTRY> getCurrent() {
            return dataSet.get(elementIndex);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntryRef<?> entryRef = (EntryRef<?>) o;
            return elementIndex == entryRef.elementIndex &&
                    Objects.equals(dataSet, entryRef.dataSet);
        }

        @Override
        public int hashCode() {
            return Objects.hash(elementIndex, dataSet);
        }
    }



    private class EntryImpl extends AbstractEntry<ENTRY> implements Entry<ENTRY> {
        private final int elementIndex;
        private final byte elementVersion;

        private EntryImpl(int elementIndex, byte elementVersion) {
            this.elementIndex = elementIndex;
            this.elementVersion = elementVersion;
        }

        @Override
        public OpCode getLastOperation() {
            if (elementVersion < 0) return OpCode.DELETE;
            if (elementVersion == 1) return OpCode.CREATE;
            return OpCode.UPDATE;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "elementIndex=" + elementIndex +
                    ", elementVersion=" + elementVersion +
                    '}';
        }

        protected EntryVersion<ENTRY> buildAllUpdatesHistory() {
            int v = Math.abs(elementVersion);
            final int elementIndex = this.elementIndex;
            final Generator<ENTRY> entryGenerator = DataSet.this.entryGenerator;
            final CreateTrigger<ENTRY>[] createTriggers = DataSet.this.createTriggers;

            int rootId = elementIndex + offset;
            final ENTRY value = entryGenerator.generate(rootId);
            int lastUpdateGeneratorIndex = -1;
            UpdateOf<ENTRY> lastUpdateOf = null;
            if (createTriggers != null) {
                for (CreateTrigger<ENTRY> trigger : createTriggers) {
                    trigger.apply(elementIndex, value);
                }
            }


            if (v > 1) {
                final RandomSequence seq = new RandomSequence(rootId);

                for (byte i = 2; i <= v; i++) {
                    UpdateGenerator updateGenerator = null;
                    int updateGeneratorIndex = -1;
                    for (int j = 0; j < numUpdateGenerators; j++) {
                        updateGeneratorIndex = (j + rootId) % numUpdateGenerators;
                        updateGenerator = updateGenerators[updateGeneratorIndex];
                        if (updateGenerator.canApplyOn(value)) {
                            break;
                        } else {
                            updateGenerator = null;
                        }
                    }
                    // no update possible any more return -1 on index and break
                    if (updateGenerator == null) {
                        lastUpdateOf = null;
                        lastUpdateGeneratorIndex = -2;
                        break;
                    } else {
                        final UpdateOf<ENTRY> update = updateGenerator.generate(value, seq);

                        if (updateTriggers != null) {
                            for (UpdateTrigger<ENTRY> trigger : updateTriggers) {
                                trigger.apply(elementIndex, i, update);
                            }
                        }

                        update.apply(value);

                        lastUpdateOf = update;
                        lastUpdateGeneratorIndex = updateGeneratorIndex;
                    }
                }
            }

            return new EntryVersion(lastUpdateOf, value, lastUpdateGeneratorIndex);
        }

        @Override
        public boolean isLive() {
            return elementVersion > 0;
        }
        @Override
        public boolean isDeleted() {
            return elementVersion <= 0;
        }

        @Override
        public int getElementIndex() {
            return elementIndex;
        }

        @Override
        public byte getElementVersion() {
            return elementVersion;
        }

        @Override
        public EntryRef getRef() {
            return new EntryRef(elementIndex, DataSet.this);
        }

        @Override
        public DataSet getDataSet() {
            return DataSet.this;
        }
    }


    public class Operation {
        private final int sequenceId;
        private final EntryImpl entry;
        private boolean toAck = true;

        public Operation(int sequenceId, EntryImpl entry) {
            this.sequenceId = sequenceId;
            this.entry = entry;
        }

        public int getSequenceId() {
            return sequenceId;
        }

        public Entry<ENTRY> getEntry() {
            return entry;
        }

        public void ack() {
            operationAcks.onNext(this);
        }

        private void synchronousAck() {
            if (toAck) {
                final OpCode opCode = entry.getLastOperation();
                versions = Bytes.ensureCapacity(versions, entry.elementIndex+1, 1024);
                versions[entry.elementIndex] = entry.elementVersion;
                if (entry.elementVersion == 1) lastIndex = Math.max(lastIndex, entry.elementIndex);
                switch (opCode) {
                    case CREATE: {
                        size++;
                        break;
                    }
                    case DELETE: {
                        size--;
                        break;
                    }
                    case UPDATE: {
                        if (filteredViews != null) {
                            for (FilteredDataView f : filteredViews) {
                                f.clearCache(entry);
                            }
                        }
                    }
                }
            } else {
                throw new IllegalStateException("The operation was acked 2 times :" + this);
            }
        }

        @Override
        public String toString() {
            return "Operation{" +
                    "sequenceId=" + sequenceId +
                    ", entry=" + entry +
                    '}';
        }

        public boolean isInvalidOperation() {
            return this == NON_GENERATABLE_FOR_ID;
        }
    }

    private final TopicProcessor<Operation> operationAcks = TopicProcessor.share("Operation-Acks", 8);

    private final Generator<OpCode> operationGenerator;

    private volatile byte[] versions;
    private volatile int lastIndex = -1;
    private volatile int size;

    private boolean feedBuilt =  false;

    DataSet(Generator<OpCode> operationGenerator, Predicate<ENTRY> creationCheck, Predicate<ENTRY> deletionCheck, byte[] versions, int lastIndex, Generator<ENTRY> entryGenerator, List<DataSetBuilder.WeightedUpdateGenerator<ENTRY>> updateGenerators, CreateTrigger<ENTRY>[] createTriggers, UpdateTrigger<ENTRY>[] updateTriggers, Scheduler scheduler, int offset) {
        this.creationCheck = creationCheck;
        this.deletionCheck = deletionCheck;
        this.entryGenerator = entryGenerator;
        this.versions = versions;
        this.lastIndex = lastIndex;
        this.size = countActive(versions);
        this.operationGenerator = operationGenerator;
        this.updateGenerators = updateGenerators.stream().map(DataSetBuilder.WeightedUpdateGenerator::getUpdateGenerator).toArray(UpdateGenerator[]::new);
        numUpdateGenerators = updateGenerators.size();
        this.updateGeneratorsIndexGenerator = (numUpdateGenerators == 0) ? null : enumerated(Integer.class)
                .values(IntStream.range(0, numUpdateGenerators).boxed().toArray(Integer[]::new))
                .weights(updateGenerators.stream().mapToDouble(DataSetBuilder.WeightedUpdateGenerator::getWeight).toArray())
                .build();
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
        this(operationGenerator, null, null, new byte[0], -1, null, null, null, null, Schedulers.single(), 0);
    }


    @Override
    public Flux<Entry<ENTRY>> traverseDataSet(boolean includeDeleted) {
        Flux<Entry<ENTRY>> entryFlux = Flux.range(0, lastIndex + 1).map(i -> {
            byte v = versions[i];

            return new EntryImpl( i, v);
        });
        if (!includeDeleted) entryFlux = entryFlux.filter(Entry::isLive);
        return entryFlux;
    }


    private static class DataSetFutureState {
        private int id = 0;
        private int lastIndex;
        private byte[] versions;
        private byte[] nextUpdateCache;
        private int size;
        private RoaringBitmap existing;

        public DataSetFutureState(int lastIndex, byte[] versions) {
            this.lastIndex = lastIndex;
            this.versions = Arrays.copyOf(versions, versions.length);
            this.nextUpdateCache = new byte[versions.length];
            this.existing = new RoaringBitmap();
            for (int i = 0; i < lastIndex; i++) {
                if (versions[i] > 0) existing.add(i);
            }
            this.size = existing.getCardinality();
        }
    }

    @Override
    public Flux<Operation> buildOperationFeed(boolean autoAck, boolean filterNonGeneratable) {
        if (feedBuilt) throw new IllegalStateException("A feed can be built only once");
        feedBuilt =  true;
        if (!autoAck) operationAcks.subscribe(Operation::synchronousAck);  // this guaranty to have a single thread for this

        Flux<Operation> operationFlux = Flux.generate(new Callable<DataSetFutureState>() {
                                                          @Override
                                                          public DataSetFutureState call() throws Exception {
                                                              return new DataSetFutureState(lastIndex, versions);
                                                          }
                                                      },
                new BiFunction<DataSetFutureState, SynchronousSink<Operation>, DataSetFutureState>() {
                    @Override
                    public DataSetFutureState apply(DataSetFutureState state, SynchronousSink<Operation> synchronousSink) {
                        int id = state.id;
                        int lastIndex = state.lastIndex;
                        int size = state.size;

                        RandomSequence randomSequence = new RandomSequence((long) id);
                        final OpCode opCode = (lastIndex == -1 || size == 0) ? OpCode.CREATE : operationGenerator.generate(randomSequence);
                        switch (opCode) {
                            case CREATE: {
                                int index = lastIndex + 1;

                                EntryImpl entry = new EntryImpl(index, (byte) 1);

                                if (creationCheck != null) {
                                    while (!creationCheck.test(entry.lastVersion().block())) {
                                        index++;
                                        entry = new EntryImpl(index, (byte) 1);
                                    }
                                }

                                state.versions = Bytes.ensureCapacity(state.versions, index + 1, 1024);
                                state.versions[index] = 1;
                                state.nextUpdateCache = Bytes.ensureCapacity(state.nextUpdateCache, index + 1, 1024);
                                state.size++;
                                state.lastIndex = index;
                                state.existing.add(index);

                                synchronousSink.next(new Operation(id, entry));

                                break;
                            }
                            case UPDATE: {
                                int nextUpdateWanted = updateGeneratorsIndexGenerator.generate(randomSequence);

                                // search random existing entry on which this update can apply
                                RoaringBitmap toCheck = state.existing.clone();
                                int toCheckSize = toCheck.getCardinality();
                                int updateGeneratorIndex = -1;
                                int select = -1;
                                byte version;
                                EntryImpl entry;
                                do {
                                    select = toCheck.select(randomSequence.nextInt(toCheckSize));

                                    version = (byte) (state.versions[select] + 1);
                                    entry = new EntryImpl(select, version);

                                    updateGeneratorIndex = state.nextUpdateCache[select] - 1;
                                    if (updateGeneratorIndex == -1) { // not cached
                                        // I need to try the update to ensure it is the one I'm looking for
                                        updateGeneratorIndex = entry.lastUpdateGeneratorIndex().block();
                                        state.nextUpdateCache[select] = (byte) (updateGeneratorIndex + 1);
                                    }

                                    if (updateGeneratorIndex != nextUpdateWanted) {
                                        select = -1;
                                        toCheck.remove(select);
                                        toCheckSize--;
                                    }
                                } while (select < 0 && toCheckSize > 0);

                                if (select >= 0) {
                                    state.versions[select] = version;
                                    state.nextUpdateCache[select] = 0;

                                    synchronousSink.next(new Operation(id, entry));
                                } else {
                                    synchronousSink.next(NON_GENERATABLE_FOR_ID);
                                }

                                break;
                            }
                            case DELETE: {
                                if (state.size == 0) {
                                    synchronousSink.next(NON_GENERATABLE_FOR_ID);
                                } else {
                                    int select = state.existing.select(randomSequence.nextInt(state.size));

                                    byte version = (byte) -state.versions[select];
                                    state.versions[select] = version;
                                    state.existing.remove(select);
                                    state.size--;

                                    EntryImpl entry = new EntryImpl(select, version);

                                    if (deletionCheck != null) {
                                        while (entry != null && !deletionCheck.test(entry.lastVersion().block())) {
                                            if (state.size == 0) {
                                                entry = null;
                                            } else {
                                                select = state.existing.select(randomSequence.nextInt(state.size));

                                                version = (byte) -state.versions[select];
                                                state.versions[select] = version;
                                                state.existing.remove(select);
                                                state.size--;

                                                entry = new EntryImpl(select, version);
                                            }
                                        }

                                        if (entry == null) {
                                            synchronousSink.next(NON_GENERATABLE_FOR_ID);
                                        } else {
                                            synchronousSink.next(new Operation(id, entry));
                                        }
                                    } else {
                                        synchronousSink.next(new Operation(id, entry));
                                    }
                                }
                                break;
                            }
                            default:
                                throw new RuntimeException("This should never occur");
                        }

                        state.id++;

                        return state;
                    }
                }
        )

                .subscribeOn(scheduler);

        if (filterNonGeneratable) operationFlux = operationFlux.filter(operation -> !operation.isInvalidOperation());
        if (autoAck) operationFlux = operationFlux.doOnNext(Operation::synchronousAck);
        return operationFlux;
    }

    private Mono<ENTRY> getEntry(int id) {
        final byte version = versions[id];
        if (version < 0) return Mono.empty();

        return new EntryImpl( id, version).lastVersion();
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

    @Override
    public <T> DataView<T> transformedView(Function<ENTRY, T> transformFunction, Class<T> targetType) {
        return FilteredDataView.createTransformedDataSet(this, transformFunction, targetType);
    }

    @Override
    public DataView<ENTRY> filteredView(Predicate<ENTRY> acceptPredicate) {
        return FilteredDataView.createFilteredDataSet(this, acceptPredicate);
    }

}
