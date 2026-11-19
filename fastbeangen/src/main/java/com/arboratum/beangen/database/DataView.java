package com.arboratum.beangen.database;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import reactor.core.publisher.Flux;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by gpicron on 23/02/2017.
 */
public interface DataView<ENTRY> {

    String getName();

    Class<ENTRY> getEntryType();

    /**
     * Select an entry from the DataView, if the DataView is empty it returns <code>null</code>
     */
    Entry<ENTRY> selectOne(RandomSequence r);

    /**
     * Generator that returns randomly selected entries of the DataView, if the DataView is empty it generates <code>null</code>
     */
    Generator<ENTRY> random();

    Flux<Entry<ENTRY>> traverseDataSet(boolean includeDeleted);

    default Flux<DataSet<ENTRY>.Operation> buildOperationFeed(boolean autoAck) {
        return buildOperationFeed(autoAck, true);
    }

    boolean canGenerateOperations();

    Flux<DataSet<ENTRY>.Operation> buildOperationFeed(boolean autoAck, boolean filterNonGeneratable);

    int getSize();

    public enum OpCode {
        CREATE, UPDATE, DELETE
    }

    public interface CreateTrigger<ENTRY> {
        void apply(int index, ENTRY value);
    }

    public interface UpdateTrigger<ENTRY> {
        void apply(int index, byte version, UpdateOf<ENTRY> value);
    }

    default <T> DataView<T> transformedView(Function<ENTRY, T> transformFunction, Class<T> targetType) {
        return transformedView(transformFunction, targetType, Integer.MAX_VALUE);
    }

    <T> DataView<T> transformedView(Function<ENTRY, T> transformFunction, Class<T> targetType, int maxTryPerSelect);

    default DataView<ENTRY> filteredView(Predicate<ENTRY> acceptPredicate) {
        return filteredView(acceptPredicate, Integer.MAX_VALUE);
    }

    DataView<ENTRY> filteredView(Predicate<ENTRY> acceptPredicate, int maxTryPerSelect);

}
