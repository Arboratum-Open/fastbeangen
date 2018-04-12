package com.arboratum.beangen.database;

import reactor.core.publisher.Mono;

/**
 * @author gpicron.
 */
public abstract class AbstractEntry<T> implements  Entry<T>{
    private volatile EntryVersion<T> evaluated;


    protected abstract EntryVersion buildAllUpdatesHistory();

    private Mono<EntryVersion<T>> evaluated() {
        if (evaluated == null) {
            return Mono.fromCallable(() -> {
                synchronized (this) {
                    if (evaluated == null) evaluated = buildAllUpdatesHistory();
                }
                return evaluated;
            });
        } else {
            return Mono.just(evaluated);
        }
    }

    public Mono<T> lastVersion() {
        return evaluated().map(e -> e.entry);
    }

    public Mono<Integer> lastUpdateGeneratorIndex() {
        return evaluated().map(e -> e.updateGeneratorIndex);
    }

    public Mono<UpdateOf<T>> lastUpdate() {
        return evaluated().map(e -> e.updateOf);
    }

    protected static class EntryVersion<ENTRY> {
        UpdateOf<ENTRY> updateOf;
        ENTRY entry;
        int updateGeneratorIndex;

        public EntryVersion(UpdateOf<ENTRY> updateOf, ENTRY entry, int updateGeneratorIndex) {
            this.updateOf = updateOf;
            this.entry = entry;
            this.updateGeneratorIndex = updateGeneratorIndex;
        }
    }
}
