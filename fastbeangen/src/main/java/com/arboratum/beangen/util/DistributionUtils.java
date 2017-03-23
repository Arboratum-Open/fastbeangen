package com.arboratum.beangen.util;

import org.apache.commons.math3.stat.Frequency;

import java.util.Map;

/**
 * Created by gpicron on 23/03/2017.
 */
public final class DistributionUtils {

    /**
     * The API use frequently Frequency object from commons math.  This method permits easy conversion
     * from the Map of counters
     *
     * @param counts
     *
     * @return Frequency object
     */

    public static Frequency convert(Map<? extends Comparable<?>, ? extends Number> counts) {
        Frequency frequency = new Frequency();
        for (Map.Entry<? extends Comparable<?>, ? extends Number> c : counts.entrySet()) {
            frequency.incrementValue(c.getKey(), c.getValue().longValue());
        }

        return frequency;
    }

}
