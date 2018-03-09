package com.arboratum.beangen.database;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * @author gpicron.
 */
public abstract class AbstractEntry<T> implements  Entry<T>{
    private Flux<Tuple2<UpdateOf<T>, T>> allUpdatesAndEntry;

    public Flux<T> allVersions() {
       return allUpdatesAndEntry().map(Tuple2::getT2);
    }

    public abstract Flux<Tuple2<UpdateOf<T>, T>> buildAllUpdatesAndEntry();

    @Override
    public Flux<Tuple2<UpdateOf<T>, T>> allUpdatesAndEntry() {
        if (allUpdatesAndEntry == null) {
            allUpdatesAndEntry = buildAllUpdatesAndEntry().cache();
        }
        return allUpdatesAndEntry;
    }

    public Mono<T> lastVersion() {
            return allVersions().last();
        }

    public Mono<UpdateOf<T>> lastUpdate() {
        return allUpdatesAndEntry().filter(t -> t.getT1() != null).map(Tuple2::getT1).last();
    }
}
