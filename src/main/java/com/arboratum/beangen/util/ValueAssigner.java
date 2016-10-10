package com.arboratum.beangen.util;

/**
 * Created by gpicron on 07/08/2016.
 */
public interface ValueAssigner<CLASS,VALUE> {

    void assign(CLASS object, VALUE value);
}
