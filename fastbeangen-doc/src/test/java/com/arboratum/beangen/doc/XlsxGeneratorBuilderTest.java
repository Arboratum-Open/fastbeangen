package com.arboratum.beangen.doc;

import com.arboratum.beangen.Generator;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static com.arboratum.beangen.BaseBuilders.aBean;
import static com.arboratum.beangen.BaseBuilders.aCharSequence;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

/**
 * Created by gpicron on 22/05/2017.
 */
public class XlsxGeneratorBuilderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void withData() throws Exception {
        final File parentDir = temporaryFolder.newFolder();
        final Generator<File> generator = new XlsxGeneratorBuilder()
                .withHeader("Col1", "col1")
                .withHeader("Col2", "col2")
                .withHeader("Col3", "col3")
                .withRow(aBean(XlsxGeneratorBuilder.Row.class)
                        .with("col1", aCharSequence(CharSequence.class).alphaNumeric(10, 30))
                        .with("col2", aCharSequence(CharSequence.class).alphaNumeric(10, 30))
                        .with("col3", aCharSequence(CharSequence.class).alphaNumeric(10, 30)))
                .withNumberOfRows(10)
                .withTargetFileName(id -> new File(parentDir, "test-" + id + ".xlsx"))
                .build();

        for (int i = 0; i < 10; i++) {
            final File generated = generator.generate(i);

            Assert.assertTrue(generated.exists());
            System.out.println(generated.length());
        }
    }

    @Test
    public void withSize() throws Exception {
        final File parentDir = new File("build/xslx");
        final Generator<File> generator = new XlsxGeneratorBuilder()
                .withSize(id -> (int)(id+1) * 200000)
                .withTargetFileName(id -> new File(parentDir, "test-" + id + ".xlsx"))
                .build();

        for (int i = 0; i < 20; i++) {
            final File generated = generator.generate(i);

            Assert.assertTrue(generated.exists());
            double targetSize = (i+1) * 200000;
            Assert.assertThat((double)generated.length(), is(closeTo(targetSize, targetSize * 0.02)));
        }
    }


}