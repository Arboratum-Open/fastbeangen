package com.arboratum.beangen.database;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DefaultUpdateFeedCompatibilityTest {
    @Test(timeout = 10000)
    public void preservesGeneratedValuesAcrossUpdatesWithTheDefaultScheduler() {
        Generator<int[]> generator = new Generator<int[]>(int[].class) {
            @Override
            public int[] generate(RandomSequence random) {
                return new int[]{random.nextInt(100)};
            }
        };
        int initial = generator.generate(0)[0];
        DataSet<int[]> dataSet = DataSet.<int[]>builder().of(0).withEntryGenerator(generator)
                .withUpdateGenerator((previous, random) -> value -> { value[0]++; return value; })
                .thenUpdatedWith(0, 1, 0).build();
        List<Integer> values = dataSet.buildOperationFeed(true).take(5)
                .concatMap(operation -> operation.getEntry().lastVersion()).map(value -> value[0])
                .collectList().block();
        assertEquals(Arrays.asList(initial, initial + 1, initial + 2, initial + 3, initial + 4), values);
    }
}
