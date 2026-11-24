package com.arboratum.beangen.verification;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class VerificationRuntimeTest {
    @Test public void runs_on_the_gradle_selected_runtime() {
        assertEquals(Integer.parseInt(System.getProperty("fastbeangen.javaVersion")), Runtime.version().feature());
        System.out.println("Verification JVM: " + Runtime.version());
    }
}
