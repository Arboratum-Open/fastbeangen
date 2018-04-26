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

    <T> DataView<T> transformedView(Function<ENTRY, T> transformFunction, Class<T> targetType);

    DataView<ENTRY> filteredView(Predicate<ENTRY> acceptPredicate);

}
