package com.arboratum.beangen.core;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import com.arboratum.beangen.distribution.RegExpStringGenerator;
import com.arboratum.beangen.util.RandomSequence;
import com.arboratum.beangen.util.ToCharFunction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Chars;
import org.apache.commons.math3.stat.Frequency;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static com.arboratum.beangen.util.SubSetSumProblemWithRepetition.findPossibleContatenationOfWords;

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

    /**
     * Specify the characters with equals probability to appear and uniform length between 0 and 20
     *
     * @param chars
     * @return
     */
    public CharSequenceGeneratorBuilder<VALUE> withCharacters(String chars) {
        new CharSetGeneratorConfig(chars).uniformLength(0,20);
        return this;
    }

    /**
     * Specify the characters with equals probability to appear
     *
     * @param chars
     * @return
     */
    public CharSetGeneratorConfig withCharactersAnd(String chars) {
        return new CharSetGeneratorConfig(chars);
    }


    /**
     * Specify the characters and their probability to appear as a Frequency of char
     *
     * @param charsAndFrequency
     * @return
     */
    public CharSetGeneratorConfig withCharactersAnd(Frequency charsAndFrequency) {
        return new CharSetGeneratorConfig(charsAndFrequency);
    }

    /**
     * Specify the words dictionary, the total expected length and the number of unique values.
     *
     * @return
     */
    public CharSequenceGeneratorBuilder<VALUE> withWords(ImmutableList<String> words, int length, int numberUnique, long valueSetGeneratorSeed) {
        int[] lengths = words.stream().mapToInt(String::length).toArray();

        char[][] solutions = findPossibleContatenationOfWords(lengths, length, new RandomSequence(valueSetGeneratorSeed))
                .map(indexes -> Arrays.stream(indexes).mapToObj(i -> words.get(i)).collect(Collectors.joining(" ")).toCharArray())
                .limitRate(numberUnique)
                .toStream().toArray(char[][]::new);

        buildFromCharArrayGenerator(randomSequence -> solutions[randomSequence.nextInt(solutions.length)]);

        return this;
    }

    /**
     * Specify the string must match the given regexp
     *
     * @param regexp
     * @return
     */
    public CharSequenceGeneratorBuilder<VALUE> matching(String regexp) {
        final RegExpStringGenerator valueFunction = new RegExpStringGenerator(regexp);

        buildFromCharArrayGenerator(valueFunction);
        return this;
    }

    /**
     * Specify the string must match contains only A-Za-z0-9 character with uniform probability
     * of length between minSize and maxSize
     *
     * @param minSize
     * @param maxSize
     * @return
     */
    public CharSequenceGeneratorBuilder<VALUE> alphaNumeric(int minSize, int maxSize) {
        return withCharactersAnd("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0987654321").uniformLength(minSize,maxSize);
    }


    public class CharSetGeneratorConfig {
        private final ToCharFunction<RandomSequence> nextChar;

        CharSetGeneratorConfig(String characters) {

            final char[] chars = Chars.toArray(new TreeSet<>(Chars.asList(characters.toCharArray())));
            nextChar = r -> chars[r.nextInt(chars.length)];
        }

        public CharSetGeneratorConfig(Frequency frequency) {
            final Generator<Character> charGenerator = BaseBuilders.enumerated(Character.class).from(frequency).build();
            nextChar = r -> charGenerator.apply(r).charValue();
        }

        /**
         * Specify the probabiliy of the length as uniformly distributed between min and max included
         *
         *
         * @param min
         * @param max
         * @return
         */
        public CharSequenceGeneratorBuilder<VALUE> uniformLength(int min, int max) {
            final int range = max - min + 1;
            final ToIntFunction<RandomSequence> lengthGenerator = (r) -> r.nextInt(range) + min;

            buildGenerator(lengthGenerator);

            return CharSequenceGeneratorBuilder.this;
        }

        /**
         * Specify the probabiliy of the length using a Frequency of Int
         *
         * @param frequency
         * @return
         */
        public CharSequenceGeneratorBuilder<VALUE> lengthDistribution(Frequency frequency) {
            final Generator<Number> charGenerator = BaseBuilders.enumerated(Number.class).from(frequency).build();
            final ToIntFunction<RandomSequence> lengthGenerator = r -> charGenerator.apply(r).intValue();

            buildGenerator(lengthGenerator);

            return CharSequenceGeneratorBuilder.this;
        }

        private void buildGenerator(ToIntFunction<RandomSequence> lengthGenerator) {
            final Function<RandomSequence, char[]> valueFunction;
            valueFunction = (r) -> {
                final int size = lengthGenerator.applyAsInt(r);
                final char[] buffer = new char[size];

                for (int i = 0; i < size; i++) {
                    buffer[i] = nextChar.applyAsChar(r);
                }

                return buffer;
            };

            buildFromCharArrayGenerator(valueFunction);
        }
    }
}
