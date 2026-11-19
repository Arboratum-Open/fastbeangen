package com.arboratum.beangen.database;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** The same fixed assertions run against the official 0.2.17 binary and the candidate. */
public class PublishedApiCompatibilityTest {
    private static class Value {
        int number = 1;
    }

    private static final Generator<Value> VALUES = new Generator<Value>(Value.class) {
        @Override
        public Value generate(RandomSequence random) {
            return new Value();
        }
    };

    private DataSetBuilder<Value> builder(int size) {
        return new DataSetBuilder<Value>().of(size).withEntryGenerator(VALUES);
    }

    @Test(timeout = 5000)
    public void boundedSelectionPreservesPublishedNullAndSuccessResults() {
        DataSet<Value> data = builder(4).build();
        assertNotNull(data.filteredView(value -> true, 1).selectOne(new RandomSequence(31)));
        assertNull(data.filteredView(value -> false, 1).selectOne(new RandomSequence(31)));
        assertNull(data.filteredView(value -> true, 0).selectOne(new RandomSequence(31)));
        assertNull(data.filteredView(value -> true, -1).selectOne(new RandomSequence(31)));
        assertNull(builder(0).build().filteredView(value -> true, 1).selectOne(new RandomSequence(31)));
        assertNotNull(data.filteredView(value -> true).selectOne(new RandomSequence(31)));
    }

    @Test(timeout = 5000)
    public void finiteBudgetsPropagateThroughComposedAndUnionViews() {
        DataSet<Value> first = builder(4).build();
        DataSet<Value> second = builder(4).build();
        assertNull(first.transformedView(value -> value.number, Integer.class, 0)
                .selectOne(new RandomSequence(31)));
        assertNull(first.filteredView(value -> true).transformedView(value -> value.number, Integer.class, 0)
                .selectOne(new RandomSequence(31)));
        DataView<Value> union = new UnionDataView<>("reference", Value.class, first, second);
        assertNull(union.filteredView(value -> true, 0).selectOne(new RandomSequence(31)));
        assertNull(union.transformedView(value -> value.number, Integer.class, 0)
                .selectOne(new RandomSequence(31)));
    }

    @Test(timeout = 5000)
    public void provenanceDistinguishesInitialAndSubsequentlyCreatedEntries() {
        DataSet<Value> data = builder(4).thenUpdatedWith(1, 0, 0).build();
        Entry<Value> initial = data.get(0);
        assertTrue(initial.wasInInitialDataSet());
        assertTrue(initial.getRef().wasInInitialDataSet());
        DataSet<Value>.Operation created = data.buildOperationFeed(true).next().block();
        assertEquals(DataView.OpCode.CREATE, created.getEntry().getLastOperation());
        assertEquals(4, created.getEntry().getElementIndex());
        assertFalse(created.getEntry().wasInInitialDataSet());
        assertFalse(created.getEntry().getRef().wasInInitialDataSet());
    }

    @Test(timeout = 5000)
    public void transformedSelectionRetainsEntryAndReferenceProvenance() {
        Entry<Integer> transformed = builder(4).build()
                .transformedView(value -> value.number, Integer.class, 1).selectOne(new RandomSequence(31));
        assertNotNull(transformed);
        assertEquals(Integer.valueOf(1), transformed.lastVersion().block());
        assertTrue(transformed.wasInInitialDataSet());
        assertTrue(transformed.getRef().wasInInitialDataSet());
        assertTrue(transformed.getRef().getCurrent().wasInInitialDataSet());
    }

    @Test(timeout = 5000)
    @SuppressWarnings("unchecked")
    public void initialProvenanceIncludesDeletedSlots() throws Exception {
        Constructor<?> full = Arrays.stream(DataSet.class.getDeclaredConstructors())
                .max(Comparator.comparingInt(Constructor::getParameterCount)).get();
        List<Object> arguments = new ArrayList<>(Arrays.asList("holes", null, null, null,
                new byte[]{1, -1, 1}, 2, VALUES, Collections.emptyList(), null, null,
                builder(0).getScheduler(), 0));
        // The published constructor additionally takes the requested live count.
        if (full.getParameterCount() == 13) arguments.add(1, 2);
        DataSet<Value> data = (DataSet<Value>) full.newInstance(arguments.toArray());
        assertEquals(2, data.getSize());
        assertTrue(data.get(1).isDeleted());
        assertTrue(data.get(2).wasInInitialDataSet());
        assertTrue(data.get(2).getRef().wasInInitialDataSet());
    }

    @Test(timeout = 5000)
    public void deletionPredicateReceivesTheOperationEntry() {
        AtomicReference<Entry<Value>> seen = new AtomicReference<>();
        DataSet<Value> data = builder(4).thenUpdatedWith(0, 0, 1)
                .withDeletionCheck((Entry<Value> entry) -> {
                    seen.set(entry);
                    return entry.wasInInitialDataSet();
                }).build();
        DataSet<Value>.Operation deleted = data.buildOperationFeed(true).next().block();
        assertEquals(DataView.OpCode.DELETE, deleted.getEntry().getLastOperation());
        assertSame(deleted.getEntry(), seen.get());
        assertTrue(seen.get().wasInInitialDataSet());
    }

    @Test(timeout = 5000)
    public void applicabilityReceivesTheMatchingReference() {
        AtomicReference<DataSet.EntryRef<Value>> seen = new AtomicReference<>();
        DataSet<Value> data = builder(4).thenUpdatedWith(0, 1, 0)
                .withUpdateGenerator(new UpdateGenerator<Value>() {
                    @Override
                    public boolean canApplyOn(Value value, DataSet.EntryRef<Value> ref) {
                        seen.set(ref);
                        return ref.wasInInitialDataSet();
                    }

                    @Override
                    public UpdateOf<Value> generate(Value previous, RandomSequence random) {
                        return value -> { value.number++; return value; };
                    }
                }).build();
        DataSet<Value>.Operation updated = data.buildOperationFeed(true).next().block();
        assertEquals(DataView.OpCode.UPDATE, updated.getEntry().getLastOperation());
        assertEquals(updated.getEntry().getRef(), seen.get());
        assertEquals(2, updated.getEntry().lastVersion().block().number);
    }

    @Test(timeout = 5000)
    public void rejectingApplicabilityDoesNotGenerateAnUpdate() {
        AtomicInteger checks = new AtomicInteger();
        DataSet<Value> data = builder(4).thenUpdatedWith(0, 1, 0)
                .withUpdateGenerator(new UpdateGenerator<Value>() {
                    @Override
                    public boolean canApplyOn(Value value, DataSet.EntryRef<Value> ref) {
                        checks.incrementAndGet();
                        assertTrue(ref.wasInInitialDataSet());
                        return false;
                    }

                    @Override
                    public UpdateOf<Value> generate(Value previous, RandomSequence random) {
                        throw new AssertionError("Rejected updates must not be generated");
                    }
                }).build();
        assertTrue(data.buildOperationFeed(false, false).next().block().isInvalidOperation());
        assertTrue(checks.get() > 0);
    }
}
