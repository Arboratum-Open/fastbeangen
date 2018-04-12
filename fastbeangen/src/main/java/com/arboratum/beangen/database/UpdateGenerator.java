package com.arboratum.beangen.database;

import com.arboratum.beangen.util.RandomSequence;

/**
 * Created by gpicron on 13/02/2017.
 */
public interface UpdateGenerator<ENTRY> {

    UpdateOf<ENTRY> generate(ENTRY previousValue, RandomSequence randomSequence);

    default boolean canApplyOn(ENTRY value) { return true; };
}
