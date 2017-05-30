package com.arboratum.beangen.database;

import com.google.common.base.Stopwatch;

import java.util.*;
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
        final UnionDataView<T> tUnionView = new UnionDataView<>(dataViews);
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

}
