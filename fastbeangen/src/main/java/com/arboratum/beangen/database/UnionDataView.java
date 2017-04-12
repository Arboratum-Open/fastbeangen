package com.arboratum.beangen.database;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by gpicron on 12/04/2017.
 */
class UnionDataView<T> implements DataView<T> {

    private final DataView[] children;
    private final Class<T> type;

    public UnionDataView(DataView<? extends T>... dataViews) {
        this.children = dataViews;
        final Object[] types = Stream.of(children).map(DataView::getEntryType).distinct().toArray();

        if (types.length > 1) throw new IllegalArgumentException("incompatible datasets");

        this.type = (Class<T>) types[0];
    }

    @Override
    public Class<T> getEntryType() {
        return type;
    }

    @Override
    public DataSet<T>.Entry selectOne(RandomSequence r) {
        int index = selectChildIndexRandBySize(r);

        if (index == -1) return null;

        return children[index].selectOne(r);
    }

    private int selectChildIndexRandBySize(RandomSequence r) {
        long[] ranges = new long[children.length];
        int[] nonEmptySets = new int[children.length];
        long acc = 0;
        int j = 0;
        for (int i = 0, childrenLength = children.length; i < childrenLength; i++) {
            final DataView child = children[i];
            final int size = child.getSize();
            if (size > 0) {
                acc += size;
                ranges[j] = acc;
                nonEmptySets[j] = i;
                j++;
            }
        }
        if (j == 0) return -1;


        int index = Arrays.binarySearch(ranges, 0, j, r.nextLong(acc));
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
                final DataSet<T>.Entry entry = selectOne(register);

                return (entry != null) ? entry.lastVersion().block() : null;
            }
        };
    }

    @Override
    public Flux<DataSet<T>.Entry> traverseDataSet(boolean includeDeleted) {
        return Flux.fromArray(children).flatMap(dataView -> dataView.traverseDataSet(includeDeleted));
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
                int index = UnionDataView.this.selectChildIndexRandBySize(r);

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
}
