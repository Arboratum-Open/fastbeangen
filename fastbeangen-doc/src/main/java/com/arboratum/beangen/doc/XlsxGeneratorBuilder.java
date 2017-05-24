package com.arboratum.beangen.doc;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.core.AbstractGeneratorBuilder;
import com.arboratum.beangen.util.RandomSequence;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jxls.template.SimpleExporter;

import java.io.*;
import java.nio.CharBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.arboratum.beangen.BaseBuilders.aBean;
import static com.arboratum.beangen.BaseBuilders.aCharSequence;

/**
 * Created by gpicron on 22/04/2017.
 */
public class XlsxGeneratorBuilder extends AbstractGeneratorBuilder<File> {
    public static final ImmutableSet<Class> SUPPORTED_TYPES = ImmutableSet.of(
            File.class
    );


    private Generator<?> dataGenerator;
    private LongFunction<File> targetFile;
    private SimpleExporter template;
    private Generator<Integer> numRowGenerator;
    private LinkedHashMap<String, String> headerMapping = new LinkedHashMap<>();


    public XlsxGeneratorBuilder() {
        super(File.class);
    }

    public XlsxGeneratorBuilder withHeader(String header, String beanField) {
        headerMapping.put(header, beanField);

        return this;
    }

    public XlsxGeneratorBuilder withRow(AbstractGeneratorBuilder<?> generator) {
        this.dataGenerator = generator.build();

        return this;
    }
    public XlsxGeneratorBuilder withNumberOfRows(int constant) {
        this.numRowGenerator = new Generator<Integer>(Integer.class) {
            public Integer generate(RandomSequence register) {
                return constant;
            }
        };

        return this;
    }

    public XlsxGeneratorBuilder withNumberOfRows(Generator<Integer> generator) {
        this.numRowGenerator = generator;

        return this;
    }

    public XlsxGeneratorBuilder withNumberOfRows(AbstractGeneratorBuilder<Integer> generator) {
        this.numRowGenerator = generator.build();

        return this;
    }
    private static final char[] CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0987654321".toCharArray();

    public XlsxGeneratorBuilder withSize(final LongFunction<Integer> sizeGen) {
        final int numChars = 3123;

        SimpleRegression sizeInference = new SimpleRegression();
        if (sizeInference.getN() == 0) {
            for (int i = 0; i < 10; i++) {
                final File tempFile;
                try {
                    tempFile = File.createTempFile("reg-", "-" + i);
                    tempFile.delete();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                final int numRow = 10 * (i+1);
                final Generator<File> sample = new XlsxGeneratorBuilder()
                        .withHeader("Col1", "col1")
                        .withHeader("Col2", "col2")
                        .withHeader("Col3", "col3")
                        .withRow(aBean(Row.class)
                                        .with("col1", aCharSequence(CharSequence.class).alphaNumeric(numChars, numChars))
                                        .with("col2", aCharSequence(CharSequence.class).alphaNumeric(numChars, numChars))
                                        .with("col3", aCharSequence(CharSequence.class).alphaNumeric(numChars, numChars))
                                )
                        .withNumberOfRows(10 * (i+1))
                        .withTargetFileName(id -> tempFile)
                        .build();

                sample.generate(1);
                System.out.println(tempFile.length() + " <- " + numRow);
                sizeInference.addData(tempFile.length(), numRow );

                tempFile.delete();
            }

        }

        this.withNumberOfRows(new Generator<Integer>(Integer.class) {
            @Override
            public Integer generate(RandomSequence register) {
                return (int)sizeInference.predict(sizeGen.apply(register.getSeed()));
            }
        });


        this.withHeader("Col1", "col1");
        this.withHeader("Col2", "col2");
        this.withHeader("Col3", "col3");
        this.dataGenerator = new Generator<Row>(Row.class) {


            @Override
            public Row generate(RandomSequence register) {
                Row r = new Row();
                r.col1 = cell(register, numChars);
                r.col2 = cell(register, numChars);
                r.col3 = cell(register, numChars);

                return r;
            }

            private CharSequence cell(RandomSequence register, int size) {
                final char[] buffer = new char[size];

                for (int i = 0; i < size; i++) {
                    buffer[i] = CHARS[register.nextInt(CHARS.length)];
                }

                return CharBuffer.wrap(buffer);
            }
        };

        return this;
    }

    public XlsxGeneratorBuilder withTargetFileName(LongFunction<File> targetFile) {
        this.targetFile = targetFile;

        return this;
    }


    @Override
    public Generator<File> build() {

        template = new SimpleExporter() ;
        try {
            template.registerGridTemplate(this.getClass().getResourceAsStream("/fastbeangen/default.xlsx"));
        } catch (IOException e) {
            throw new RuntimeException("Bug", e);
        }

        final Iterable<String> headers = this.headerMapping.keySet();
        final String mapFields = this.headerMapping.values().stream().collect(Collectors.joining(","));

        setup(randomSequence -> {
            File target = targetFile.apply(randomSequence.getSeed());
            final File parentFile = target.getParentFile();
            if ((parentFile.exists() && parentFile.isDirectory()) || parentFile.mkdirs()) {

                try (OutputStream out = new FileOutputStream( target )) {
                    final List<?> rows = Stream.generate(() -> dataGenerator.generate(randomSequence))
                            .limit(numRowGenerator.generate(randomSequence.getSeed()))
                            .collect(Collectors.toList());

                    template.gridExport(headers, rows, mapFields, out);

                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new IllegalArgumentException(parentFile + " is not a directory or cannot be created");
            }

            return target;

        });
        return super.build();
    }


    public static final class Row {
        public CharSequence getCol1() {
            return col1;
        }

        public void setCol1(CharSequence col1) {
            this.col1 = col1;
        }

        public CharSequence getCol2() {
            return col2;
        }

        public void setCol2(CharSequence col2) {
            this.col2 = col2;
        }

        public CharSequence getCol3() {
            return col3;
        }

        public void setCol3(CharSequence col3) {
            this.col3 = col3;
        }

        private CharSequence col1;
        private CharSequence col2;
        private CharSequence col3;
    }
}
