package com.arboratum.beangen.database;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.roaringbitmap.buffer.MutableRoaringBitmap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author gpicron.
 */
class FilteredDataView<S,T> implements DataView<T> {
    private static final Cache<FilteredDataView, MutableRoaringBitmap> filterCache = CacheBuilder.newBuilder()
            .weakValues()
            .build();
    private final String name;
    private final DataSet<S> source;
    private final Function<Entry<S>, Entry<T>> predicateTranform;
    private final Class<T> targetType;
    private final boolean cacheFilter;
    private volatile boolean filterFull = false;
    private final ReadWriteLock readWriteLock;
    private final Lock readLock;
    private final Lock writeLock;


    protected static <S,T> FilteredDataView<S,T> createTransformedDataSet(DataSet<S> source, Function<S,T> transform, Class<T> targetType) {
        return new FilteredDataView<S,T>(source.getName() + "T",  source, sEntry -> new TransformedEntry<>(sEntry, transform), targetType, false);
    }

    protected static <T> FilteredDataView<T,T> createFilteredDataSet(DataSet<T> source, Predicate<T> predicate) {
        return new FilteredDataView<T,T>(source.getName() + "F", source, sEntry -> {
            T value = sEntry.lastVersion().block();
            if (predicate.test(value)) {
                return sEntry;
            } else {
                return null;
            }
        }, source.getEntryType(), true);
    }

    private FilteredDataView(String name, DataSet<S> source, Function<Entry<S>, Entry<T>> predicateTranform, Class<T> targetType, boolean cacheFilter) {
        this.name = name;
        this.source = source;
        this.predicateTranform = predicateTranform;
        this.targetType = targetType;
        this.cacheFilter = cacheFilter;
        if (cacheFilter) {
            readWriteLock = new ReentrantReadWriteLock();
            readLock = readWriteLock.readLock();
            writeLock = readWriteLock.writeLock();
            source.registerFilteredDataView(this);
        } else {
            readWriteLock = null;
            readLock = null;
            writeLock = null;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getEntryType() {
        return targetType;
    }

    @Override
    public Entry<T> selectOne(RandomSequence r) {
        return doSelectOne(r);
    }


    private Entry<T> doSelectOne(RandomSequence root) {
        final RandomSequence r = root.fork();

        if (source.getSize() == 0 || filterFull) return null;


        final MutableRoaringBitmap filter;

        if (cacheFilter) {
            try {
                filter = filterCache.get(this, MutableRoaringBitmap::new);
            } catch (ExecutionException e) {
                throw new IllegalStateException(e);
            }
        } else {
            filter = new MutableRoaringBitmap();
        }

        do {
            int elementIndex = r.nextInt(source.getLastIndex() + 1);

            final boolean skip;
            readLock.lock();
            try {
                if (filterFull) {
                    return null;
                } else {
                    skip = filter.contains(elementIndex);
                }
            } finally {
                readLock.unlock();
            }

            if (skip) continue;

            Entry<S> entry = source.get(elementIndex);

            if (entry.isDeleted()) {
                addToCache(filter, elementIndex);
                continue; // just skip
            }

            Entry<T> filteredEntry = predicateTranform.apply(entry);

            if (filteredEntry != null) {
                return filteredEntry;
            } else {
                addToCache(filter, elementIndex);
            }
        } while (true);
    }

    private void addToCache(MutableRoaringBitmap filter, int elementIndex) {
        if (cacheFilter) {
            writeLock.lock();
            try {
                filter.add(elementIndex);
                if (filter.getCardinality() == source.getSize()) {
                    filterFull = true;
                }

            } finally {
                writeLock.unlock();
            }
        } else {
            filter.add(elementIndex);

            if (filter.getCardinality() == source.getSize()) {
                writeLock.lock();
                try {
                    filterFull = true;
                } finally {
                    writeLock.unlock();
                }
            }
        }
    }



    @Override
    public Generator<T> random() {
        return new Generator<T>(getEntryType()) {
            @Override
            public T generate(RandomSequence register) {
                return doSelectOne(register).lastVersion().block();
            }
        };
    }

    @Override
    public Flux<Entry<T>> traverseDataSet(boolean includeDeleted) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public boolean canGenerateOperations() {
        throw new UnsupportedOperationException("UnsupportedOperationException on filtered view");
    }

    @Override
    public Flux<DataSet<T>.Operation> buildOperationFeed(boolean autoAck, boolean filterNonGeneratable) {
        throw new UnsupportedOperationException("UnsupportedOperationException on filtered view");
    }

    @Override
    public int getSize() {
        return source.getSize();
    }

    @Override
    public <T1> DataView<T1> transformedView(Function<T, T1> transformFunction, Class<T1> targetType) {
        return new FilteredDataView<S,T1>(source.getName() + "T", source, sEntry -> {
            Entry<T> tEntry = predicateTranform.apply(sEntry);

            if (tEntry == null) return null;

            return new TransformedEntry<>(tEntry, transformFunction);
        }, targetType, false);
    }

    @Override
    public DataView<T> filteredView(Predicate<T> acceptPredicate) {
        return new FilteredDataView<S,T>(source.getName() + "F", source, sEntry -> {
            Entry<T> tEntry = predicateTranform.apply(sEntry);
            if (tEntry == null) return null;

            T value = tEntry.lastVersion().block();
            if (acceptPredicate.test(value)) {
                return tEntry;
            } else {
                return null;
            }
        }, getEntryType(), true);
   }

    public void clearCache(Entry entry) {
        MutableRoaringBitmap filter = filterCache.getIfPresent(this);
        if (filter != null) {
            writeLock.lock();
            try {
                boolean checkedRemove = filter.checkedRemove(entry.getElementIndex());
                if (checkedRemove && filterFull) {
                    filterFull = false;
                }
            } finally {
                writeLock.unlock();
            }



        }

    }

    private static class TransformedEntry<S,T> implements Entry<T> {
        private final Entry<S> entry;
        private final Function<? super S, ? extends T> transformFunction;
        private Mono<T> lastVersion;

        public TransformedEntry(Entry<S> entry, Function<? super S, ? extends T> transformFunction) {
            this.entry = entry;
            this.transformFunction = transformFunction;
        }

        @Override
        public OpCode getLastOperation() {
            return entry.getLastOperation();
        }

        @Override
        public Mono<T> lastVersion() {
            if (lastVersion == null) lastVersion = (Mono<T>) entry.lastVersion().map(transformFunction).cache(); // cache last Version
            return lastVersion;
        }

        @Override
        public Mono<UpdateOf<T>> lastUpdate() {
            throw new UnsupportedOperationException("UnsupportedOperationException on transformed view");
        }

        @Override
        public boolean isLive() {
            return entry.isLive();
        }

        @Override
        public boolean isDeleted() {
            return entry.isDeleted();
        }

        @Override
        public int getElementIndex() {
            return entry.getElementIndex();
        }

        @Override
        public byte getElementVersion() {
            return entry.getElementVersion();
        }

        @Override
        public DataSet.EntryRef getRef() {
            DataSet.EntryRef ref = entry.getRef();
            return new DataSet.EntryRef(entry.getElementIndex(), entry.getDataSet()) {
                @Override
                public Entry getCurrent() {
                    return new TransformedEntry(ref.getCurrent(), transformFunction);
                }
            };
        }

        @Override
        public DataSet getDataSet() {
            return entry.getDataSet();
        }
    }
}
