package com.arboratum.beangen.database;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by gpicron on 12/04/2017.
 */
public class UnionDataViewTest {


    @Test
    public void selectOneWithSomeEmptyUnderLyingDataSet() throws Exception {
        Generator<DataSetBuilderTest.Pojo> generator = BaseBuilders.aBean(DataSetBuilderTest.Pojo.class).withDefaultFieldPopulators().build();
        Generator<Integer> update = BaseBuilders.aInteger().uniform(0, 5).build();

        UpdateGenerator<DataSetBuilderTest.Pojo> updateGenerator = new UpdateGenerator<DataSetBuilderTest.Pojo>() {

            @Override
            public UpdateOf<DataSetBuilderTest.Pojo> generate(DataSetBuilderTest.Pojo previousValue, RandomSequence randomSequence) {
                return pojo -> {
                    pojo.setName(pojo.getName() + update.generate(randomSequence).intValue());

                    return pojo;
                };
            }
        };


        final DataSet<DataSetBuilderTest.Pojo> set1 = new DataSetBuilder<DataSetBuilderTest.Pojo>()
                .of(0)
                .thenUpdatedWith(5, 4, 1)
                .withEntryGenerator(generator)
                .withUpdateGenerator(updateGenerator)
                .build();
        final DataSet<DataSetBuilderTest.Pojo> set2 = new DataSetBuilder<DataSetBuilderTest.Pojo>()
                .of(0)
                .thenUpdatedWith(5, 4, 1)
                .withEntryGenerator(generator)
                .withUpdateGenerator(updateGenerator)
                .build();

        UnionDataView<DataSetBuilderTest.Pojo> union = new UnionDataView<>(set1, set2);

        final RandomSequence r = new RandomSequence(2);
        for (int i = 0; i < 100; i++) {
            Assert.assertNull(union.selectOne(r));
        }


        final DataSet<DataSetBuilderTest.Pojo> set3 = new DataSetBuilder<DataSetBuilderTest.Pojo>()
                .of(100)
                .thenUpdatedWith(5, 4, 1)
                .withEntryGenerator(generator)
                .withUpdateGenerator(updateGenerator)
                .build();

        union = new UnionDataView<>(set1, set3, set2);
        for (int i = 0; i < 1000; i++) {
            Assert.assertNotNull(union.selectOne(r));
        }

    }

}