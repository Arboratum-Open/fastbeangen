package com.arboratum.beangen.doc;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import com.arboratum.beangen.core.AbstractGeneratorBuilder;
import com.arboratum.beangen.util.RandomSequence;
import com.google.common.collect.ImmutableSet;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.io.*;
import java.nio.CharBuffer;
import java.util.function.LongFunction;

/**
 * Created by gpicron on 22/04/2017.
 */
public class DocxGeneratorBuilder extends AbstractGeneratorBuilder<File> {
    public static final ImmutableSet<Class> SUPPORTED_TYPES = ImmutableSet.of(
            File.class
    );

    private Generator<? extends CharSequence> dataGenerator;
    private LongFunction<File> targetFile;
    private IXDocReport ixDocReport;


    public DocxGeneratorBuilder() {
        super(File.class);
    }

    public DocxGeneratorBuilder withData(AbstractGeneratorBuilder<? extends CharSequence> generator) {
        this.dataGenerator = generator.build();

        return this;
    }

    private static final char[] CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0987654321".toCharArray();

    public DocxGeneratorBuilder withSize(final LongFunction<Integer> sizeGen) {
        final SimpleRegression sizeInference = new SimpleRegression();

        if (sizeInference.getN() == 0) {
            for (int i = 0; i < 10; i++) {
                final File tempFile;
                try {
                    tempFile = File.createTempFile("reg-", "-" + i);
                    tempFile.delete();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                final int numChars = (i+1) * 100123;
                final Generator<File> sample = new DocxGeneratorBuilder()
                        .withData(BaseBuilders.aString().alphaNumeric(numChars, numChars))
                        .withTargetFileName(id -> tempFile)
                        .build();
                sample.generate(1);

                sizeInference.addData(tempFile.length(), numChars );

                tempFile.delete();
            }
        }
        this.dataGenerator = new Generator<CharSequence>(CharSequence.class) {
            @Override
            public CharSequence generate(RandomSequence register) {
                final int size = (int) Math.max(0, sizeInference.predict(sizeGen.apply(register.getSeed())));
                final char[] buffer = new char[size];

                for (int i = 0; i < size; i++) {
                    buffer[i] = CHARS[register.nextInt(CHARS.length)];
                }

                return CharBuffer.wrap(buffer);
            }
        };

        return this;
    }

    public DocxGeneratorBuilder withTargetFileName(LongFunction<File> targetFile) {
        this.targetFile = targetFile;

        return this;
    }

    private DocxGeneratorBuilder withTemplate(File template) throws IOException, XDocReportException {
        InputStream in = new FileInputStream(template);
        ixDocReport = XDocReportRegistry.getRegistry().loadReport( in, TemplateEngineKind.Velocity );

        return this;
    }

    @Override
    public Generator<File> build() {
        try {
            ixDocReport = XDocReportRegistry.getRegistry().loadReport( this.getClass().getResourceAsStream("/fastbeangen/default.docx"), TemplateEngineKind.Velocity );
        } catch (IOException e) {
            throw new RuntimeException("Bug", e);
        } catch (XDocReportException e) {
            throw new RuntimeException("Bug", e);
        }

        setup(randomSequence -> {
            final CharSequence data = dataGenerator.generate(randomSequence);
            File target = targetFile.apply(randomSequence.getSeed());
            final File parentFile = target.getParentFile();
            if ((parentFile.exists() && parentFile.isDirectory()) || parentFile.mkdirs()) {

                try {
                    IContext context = ixDocReport.createContext();
                    context.put("DATA", data );

                    OutputStream out = new FileOutputStream( target );
                    ixDocReport.process( context, out );

                } catch (XDocReportException e) {
                    throw new RuntimeException(e);
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
}
