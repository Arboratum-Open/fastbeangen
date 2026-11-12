package com.arboratum.beangen.database;

import com.arboratum.beangen.BaseBuilders;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class OperationAcknowledgementTest {
    @Test(timeout = 15000)
    public void preservesEveryAcknowledgementBeyondBufferCapacityWithConcurrentProducers() throws Exception {
        DataSet<Integer> dataSet = DataSet.<Integer>builder().of(0)
                .withEntryGenerator(BaseBuilders.aInteger().uniform(0, 100).build())
                .thenUpdatedWith(1, 0, 0).build();
        List<DataSet<Integer>.Operation> operations = dataSet.buildOperationFeed(false)
                .take(128).collectList().block();
        assertEquals(0, dataSet.getSize());

        ExecutorService producers = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> acknowledgements = new ArrayList<>();
            for (DataSet<Integer>.Operation operation : operations) {
                acknowledgements.add(producers.submit(operation::ack));
            }
            for (Future<?> acknowledgement : acknowledgements) {
                acknowledgement.get(10, TimeUnit.SECONDS);
            }
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (dataSet.getSize() != 128 && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            assertEquals(128, dataSet.getSize());
            for (int i = 0; i < 128; i++) {
                assertEquals(1, dataSet.get(i).getElementVersion());
            }
        } finally {
            producers.shutdownNow();
        }
    }
}
