package com.arboratum.beangen.database;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import com.google.common.base.Stopwatch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by gpicron on 14/02/2017.
 */
public class Database {
    private Map<String, DataSetBuilder> dataSetBuilders = new HashMap<>();
    private Map<String, DataView> dataSets = new HashMap<>();


    public <T> void addDataSet(String name, DataSetBuilder<T> dataset) {
        dataSetBuilders.put(name, dataset);
    }

    public <T> DataView createUnionView(String name, String... names) {
        final DataView[] dataViews = Stream.of(names).map(this::getDataView).toArray(DataView[]::new);
        final UnionView<T> tUnionView = new UnionView<>(dataViews);
        dataSets.put(name, tUnionView);
        return tUnionView;
    }

    public <T> DataView<T> getDataView(String dataSetName) {
        DataView dataSet = dataSets.get(dataSetName);
        if (dataSet == null) {
            final DataSetBuilder builder = dataSetBuilders.remove(dataSetName);
            if (builder == null) {
                throw new IllegalArgumentException("Dataset with name '"+dataSetName+"' not defined");
            }
            dataSet = builder.build();
            dataSets.put(dataSetName, dataSet);
        }
        return dataSet;
    }



    public <T> void onCreate(String dataSet, DataSet.CreateTrigger<T> trigger) {
        dataSetBuilders.get(dataSet).onCreate(trigger);
    }
    public <T> void onUpdate(String dataSet, DataSet.UpdateTrigger<T> trigger) {
        dataSetBuilders.get(dataSet).onUpdate(trigger);
    }

    public void initialize() {
        Stopwatch stopwatch = Stopwatch.createStarted();

        long count = 0;
        Optional<Map.Entry<String, DataSetBuilder>> first = dataSetBuilders.entrySet().stream().findFirst();
        while (first.isPresent()) {
            final Map.Entry<String, DataSetBuilder> builder = first.get();
            final DataSet dataSet = builder.getValue().build();
            count += dataSet.getSize();
            dataSets.put(builder.getKey(), dataSet);
            dataSetBuilders.remove(builder.getKey());
            first = dataSetBuilders.entrySet().stream().findFirst();
        }

        System.out.println("Initialize database with total entries: " + count + " in " + stopwatch.stop());
    }

    private static class UnionView<T> implements DataView<T> {

        private final DataView[] children;
        private final Class<T> type;

        public UnionView(DataView[] dataViews) {
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

            return children[index].selectOne(r);
        }

        private int selectChildIndexRandBySize(RandomSequence r) {
            long[] ranges = new long[children.length];
            long acc = 0;
            for (int i = 0, childrenLength = children.length; i < childrenLength; i++) {
                acc += children[i].getSize();
                ranges[i] = acc;
            }

            int index = Arrays.binarySearch(ranges, r.nextLong(acc));
            if (index < 0) {
                index = -index - 1;
            }
            return index;
        }

        @Override
        public Generator<T> random() {
            return new Generator<T>(getEntryType()) {
                @Override
                public T generate(RandomSequence register) {
                    return selectOne(register).lastVersion().block();
                }
            };
        }
        @Override
        public Flux<DataSet<T>.Entry> traverseDataSet(boolean includeDeleted) {
            return Flux.fromArray(children).flatMap(dataView -> dataView.traverseDataSet(includeDeleted));
        }

        @Override
        public Flux<DataSet<T>.Operation> buildOperationFeed() {
            final Iterator<DataSet<T>.Operation>[] feeds = Stream.of(children)
                    .map(DataView::buildOperationFeed)
                    .map((operationFlux) -> operationFlux.toIterable(1))
                    .map(Iterable::iterator)
                    .toArray(Iterator[]::new);

            return Flux.generate(new Consumer<SynchronousSink<DataSet<T>.Operation>>() {
                final RandomSequence r = new RandomSequence(0);
                @Override
                public void accept(SynchronousSink<DataSet<T>.Operation> fluxSink) {
                        int index = UnionView.this.selectChildIndexRandBySize(r);

                        final Iterator<DataSet<T>.Operation> feed = feeds[index];

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
}
