package com.arboratum.beangen.core;

import com.arboratum.beangen.distribution.UniformIntegerDistribution;
import com.arboratum.beangen.util.RandomSequence;
import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import java.util.Calendar;
import java.util.Date;
import java.util.function.Function;

/**
 * Created by gpicron on 08/08/2016.
 */
public class DateGeneratorBuilder<VALUE> extends AbstractGeneratorBuilder<VALUE> {

    public static final ImmutableSet<Class> SUPPORTED_TYPES = ImmutableSet.of(
            Date.class,
            Calendar.class
    );
    private DateTimeZone zone = DateTimeZone.UTC;

    public DateGeneratorBuilder(Class<VALUE> fieldType) {
        super(fieldType);
    }

    public DateGeneratorBuilder<VALUE> uniform() {
        setupFromLong(RandomSequence::nextLong);

        return this;
    }

    public DateGeneratorBuilder<VALUE> uniform(Date min, Date max) {
        final Function<RandomSequence, Long> function = new UniformIntegerDistribution<Long>(min.getTime(), max.getTime(), Long.TYPE).build();
        setupFromLong(function);
        return this;
    }

    private void setupFromLong(Function<RandomSequence, Long> function) {
        if (fieldType.isAssignableFrom(Date.class)) {
            setup(randomSequence -> {
                return (VALUE) new Instant(function.apply(randomSequence)).toDateTime(zone).withMillisOfDay(0).toDate();
            });
        } else {
            setup(randomSequence -> {
                return (VALUE) new Instant(function.apply(randomSequence)).toDateTime(zone).withMillisOfDay(0).toGregorianCalendar();
            });
        }
    }

}
