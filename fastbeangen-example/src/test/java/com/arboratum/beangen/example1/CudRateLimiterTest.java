package com.arboratum.beangen.example1;

import com.arboratum.beangen.example1.model.Person;
import com.beust.jcommander.JCommander;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class CudRateLimiterTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void supplied_limiter_is_consumed_before_processing_each_operation() throws Exception {
        var database = new ComplexDatabase();
        JCommander.newBuilder().addObject(database).build().parse("--persons", "10", "--relations", "0");
        database.initialize();
        var limiter = RateLimiter.create(0.25);
        var processed = new AtomicBoolean();
        var complete = new ProbeComplete();
        var example = new SimpleGenAndQueryTest();
        example.dao = new TestDbDAO() {
            private void probe() {
                processed.set(true);
                assertFalse("The supplied limiter must be acquired before DAO processing", limiter.tryAcquire());
                // Stop at the first operation, without database access, buffering or wall-clock sleeps.
                throw complete;
            }
            @Override public void insert(Person person) { probe(); }
            @Override public void delete(Person person) { probe(); }
            @Override public void update(Person person) { probe(); }
        };
        var method = SimpleGenAndQueryTest.class.getDeclaredMethod("createCUDFlux", ComplexDatabase.class, File.class, RateLimiter.class);
        method.setAccessible(true);
        var pipeline = (Flux<?>) method.invoke(example, database, new File(temporary.getRoot(), "counter"), limiter);
        var result = assertThrows(RuntimeException.class, () -> pipeline.blockLast(Duration.ofSeconds(5)));
        assertSame(complete, Exceptions.unwrap(result));
        assertTrue("The actual CUD pipeline must reach processing", processed.get());
    }

    private static final class ProbeComplete extends Error { }
}
