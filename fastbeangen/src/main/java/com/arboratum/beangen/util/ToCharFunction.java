package com.arboratum.beangen.util;

import java.util.function.Function;

/**
 * Represents a function that produces an char-valued result.  This is the
 * {@code char}-producing primitive specialization for {@link Function}.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #applyAsChar(Object)}.
 *
 * @param <T> the type of the input to the function
 *
 * @see Function
 */
@FunctionalInterface
public interface ToCharFunction<T> {

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    char applyAsChar(T value);
}
