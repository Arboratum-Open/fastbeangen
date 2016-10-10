package com.arboratum.beangen;

import com.arboratum.beangen.core.*;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by gpicron on 09/08/2016.
 */
public class BaseBuilders {

    public static BooleanGeneratorBuilder aBoolean() {
        return new BooleanGeneratorBuilder(Boolean.class);
    }

    public static IntegerGeneratorBuilder<Byte> aByte() {
        return new IntegerGeneratorBuilder<>(Byte.class);
    }

    public static IntegerGeneratorBuilder<Short> aShort() {
        return new IntegerGeneratorBuilder<>(Short.class);
    }

    public static IntegerGeneratorBuilder<Integer> aInteger() {
        return new IntegerGeneratorBuilder<>(Integer.class);
    }
    public static IntegerGeneratorBuilder<Long> aLong() {
        return new IntegerGeneratorBuilder<>(Long.class);
    }

    public static DecimalGeneratorBuilder<Float> aFloat() {
        return new DecimalGeneratorBuilder<>(Float.class);
    }

    public static DecimalGeneratorBuilder<Double> aDouble() {
        return new DecimalGeneratorBuilder<>(Double.class);
    }

    public static DateGeneratorBuilder<Date> aDate() {
        return new DateGeneratorBuilder<>(Date.class);
    }

    public static <VALUE extends Enum<VALUE>> EnumGeneratorBuilder<VALUE> anEnum(Class<VALUE> clazz) {
        return new EnumGeneratorBuilder<>(clazz);
    }

    public static CharSequenceGeneratorBuilder<String> aString() {
        return new CharSequenceGeneratorBuilder(String.class);
    }

    public static <C> CharSequenceGeneratorBuilder<C> aCharSequence(Class<C> clazz) {
        return new CharSequenceGeneratorBuilder(clazz);
    }

    public static <COL extends List<VALUE>, VALUE> CollectionGeneratorBuilder<COL> aList() {
        return new CollectionGeneratorBuilder(List.class);
    }
    public static <COL extends Set<VALUE>, VALUE> CollectionGeneratorBuilder<COL> aSet() {
        return new CollectionGeneratorBuilder(Set.class);
    }

    public static <CLASS> BeanGeneratorBuilder<CLASS> aBean(Class<CLASS> clazz) {
        return new BeanGeneratorBuilder<CLASS>(clazz);
    }

    public static <CLASS> EnumeratedDistributionGeneratorBuilder<CLASS> enumerated(Class<CLASS> clazz) {
        return new EnumeratedDistributionGeneratorBuilder<CLASS>(clazz);
    }
}
