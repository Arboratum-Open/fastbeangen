package com.arboratum.beangen.core;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by gpicron on 09/08/2016.
 */
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

    public Class<CLASS> getFieldType() {
        return this.fieldType;
    }

    public Function<RandomSequence, CLASS> getGenerator() {
        return this.generator;
    }

    public void setGenerator(Function<RandomSequence, CLASS> generator) {
        this.generator = generator;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof AbstractGeneratorBuilder)) return false;
        final AbstractGeneratorBuilder other = (AbstractGeneratorBuilder) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$fieldType = this.getFieldType();
        final Object other$fieldType = other.getFieldType();
        if (this$fieldType == null ? other$fieldType != null : !this$fieldType.equals(other$fieldType)) return false;
        final Object this$generator = this.getGenerator();
        final Object other$generator = other.getGenerator();
        if (this$generator == null ? other$generator != null : !this$generator.equals(other$generator)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $fieldType = this.getFieldType();
        result = result * PRIME + ($fieldType == null ? 43 : $fieldType.hashCode());
        final Object $generator = this.getGenerator();
        result = result * PRIME + ($generator == null ? 43 : $generator.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof AbstractGeneratorBuilder;
    }

    public String toString() {
        return "com.arboratum.beangen.core.AbstractGeneratorBuilder(fieldType=" + this.getFieldType() + ", generator=" + this.getGenerator() + ")";
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

        return new AbstractGeneratorBuilder(this.fieldType) {

            @Override
            public Generator build() {
                final Generator<CLASS> source = AbstractGeneratorBuilder.this.build();
                return new Generator(this.fieldType) {
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
