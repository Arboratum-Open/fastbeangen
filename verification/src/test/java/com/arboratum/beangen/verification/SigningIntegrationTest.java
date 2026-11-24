package com.arboratum.beangen.verification;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.arboratum.beangen.verification.PublicationVerification.*;
import static org.junit.Assert.*;

public class SigningIntegrationTest {
    @Test public void actual_bundle_task_rejects_missing_blank_and_unusable_signing_input() throws Exception {
        Path bundle = ROOT.resolve("build/distributions/fastbeangen-central.zip");
        byte[] previous = Files.exists(bundle) ? Files.readAllBytes(bundle) : null;
        var previousTime = Files.exists(bundle) ? Files.getLastModifiedTime(bundle) : null;
        for (String kind : List.of("missing", "blank", "unusable")) {
            Path log = ROOT.resolve("build/verification/signing-" + kind + ".log");
            Files.createDirectories(log.getParent());
            var builder = new ProcessBuilder(ROOT.resolve("gradlew").toString(), "--no-daemon", "--console=plain", "--max-workers=1",
                    "--project-cache-dir", ROOT.resolve("build/verification/signing-gradle-cache").toString(),
                    "-PjavaVersion=" + Runtime.version().feature(), "bundleCentral").directory(ROOT.toFile())
                    .redirectErrorStream(true).redirectOutput(log.toFile());
            builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
            builder.environment().remove("SIGNING_KEY");
            builder.environment().remove("SIGNING_PASSWORD");
            if (!kind.equals("missing")) builder.environment().put("SIGNING_KEY", kind.equals("blank") ? " " : "not-a-real-PGP-private-key");
            var process = builder.start();
            boolean finished = false;
            try {
                finished = process.waitFor(180, TimeUnit.SECONDS);
                assertTrue("Signing-negative timeout: " + log, finished);
                assertEquals("Expected actual bundle failure: " + log, 1, process.exitValue());
            } finally {
                if (!finished) VerificationProcess.terminate(process);
            }
            String output = Files.readString(log);
            assertTrue("Wrong failure: " + log, output.contains(kind.equals("unusable")
                    ? "Could not read PGP secret key" : "SIGNING_KEY is required for a signed Central bundle"));
            if (previous == null) assertFalse("Failed signing produced a bundle", Files.exists(bundle));
            else {
                assertArrayEquals("Failed signing changed an existing bundle", previous, Files.readAllBytes(bundle));
                assertEquals(previousTime, Files.getLastModifiedTime(bundle));
            }
            System.out.println("Actual Gradle bundle rejected " + kind + " signing input; existing outputs preserved; full log: " + log);
        }
    }
}
