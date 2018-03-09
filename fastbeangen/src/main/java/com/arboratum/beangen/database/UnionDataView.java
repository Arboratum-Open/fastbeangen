package com.arboratum.beangen.database;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import com.arboratum.beangen.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by gpicron on 12/04/2017.
 */
class UnionDataView<T> implements DataView<T> {

    private final DataView[] children;
    private final Class<T> type;

    public UnionDataView(Class<T> type, DataView<? extends T>... dataViews) {
        this.children = dataViews;
        this.type = type;
    }

    public UnionDataView(DataView<? extends T>... dataViews) {
        this.children = dataViews;
        final List<Class<?>> types = ReflectionUtils.commonSuperClass(Stream.of(children).map(DataView::getEntryType).distinct().toArray(Class[]::new));

        this.type = (Class<T>) types.get(0);
    }

    @Override
    public Class<T> getEntryType() {
        return type;
    }

    @Override
    public Entry<T> selectOne(RandomSequence r) {

        int index = selectChildIndexRandBySize(r, null);

        if (index == -1) return null;

        Entry entry = children[index].selectOne(r);

        if (entry == null) {
            final BitSet exclude = new BitSet();
            while (entry == null) {
                exclude.set(index);

                index = selectChildIndexRandBySize(r, exclude);

                if (index == -1) return null;

                entry = children[index].selectOne(r);
            }
        }
        return entry;
    }

    private int selectChildIndexRandBySize(RandomSequence r, final BitSet exclude) {
        long[] ranges = new long[children.length];
        int[] nonEmptySets = new int[children.length];
        long acc = 0;
        int j = 0;
        for (int i = 0, childrenLength = children.length; i < childrenLength; i++) {
            final DataView child = children[i];
            final int size = child.getSize();
            if (size > 0 && (exclude == null || !exclude.get(i))) {
                acc += size;
                ranges[j] = acc - 1;
                nonEmptySets[j] = i;
                j++;
            }
        }
        if (j == 0) return -1;


        long rnd = r.nextLong(acc);
        int index = Arrays.binarySearch(ranges, 0, j, rnd);
        if (index < 0) {
            index = -index - 1;
        }
        return nonEmptySets[index];
    }

    @Override
    public Generator<T> random() {
        return new Generator<T>(getEntryType()) {
            @Override
            public T generate(RandomSequence register) {
                final Entry<T> entry = selectOne(register);

                return (entry != null) ? entry.lastVersion().block() : null;
            }
        };
    }

    @Override
    public Flux<Entry<T>> traverseDataSet(boolean includeDeleted) {
        return Flux.fromArray(children).concatMap(dataView -> dataView.traverseDataSet(includeDeleted));
    }

    @Override
    public Flux<DataSet<T>.Operation> buildOperationFeed(boolean autoAck) {
        final Iterator<DataSet<T>.Operation>[] feeds = Stream.of(children)
                .map(v -> v.buildOperationFeed(autoAck))
                .map((operationFlux) -> operationFlux.toIterable(1))
                .map(Iterable::iterator)
                .toArray(Iterator[]::new);

        return Flux.generate(new Consumer<SynchronousSink<DataSet<T>.Operation>>() {
            final RandomSequence r = new RandomSequence(0);

            @Override
            public void accept(SynchronousSink<DataSet<T>.Operation> fluxSink) {
                int index = UnionDataView.this.selectChildIndexRandBySize(r, null);

                final Iterator<DataSet<T>.Operation> feed = (index == -1) ? feeds[r.nextInt(feeds.length)] : feeds[index];

                try {
                    if (feed.hasNext()) {
                        fluxSink.next(feed.next());
                    }
                } catch (Exception e) {
                    if (e instanceof IllegalStateException) { // caused when we stop consuming
                        fluxSink.complete();
                    } else {
                        fluxSink.error(e);
                    }

                }
            }
        });
    }

    @Override
    public int getSize() {
        return Stream.of(children).mapToInt(DataView::getSize).sum();
    }

    @Override
    public <U> DataView<U> transformedView(Function<T, U> transformFunction, Class<U> targetType) {
        final DataView<U>[] newChildren = Arrays.stream(children)
                .map(v -> v.transformedView(transformFunction, targetType))
                .toArray(DataView[]::new);

        return new UnionDataView<>(targetType, newChildren);
    }

    @Override
    public DataView<T> filteredView(Predicate<T> acceptPredicate) {
        final DataView<T>[] newChildren = Arrays.stream(children)
                .map(v -> v.filteredView(acceptPredicate))
                .toArray(DataView[]::new);
        return new UnionDataView<>(getEntryType(), newChildren);
    }
}
