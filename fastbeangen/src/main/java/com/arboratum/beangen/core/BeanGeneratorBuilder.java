package com.arboratum.beangen.core;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.Populator;
import com.arboratum.beangen.util.RandomSequence;
import com.arboratum.beangen.util.ValueAssigner;
import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.common.primitives.Primitives;

import java.lang.reflect.Modifier;
import java.util.*;

import static com.arboratum.beangen.BaseBuilders.aBoolean;

/**
 * Created by gpicron on 08/08/2016.
 */
public class BeanGeneratorBuilder<CLASS> extends AbstractGeneratorBuilder<CLASS> {
    private final MethodAccess access;

    private final SortedMap<String, Generator> populators = new TreeMap<>();
    private String idfield;

    public BeanGeneratorBuilder(Class<CLASS> type) {
        super(type);
        access = MethodAccess.get(type);
    }

    protected void assertFieldTypeSupported() {}

    @Override
    public Generator<CLASS> build() {
        final ConstructorAccess<CLASS> constructorAccess = ConstructorAccess.get(fieldType);
        final Populator[] pops = populators.entrySet().stream()
                .map(e -> {
                    return new Populator(getValueAssigner(e.getKey()), e.getValue());
                })
                .toArray(Populator[]::new);

        setup(seq -> {
            CLASS o = constructorAccess.newInstance();

            for (Populator p : pops) p.populate(o, seq);

            return o;
        });

        return super.build();
    }

    private static class IndexedMethod implements Comparable<IndexedMethod> {
        final int i;
        final String methodName;

        private IndexedMethod(int i, String methodName) {
            this.i = i;
            this.methodName = methodName;
        }

        @Override
        public int compareTo(IndexedMethod o) {
            return methodName.compareTo(o.methodName);
        }
    }

    public BeanGeneratorBuilder<CLASS> withDefaultFieldPopulators() {
        final String[] methodNames = access.getMethodNames();
        final SortedSet<IndexedMethod> methods = new TreeSet<>();
        for (int i = 0; i < methodNames.length; i++) {
            methods.add(new IndexedMethod(i, methodNames[i]));
        }

        final Class[] returnTypes = access.getReturnTypes();
        final Class[][] parameterTypes = access.getParameterTypes();
        for (IndexedMethod method : methods) {
            final int i = method.i;
            if (returnTypes[i] == Void.TYPE && parameterTypes[i].length == 1 && methodNames[i].startsWith("set")) {
                final String fieldName = methodNames[i].substring(3, 4).toLowerCase() + methodNames[i].substring(4);
                if (populators.containsKey(fieldName)) continue;
                final int methodIndex = i;
                final Class aClass = parameterTypes[i][0];

                if (BooleanGeneratorBuilder.SUPPORTED_TYPES.contains(aClass)) {
                    with(fieldName, aBoolean().uniform());
                } else if (IntegerGeneratorBuilder.SUPPORTED_TYPES.contains(aClass)) {
                    with(fieldName, new IntegerGeneratorBuilder<>(aClass).uniform());
                } else if (DecimalGeneratorBuilder.SUPPORTED_TYPES.contains(aClass)) {
                    with(fieldName, new DecimalGeneratorBuilder<>(aClass).uniform());
                } else if (CharSequenceGeneratorBuilder.SUPPORTED_TYPES.contains(aClass)) {
                    with(fieldName, new CharSequenceGeneratorBuilder<>(aClass).alphaNumeric(0,20));
                } else if (DateGeneratorBuilder.SUPPORTED_TYPES.contains(aClass)) {
                    with(fieldName, new DateGeneratorBuilder<>(aClass).uniform());
                } else if (aClass.isEnum()) {
                    with(fieldName, new EnumGeneratorBuilder<>(aClass).uniform());
                } else if (!Modifier.isAbstract(aClass.getModifiers())){
                    with(fieldName, new BeanGeneratorBuilder<>(aClass).withDefaultFieldPopulators());
                }
            }
        }
        return this;
    }


    public BeanGeneratorBuilder<CLASS> injectIdIn(String fieldName) {
        populators.put(fieldName, new Generator(Long.class) {
            @Override
            public Object generate(RandomSequence register) {
                return register.getSeed();
            }

        });

        return this;
    }

    public <VALUE> BeanGeneratorBuilder<CLASS> with(String fieldName, AbstractGeneratorBuilder<VALUE> generator) {
        return with(fieldName, generator.build());

    }

    public <VALUE> BeanGeneratorBuilder<CLASS> with(String fieldName, Generator<VALUE> generator) {
        final ValueAssigner valueAssigner = getValueAssigner(fieldName);
        if (valueAssigner.accept(generator.getType())) {
            populators.put(fieldName, generator);
        } else {
            throw new IllegalArgumentException("The field '" + fieldName + "' cannot be set to type accept with type " + generator.getType());
        }

        return this;
    }

    private <CLASS,FIELD> ValueAssigner<CLASS,FIELD> getValueAssigner(String fieldName) {
        final int methodIndex = access.getIndex("set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), 1);
        return new ValueAssigner<CLASS, FIELD>() {
            @Override
            public void assign(CLASS o, FIELD v) {
                access.invoke(o, methodIndex, v);
            }

            @Override
            public boolean accept(Class<? extends FIELD> type) {
                final Class parameterClass = access.getParameterTypes()[methodIndex][0];
                if (type.isAssignableFrom(parameterClass)) {
                    return true;
                } else if (parameterClass.isPrimitive() && parameterClass == Primitives.unwrap(type)) {
                    return true;
                } else if (type.isPrimitive() && type == Primitives.unwrap(parameterClass)) {
                    return true;
                } else {
                    return false;
                }
            }
        };
    }


    private Class getFieldType(String fieldName) {
        final int methodIndex = access.getIndex("set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
        return access.getParameterTypes()[methodIndex][0];
    }

}
