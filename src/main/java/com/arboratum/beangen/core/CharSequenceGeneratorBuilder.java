package com.arboratum.beangen.core;

import com.arboratum.beangen.util.RandomSequence;
import com.arboratum.beangen.distribution.RegExpStringGenerator;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Chars;

import java.nio.CharBuffer;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Created by gpicron on 09/08/2016.
 */
public class CharSequenceGeneratorBuilder<VALUE> extends AbstractGeneratorBuilder<VALUE> {
    public static final ImmutableSet<Class> SUPPORTED_TYPES = ImmutableSet.of(
            String.class,
            CharBuffer.class, Appendable.class, CharSequence.class,
            StringBuffer.class, StringBuilder.class,
            new char[0].getClass()
    );


    public CharSequenceGeneratorBuilder(Class<VALUE> fieldType) {
        super(fieldType);
    }


    private void buildFromCharArrayGenerator(Function<RandomSequence, char[]> valueFunction) {
        if (fieldType == String.class) {
            setup(r -> (VALUE) new String(valueFunction.apply(r)));
        } else if (fieldType == CharBuffer.class || fieldType == Appendable.class || fieldType == CharSequence.class) {
            setup(r -> (VALUE) CharBuffer.wrap(valueFunction.apply(r)));
        } else if (fieldType == StringBuilder.class ) {
            setup(r -> {
                    char[] value = valueFunction.apply(r);
                    StringBuilder sb = new StringBuilder(value.length);
                    sb.append(value);
                    return (VALUE)sb;
            });
        } else if (fieldType == StringBuffer.class) {
            setup(r -> {
                char[] value = valueFunction.apply(r);
                StringBuffer sb = new StringBuffer(value.length);
                sb.append(value);
                return (VALUE)sb;
            });
        } else if (fieldType.isArray() && fieldType.getComponentType() == Character.TYPE) {
            setup((Function<RandomSequence, VALUE>) valueFunction);
        } else {
            throw new IllegalArgumentException("Unsupported type");
        }
    }

    public CharSequenceGeneratorBuilder<VALUE> withCharacters(String chars) {
        new CharSetGeneratorConfig(chars).uniformLength(0,20);
        return this;
    }

    public CharSetGeneratorConfig withCharactersAnd(String chars) {
        return new CharSetGeneratorConfig(chars);
    }

    public CharSequenceGeneratorBuilder<VALUE> matching(String regexp) {
        buildFromCharArrayGenerator(new RegExpStringGenerator(regexp));
        return this;
    }

    public CharSequenceGeneratorBuilder<VALUE> alphaNumeric(int minSize, int maxSize) {
        return withCharactersAnd("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0987654321").uniformLength(minSize,maxSize);
    }


    public class CharSetGeneratorConfig {

        private final char[] chars;

        CharSetGeneratorConfig(String characters) {
            chars = Chars.toArray(new TreeSet<>(Chars.asList(characters.toCharArray())));

        }

        public CharSequenceGeneratorBuilder<VALUE> uniformLength(int min, int max) {
            final int range = max - min + 1;
            final ToIntFunction<RandomSequence> lengthGenerator = (r) -> r.nextInt(range) + min;

            final Function<RandomSequence, char[]> valueFunction = (r) -> {
                final int size = lengthGenerator.applyAsInt(r);
                final char[] buffer = new char[size];

                for (int i = 0; i < size; i++) {
                    buffer[i] = chars[r.nextInt(chars.length)];
                }

                return buffer;
            };

            buildFromCharArrayGenerator(valueFunction);

            return CharSequenceGeneratorBuilder.this;
        }
    }
}
