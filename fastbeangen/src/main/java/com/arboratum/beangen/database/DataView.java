package com.arboratum.beangen.database;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import reactor.core.publisher.Flux;

/**
 * Created by gpicron on 23/02/2017.
 */
public interface DataView<ENTRY> {

    Class<ENTRY> getEntryType();

    /**
     * Select an entry from the DataView, if the DataView is empty it returns <code>null</code>
     */
    DataSet<ENTRY>.Entry selectOne(RandomSequence r);

    /**
     * Generator that returns randomly selected entries of the DataView, if the DataView is empty it generates <code>null</code>
     */
    Generator<ENTRY> random();

    Flux<DataSet<ENTRY>.Entry> traverseDataSet(boolean includeDeleted);

    Flux<DataSet<ENTRY>.Operation> buildOperationFeed(boolean autoAck);

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
}
