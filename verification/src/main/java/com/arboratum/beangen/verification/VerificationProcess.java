package com.arboratum.beangen.verification;

import java.util.concurrent.TimeUnit;

final class VerificationProcess {
    private VerificationProcess() { }

    static void terminate(Process process) throws Exception {
        // Gradle's wrapper owns a single-use daemon; killing only the wrapper leaves a writer behind.
        var descendants = process.descendants().toList();
        for (var child : descendants.reversed()) child.destroyForcibly();
        process.destroyForcibly();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Owned process did not terminate: " + process.pid());
        }
        for (var child : descendants) {
            if (child.isAlive()) child.onExit().get(10, TimeUnit.SECONDS);
        }
    }
}
