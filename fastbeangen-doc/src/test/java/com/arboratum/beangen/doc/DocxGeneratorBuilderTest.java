package com.arboratum.beangen.doc;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

/**
 * Created by gpicron on 22/05/2017.
 */
public class DocxGeneratorBuilderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void withData() throws Exception {
        final File parentDir = temporaryFolder.newFolder();
        final Generator<File> generator = new DocxGeneratorBuilder()
                .withData(BaseBuilders.aString().alphaNumeric(10000, 30000))
                .withTargetFileName(id -> new File(parentDir, "test-" + id + ".docx"))
                .build();

        for (int i = 0; i < 10; i++) {
            final File generated = generator.generate(i);

            Assert.assertTrue(generated.exists());
            System.out.println(generated.length());
        }
    }

    @Test
    public void withSize() throws Exception {
        final File parentDir = new File("build/docx");
        final Generator<File> generator = new DocxGeneratorBuilder()
                .withSize(id -> (int)(id+1) * 200000)
                .withTargetFileName(id -> new File(parentDir, "test-" + id + ".docx"))
                .build();

        for (int i = 0; i < 20; i++) {
            final File generated = generator.generate(i);

            Assert.assertTrue(generated.exists());
            double targetSize = (i+1) * 200000;
            Assert.assertTrue(Math.abs(targetSize - generated.length()) / targetSize < 0.02); // less than 2 pct error
        }
    }


}