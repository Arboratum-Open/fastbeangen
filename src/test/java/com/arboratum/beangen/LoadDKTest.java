package com.arboratum.beangen;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.util.Collector;
import org.junit.Test;

/**
 * Created by gpicron on 11/08/2016.
 */
public class LoadDKTest {

    @Test
    public void testComparison() throws Exception {
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.readTextFile("dk_male.csv")
                .map(value -> value.split("\t"))
                .flatMap(new FlatMapFunction<String[], Tuple2<String,Long>>() {
                    @Override
                    public void flatMap(String[] value, Collector<Tuple2<String,Long>> out) throws Exception {
                        if (value.length == 2)
                        for (String v : value[1].split(",")) {
                            out.collect(Tuple2.of(v, Long.parseLong(value[0])));
                        }
                    }
                })
                .sortPartition(1, Order.DESCENDING)
                .writeAsCsv("src/main/resources/generator/givenName_male_DK.csv", FileSystem.WriteMode.OVERWRITE).setParallelism(1);
        env.execute();
    }
}