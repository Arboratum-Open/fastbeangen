package com.arboratum.beangen.verification;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class VerificationFixtures {
    record Entry(String name, byte[] bytes, int mode) {
        Entry(String name, byte[] bytes) { this(name, bytes, 0100644); }
    }

    static void zip(Path destination, List<Entry> entries) throws Exception {
        try (var zip = new ZipOutputStream(Files.newOutputStream(destination))) {
            zip.setEncoding("UTF-8");
            for (var input : entries) {
                var entry = new ZipEntry(input.name());
                entry.setUnixMode(input.mode());
                zip.putNextEntry(entry);
                zip.write(input.bytes());
                zip.closeEntry();
            }
        }
    }

    private VerificationFixtures() { }
}
