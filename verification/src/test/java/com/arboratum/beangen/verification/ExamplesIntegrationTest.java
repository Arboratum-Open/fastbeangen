package com.arboratum.beangen.verification;

import org.junit.Test;

public class ExamplesIntegrationTest {
    @Test public void actual_bounded_examples_preserve_queries_checkpoints_and_csv() throws Exception {
        ExamplesVerification.run(System.getProperty("fastbeangen.exampleJava"), System.getProperty("fastbeangen.exampleClasspath"));
    }
}
