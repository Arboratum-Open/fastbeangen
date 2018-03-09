package com.arboratum.beangen.core;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.Populator;
import com.arboratum.beangen.util.RandomSequence;
import com.arboratum.beangen.util.ValueAssigner;
import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.common.primitives.Primitives;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

import static com.arboratum.beangen.BaseBuilders.aBoolean;

/**
 * Created by gpicron on 08/08/2016.
 */
public class BeanGeneratorBuilder<CLASS> extends AbstractGeneratorBuilder<CLASS> {
    private final MethodAccess access;

    private final SortedMap<String, Generator> populators = new TreeMap<>();
    private final LinkedHashMap<Object, Generator> populators2 = new LinkedHashMap<>();
    private String idfield;
    private List<Generator<? extends CombinedFieldValue<CLASS>>> updateOfPopulators = new ArrayList();

    public BeanGeneratorBuilder(Class<CLASS> type) {
        super(type);
        access = MethodAccess.get(type);
    }

    protected void assertFieldTypeSupported() {}

    @Override
    public Generator<CLASS> build() {
        final ConstructorAccess<CLASS> constructorAccess = ConstructorAccess.get(fieldType);
        final ArrayList<Populator> pops = new ArrayList<>();
        populators.entrySet().stream()
                .map(e -> new Populator(getValueAssigner(e.getKey()), e.getValue()))
                .collect(Collectors.toCollection(() -> pops));
        populators2.entrySet().stream()
                .map(e -> {
                    if (e.getKey() instanceof Tuple3) {
                        Tuple3<String, String, String> k = (Tuple3<String, String, String>) e.getKey();
                        return new Populator(getValueAssigner(k.getT1(), k.getT2(), k.getT3()), e.getValue());
                    } else if (e.getKey() instanceof Tuple2) {
                        Tuple2<String,String> k = (Tuple2<String,String>) e.getKey();
                        return new Populator(getValueAssigner(k.getT1(), k.getT2()), e.getValue());
                    } else {
                        throw new RuntimeException("bug");
                    }

                })
                .collect(Collectors.toCollection(() -> pops));
        updateOfPopulators.stream()
                .map(updateGenerator -> new Populator(new ValueAssigner<CLASS, CombinedFieldValue<CLASS>>() {
                    @Override
                    public void assign(CLASS object, CombinedFieldValue<CLASS> updateOf) {
                        if (updateOf != null) updateOf.apply(object);
                    }

                    @Override
                    public boolean accept(Class<? extends CombinedFieldValue<CLASS>> type) {
                        return true;
                    }

                }, updateGenerator))
                .collect(Collectors.toCollection(() -> pops));;

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

    public BeanGeneratorBuilder<CLASS> with(AbstractGeneratorBuilder<CombinedFieldValue<CLASS>> generator) {
        return with(generator.build());
    }

    public <VALUE> BeanGeneratorBuilder<CLASS> with(String fieldName, AbstractGeneratorBuilder<VALUE> generator) {
        return with(fieldName, generator.build());

    }

    public BeanGeneratorBuilder<CLASS> with(Generator<? extends CombinedFieldValue<CLASS>> generator) {
        updateOfPopulators.add(generator);
        return this;
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

    public <FIELD1,FIELD2> BeanGeneratorBuilder<CLASS> with(String fieldName1, String fieldName2, AbstractGeneratorBuilder<Tuple2<FIELD1,FIELD2>> generator) {
        return with(fieldName1,fieldName2, generator.build());
    }
    public <FIELD1,FIELD2, FIELD3> BeanGeneratorBuilder<CLASS> with(String fieldName1, String fieldName2, String fieldName3, AbstractGeneratorBuilder<Tuple3<FIELD1,FIELD2,FIELD3>> generator) {
        return with(fieldName1,fieldName2, fieldName3, generator.build());
    }

    public <FIELD1,FIELD2> BeanGeneratorBuilder<CLASS> with(String fieldName1, String fieldName2, Generator<Tuple2<FIELD1,FIELD2>> generator) {
        final ValueAssigner<CLASS,Tuple2<FIELD1,FIELD2>> valueAssigner = getValueAssigner(fieldName1, fieldName2);
        if (valueAssigner.accept(generator.getType())) {
            populators2.put(Tuples.of(fieldName1,fieldName2), generator);
        } else {
            throw new IllegalArgumentException("The field '" + fieldName1 + "," + fieldName2 + "' cannot be set to type accept with type " + generator.getType());
        }

        return this;
    }
    public <FIELD1,FIELD2, FIELD3> BeanGeneratorBuilder<CLASS> with(String fieldName1, String fieldName2, String fieldName3, Generator<Tuple3<FIELD1,FIELD2, FIELD3>> generator) {
        final ValueAssigner<CLASS,Tuple3<FIELD1,FIELD2,FIELD3>> valueAssigner = getValueAssigner(fieldName1, fieldName2, fieldName3);
        if (valueAssigner.accept(generator.getType())) {
            populators2.put(Tuples.of(fieldName1,fieldName2,fieldName3), generator);
        } else {
            throw new IllegalArgumentException("The field '" + fieldName1 + "," + fieldName2 + "' cannot be set to type accept with type " + generator.getType());
        }

        return this;
    }

    private <CLASS, FIELD1,FIELD2> ValueAssigner<CLASS,Tuple2<FIELD1,FIELD2>> getValueAssigner(String fieldName1, String fieldName2) {
        final ValueAssigner<CLASS, FIELD1> valueAssigner1 = getValueAssigner(fieldName1);
        final ValueAssigner<CLASS, FIELD2> valueAssigner2 = getValueAssigner(fieldName2);
        return new ValueAssigner<CLASS, Tuple2<FIELD1, FIELD2>>() {
            @Override
            public void assign(CLASS object, Tuple2<FIELD1, FIELD2> values) {
                valueAssigner1.assign(object, values.getT1());
                valueAssigner2.assign(object, values.getT2());
            }

            @Override
            public boolean accept(Class<? extends Tuple2<FIELD1, FIELD2>> type) {
                final ParameterizedType typeInfo = (ParameterizedType) type.getGenericSuperclass();
                if (typeInfo == null) return true;

                final Class<FIELD1> type1 = (Class<FIELD1>) typeInfo.getActualTypeArguments()[0];
                final Class<FIELD2> type2 = (Class<FIELD2>) typeInfo.getActualTypeArguments()[1];
                return valueAssigner1.accept(type1) && valueAssigner2.accept(type2);
            }
        };
    }
    private <CLASS, FIELD1,FIELD2,FIELD3> ValueAssigner<CLASS,Tuple3<FIELD1,FIELD2,FIELD3>> getValueAssigner(String fieldName1, String fieldName2, String fieldName3) {
        final ValueAssigner<CLASS, FIELD1> valueAssigner1 = getValueAssigner(fieldName1);
        final ValueAssigner<CLASS, FIELD2> valueAssigner2 = getValueAssigner(fieldName2);
        final ValueAssigner<CLASS, FIELD3> valueAssigner3 = getValueAssigner(fieldName3);
        return new ValueAssigner<CLASS, Tuple3<FIELD1,FIELD2,FIELD3>>() {
            @Override
            public void assign(CLASS object, Tuple3<FIELD1,FIELD2,FIELD3> values) {
                valueAssigner1.assign(object, values.getT1());
                valueAssigner2.assign(object, values.getT2());
                valueAssigner3.assign(object, values.getT3());
            }

            @Override
            public boolean accept(Class<? extends Tuple3<FIELD1,FIELD2,FIELD3>> type) {
                final ParameterizedType typeInfo = (ParameterizedType) type.getGenericSuperclass();
                if (typeInfo == null) return true;

                final Class<FIELD1> type1 = (Class<FIELD1>) typeInfo.getActualTypeArguments()[0];
                final Class<FIELD2> type2 = (Class<FIELD2>) typeInfo.getActualTypeArguments()[1];
                final Class<FIELD3> type3 = (Class<FIELD3>) typeInfo.getActualTypeArguments()[2];
                return valueAssigner1.accept(type1) && valueAssigner2.accept(type2) && valueAssigner3.accept(type3);
            }
        };
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
                if (parameterClass.isAssignableFrom(type)) {
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
