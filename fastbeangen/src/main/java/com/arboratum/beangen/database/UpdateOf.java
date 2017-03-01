package com.arboratum.beangen.database;

/**
 * Created by gpicron on 15/12/2016.
 */
public interface UpdateOf<ENTRY> {
    ENTRY apply(ENTRY entry);
}
