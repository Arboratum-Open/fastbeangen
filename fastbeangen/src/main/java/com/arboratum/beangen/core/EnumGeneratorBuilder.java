package com.arboratum.beangen.core;

/**
 * Created by gpicron on 08/08/2016.
 */
public class EnumGeneratorBuilder<VALUE extends Enum<VALUE>> extends AbstractGeneratorBuilder<VALUE> {

    public EnumGeneratorBuilder(Class<VALUE> fieldType) {
        super(fieldType);
    }

    @Override
    protected void assertFieldTypeSupported() {
        if (!fieldType.isEnum()) throw new IllegalArgumentException(fieldType.toGenericString() + " is not a enum");
    }

    public EnumGeneratorBuilder<VALUE> uniform() {
        final VALUE[] enumConstants = fieldType.getEnumConstants();
        setup(r -> enumConstants[r.nextInt(enumConstants.length)]);
        return this;
    }

}
