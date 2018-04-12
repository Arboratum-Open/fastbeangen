package com.arboratum.beangen.database;

import reactor.core.publisher.Mono;

/**
 * @author gpicron.
 */
public interface Entry<ENTRY> {
    DataView.OpCode getLastOperation();

    Mono<ENTRY> lastVersion();

    Mono<UpdateOf<ENTRY>> lastUpdate();

    boolean isLive();

    boolean isDeleted();

    int getElementIndex();

    byte getElementVersion();

    DataSet.EntryRef getRef();

    DataSet getDataSet();
}
