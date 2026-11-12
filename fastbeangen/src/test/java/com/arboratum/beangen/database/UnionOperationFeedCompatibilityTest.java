package com.arboratum.beangen.database;

import com.arboratum.beangen.BaseBuilders;
import org.junit.Test;
import reactor.core.scheduler.Schedulers;

import static org.junit.Assert.assertEquals;

public class UnionOperationFeedCompatibilityTest {
    @Test(timeout = 10000)
    public void preservesOperationsWhenSubscribedFromANonBlockingScheduler() {
        DataSet<Integer> first = DataSet.<Integer>builder().of(2)
                .withEntryGenerator(BaseBuilders.aInteger().uniform(0, 100).build())
                .thenUpdatedWith(1, 0, 0).build();
        DataSet<Integer> second = DataSet.<Integer>builder().of(2)
                .withEntryGenerator(BaseBuilders.aInteger().uniform(0, 100).build())
                .thenUpdatedWith(1, 0, 0).build();
        DataView<Integer> union = new UnionDataView<>("union", Integer.class, first, second);
        Long count = union.buildOperationFeed(true).subscribeOn(Schedulers.parallel())
                .take(10).count().block();
        assertEquals(10L, count.longValue());
    }
}
