package com.arboratum.beangen.verification;

import org.junit.Test;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class VerificationProcessTest {
    private static Process start(String... args) throws Exception {
        var command = new java.util.ArrayList<>(java.util.List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-cp",
                Path.of(WaitingProcess.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
                WaitingProcess.class.getName()));
        command.addAll(java.util.List.of(args));
        return new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
    }

    @Test public void timeout_cleanup_terminates_and_awaits_owned_parent_and_child() throws Exception {
        var process = start();
        try {
            String line = CompletableFuture.supplyAsync(() -> {
                try { return process.inputReader().readLine(); }
                catch (java.io.IOException e) { throw new UncheckedIOException(e); }
            }).get(10, TimeUnit.SECONDS);
            var child = ProcessHandle.of(Long.parseLong(line)).orElseThrow();
            assertTrue(process.isAlive());
            assertTrue(child.isAlive());
            VerificationProcess.terminate(process);
            assertFalse(process.isAlive());
            assertFalse(child.isAlive());
        } finally {
            VerificationProcess.terminate(process);
        }
    }

    public static class WaitingProcess {
        public static void main(String[] args) throws Exception {
            if (args.length == 0) {
                var child = start("leaf");
                System.out.println(child.pid());
                System.out.flush();
            }
            Thread.sleep(Duration.ofDays(1));
        }
    }
}
