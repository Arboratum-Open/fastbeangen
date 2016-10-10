package com.arboratum.beangen.core;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import lombok.Data;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by gpicron on 09/08/2016.
 */
@Data
public abstract class AbstractGeneratorBuilder<CLASS> {
    protected final Class<CLASS> fieldType;

    private Function<RandomSequence, CLASS> generator;

    public AbstractGeneratorBuilder(Class<CLASS> fieldType) {
        this.fieldType = fieldType;
    }

    protected void setup(Function<RandomSequence, CLASS> generator) {
        this.generator = generator;
    }

    public Generator<CLASS> build() {
        assertFieldTypeSupported();
        return new DelegateGenerator<>(fieldType, generator);
    }

    protected void assertFieldTypeSupported() {
        try {
            final Set<Class> supported_types = (Set<Class>) this.getClass().getField("SUPPORTED_TYPES").get(null);
            if (!supported_types.contains(fieldType)) throw new IllegalArgumentException("Type not supported by this generator");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Not SUPPORTED_TYPES static field accessible in class" + this.getClass());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Not SUPPORTED_TYPES static field found in class" + this.getClass());
        }
    }

    private static class DelegateGenerator<CLASS> extends Generator<CLASS> {
        private final Function<RandomSequence, CLASS> generator;

        protected DelegateGenerator(Class<CLASS> type, Function<RandomSequence, CLASS> generator) {
            super(type);
            this.generator = generator;
        }

        @Override
        public CLASS generate(RandomSequence register) {
            return generator.apply(register);
        }
    }

    public AbstractGeneratorBuilder<CLASS> postProcess(BiFunction<CLASS, RandomSequence, CLASS> processor) {

        return new AbstractGeneratorBuilder(Object.class) {

            @Override
            public Generator build() {
                final Generator<CLASS> source = AbstractGeneratorBuilder.this.build();
                return new Generator(Object.class) {
                    @Override
                    public Object generate(RandomSequence register) {
                        return processor.apply(source.generate(register), register);
                    }

                };
            }
        };
    }

    public <TARGET> AbstractGeneratorBuilder<TARGET> convert(Function<CLASS, TARGET> conversion) {
        final Generator<CLASS> source = this.build();

        return new AbstractGeneratorBuilder(Object.class) {

            @Override
            public Generator build() {
                return new Generator(Object.class) {
                    @Override
                    public Object generate(RandomSequence register) {
                        return conversion.apply(source.generate(register));
                    }

                };
            }
        };
    }
    public AbstractGeneratorBuilder<String> convertToString() {
        final Generator<CLASS> source = this.build();

        return new AbstractGeneratorBuilder(Object.class) {

            @Override
            public Generator build() {
                return new Generator(String.class) {
                    @Override
                    public String generate(RandomSequence register) {
                        return String.valueOf(source.generate(register));
                    }

                };
            }
        };
    }
}
