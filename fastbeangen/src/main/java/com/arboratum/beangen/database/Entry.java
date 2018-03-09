package com.arboratum.beangen.database;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * @author gpicron.
 */
public interface Entry<ENTRY> {
    DataView.OpCode getLastOperation();

    Flux<ENTRY> allVersions();

    Flux<Tuple2<UpdateOf<ENTRY>, ENTRY>> allUpdatesAndEntry();

    Mono<ENTRY> lastVersion();

    Mono<UpdateOf<ENTRY>> lastUpdate();

    boolean isLive();

    boolean isDeleted();

    int getElementIndex();

    byte getElementVersion();

    DataSet.EntryRef getRef();

    DataSet getDataSet();
}
