package com.arboratum.beangen.verification;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.arboratum.beangen.verification.PublicationVerification.*;
import static com.arboratum.beangen.verification.ReleaseVerification.*;
import static org.junit.Assert.*;

/** All signatures and class bytes below are structural fixtures, never real release inputs. */
public class ReleaseTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    Path root;
    Path repository;
    Path notes;
    Path output;
    final Map<String, Object> identity = Map.of("baseCommit", "fixture", "sourceSha256", "0".repeat(64));

    @Before public void fixture() throws Exception {
        root = temporary.getRoot().toPath();
        repository = root.resolve("repository");
        notes = root.resolve("notes.md");
        Files.writeString(notes, "# Unreleased test notes\n");
        output = root.resolve("release");
        for (String module : MODULES) {
            Path directory = repository.resolve("com/arboratum/" + module + "/" + VERSION);
            Files.createDirectories(directory);
            for (String suffix : SUFFIXES) {
                Path file = directory.resolve(module + "-" + VERSION + suffix);
                byte[] payload;
                if (suffix.endsWith(".jar")) {
                    var entries = new ArrayList<VerificationFixtures.Entry>();
                    for (String name : List.of("LICENSE", "COPYING")) {
                        entries.add(new VerificationFixtures.Entry("META-INF/" + name, Files.readAllBytes(ROOT.resolve(name))));
                    }
                    String name = switch (suffix) {
                        case ".jar" -> "Demo.class";
                        case "-sources.jar" -> "Demo.java";
                        default -> "index.html";
                    };
                    entries.add(new VerificationFixtures.Entry(name, new byte[] {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe, 0, 0, 0, 65}));
                    VerificationFixtures.zip(file, entries);
                    payload = Files.readAllBytes(file);
                } else if (suffix.equals(".pom")) {
                    payload = ("<project xmlns=\"" + POM_NS + "\"><groupId>com.arboratum</groupId><artifactId>" + module
                            + "</artifactId><version>" + VERSION + "</version><name>Fixture</name><description>Fixture</description>"
                            + "<url>https://example.invalid</url><licenses><license><name>" + LICENSE_NAME + "</name><url>" + LICENSE_URL
                            + "</url></license></licenses><developers><developer><id>fixture</id><name>Fixture</name></developer></developers>"
                            + "<scm><connection>scm:git:fixture</connection><url>https://example.invalid</url></scm></project>")
                            .getBytes(StandardCharsets.UTF_8);
                } else {
                    payload = ("{\"variants\":[{\"attributes\":{\"org.gradle.category\":\"library\",\"org.gradle.usage\":\"java-api\",\"org.gradle.jvm.version\":21}},"
                            + "{\"attributes\":{\"org.gradle.category\":\"library\",\"org.gradle.usage\":\"java-runtime\",\"org.gradle.jvm.version\":21}}]}")
                            .getBytes(StandardCharsets.UTF_8);
                }
                writeArtifact(file, payload);
            }
        }
    }

    void writeArtifact(Path path, byte[] payload) throws Exception {
        Files.write(path, payload);
        for (String algorithm : HASHES) {
            Files.writeString(path.resolveSibling(path.getFileName() + "." + extension(algorithm)), digest(payload, algorithm));
        }
    }

    Path primary() {
        return repository.resolve("com/arboratum/fastbeangen/" + VERSION + "/fastbeangen-" + VERSION + ".jar");
    }

    void signedFixture() throws Exception {
        for (String module : MODULES) for (String suffix : SUFFIXES) {
            writeArtifact(repository.resolve("com/arboratum/" + module + "/" + VERSION + "/" + module + "-" + VERSION + suffix + ".asc"),
                    "-----BEGIN PGP SIGNATURE-----\nstructural-fixture-not-a-real-signature\n-----END PGP SIGNATURE-----\n".getBytes(StandardCharsets.US_ASCII));
        }
    }

    void stage() throws Exception { ReleaseVerification.stage(repository, output, notes, identity); }

    Map<String, String> stagedDigests() throws Exception {
        Map<String, String> result = new TreeMap<>();
        for (String name : names(output)) result.put(name, digest(Files.readAllBytes(output.resolve(name))));
        return result;
    }

    List<VerificationFixtures.Entry> centralEntries() throws Exception {
        var entries = new ArrayList<VerificationFixtures.Entry>();
        for (var entry : verifyRepository(repository, true).entrySet()) {
            entries.add(new VerificationFixtures.Entry(entry.getKey(), Files.readAllBytes(entry.getValue())));
        }
        return entries;
    }

    @Test public void test_unsigned_stage_and_manifest_verify_and_restage() throws Exception {
        stage();
        var first = stagedDigests();
        stage();
        assertEquals(first, stagedDigests());
        assertEquals("unverified", verifyStage(output, null, null).get("signing"));
        assertEquals(11, first.size());
        assertFalse(first.keySet().stream().anyMatch(name -> name.endsWith(".asc")));
    }

    @Test public void test_missing_and_modified_required_checksums_fail() throws Exception {
        for (String algorithm : List.of("md5", "sha1", "sha256")) {
            Path path = primary().resolveSibling(primary().getFileName() + "." + algorithm);
            byte[] original = Files.readAllBytes(path);
            Files.delete(path);
            assertThrows(algorithm, IllegalStateException.class, () -> verifyRepository(repository, false));
            Files.writeString(path, "0".repeat(64));
            assertThrows(algorithm, IllegalStateException.class, () -> verifyRepository(repository, false));
            Files.write(path, original);
        }
    }

    @Test public void test_missing_artifact_fails() throws Exception {
        Files.delete(primary());
        assertThrows(IllegalStateException.class, this::stage);
        assertFalse(Files.exists(output));
    }

    @Test public void test_invalid_license_fails() throws Exception {
        Path pom = primary().resolveSibling("fastbeangen-" + VERSION + ".pom");
        writeArtifact(pom, Files.readString(pom).replace("LGPL-3.0-only", "GPL-3.0-only").getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalStateException.class, this::stage);
    }

    @Test public void test_foreign_file_and_module_fail() throws Exception {
        Path foreign = primary().resolveSibling(".env");
        Files.writeString(foreign, "private-test-fixture");
        assertThrows(IllegalStateException.class, this::stage);
        Files.delete(foreign);
        Files.createDirectory(repository.resolve("com/arboratum/unrelated"));
        assertThrows(IllegalStateException.class, this::stage);
    }

    @Test public void test_symlink_input_fails() throws Exception {
        Path external = root.resolve("external.jar");
        Files.move(primary(), external);
        Files.createSymbolicLink(primary(), external);
        assertThrows(IllegalStateException.class, this::stage);
    }

    @Test public void test_central_requires_signature_for_every_artifact() throws Exception {
        assertThrows(IllegalStateException.class, () -> verifyRepository(repository, true));
        signedFixture();
        assertFalse(verifyRepository(repository, true).isEmpty());
        writeArtifact(primary().resolveSibling(primary().getFileName() + ".asc"), "not a signature".getBytes(StandardCharsets.US_ASCII));
        assertThrows(IllegalStateException.class, () -> verifyRepository(repository, true));
    }

    @Test public void test_changed_staged_file_fails_and_is_not_overwritten() throws Exception {
        stage();
        Path path = output.resolve("RELEASE-NOTES.md");
        Files.writeString(path, "manually changed");
        assertThrows(IllegalStateException.class, () -> verifyStage(output, null, null));
        assertThrows(IllegalStateException.class, this::stage);
        assertEquals("manually changed", Files.readString(path));
    }

    @Test public void test_foreign_staged_file_fails_and_is_preserved() throws Exception {
        stage();
        Path foreign = output.resolve("private.json");
        Files.writeString(foreign, "fixture");
        assertThrows(IllegalStateException.class, this::stage);
        assertTrue(Files.exists(foreign));
    }

    @Test public void test_central_archive_rejects_foreign_duplicate_or_changed_entries() throws Exception {
        signedFixture();
        var entries = centralEntries();
        Path archive = root.resolve("central.zip");
        VerificationFixtures.zip(archive, entries);
        verifyCentralBundle(archive, repository);
        for (String name : List.of("../private", "private.json", entries.getFirst().name())) {
            var extra = new ArrayList<>(entries);
            extra.add(new VerificationFixtures.Entry(name, "foreign".getBytes(StandardCharsets.UTF_8)));
            VerificationFixtures.zip(archive, extra);
            assertThrows(name, IllegalStateException.class, () -> verifyCentralBundle(archive, repository));
        }
        var changed = entries.stream().map(e -> new VerificationFixtures.Entry(e.name(), "changed".getBytes(StandardCharsets.UTF_8))).toList();
        VerificationFixtures.zip(archive, changed);
        assertThrows(IllegalStateException.class, () -> verifyCentralBundle(archive, repository));
    }

    @Test public void test_crlf_signature_armor_is_structurally_accepted() throws Exception {
        signedFixture();
        Path signature = primary().resolveSibling(primary().getFileName() + ".asc");
        writeArtifact(signature, Files.readString(signature).replace("\n", "\r\n").getBytes(StandardCharsets.US_ASCII));
        assertFalse(verifyRepository(repository, true).isEmpty());
    }

    @Test public void test_symlink_namespace_fails() throws Exception {
        Path original = repository.resolve("com");
        Path moved = root.resolve("external-com");
        Files.move(original, moved);
        Files.createSymbolicLink(original, moved);
        assertThrows(IllegalStateException.class, this::stage);
    }

    @Test public void test_central_archive_symlink_entry_fails() throws Exception {
        signedFixture();
        var entries = centralEntries();
        var first = entries.getFirst();
        entries.set(0, new VerificationFixtures.Entry(first.name(), first.bytes(), 0120777));
        Path archive = root.resolve("linked.zip");
        VerificationFixtures.zip(archive, entries);
        assertThrows(IllegalStateException.class, () -> verifyCentralBundle(archive, repository));
    }

    @Test public void test_hardlinked_staging_is_rejected_without_changing_backup() throws Exception {
        stage();
        Path backup = root.resolve("linked-backup.md");
        Files.createLink(backup, output.resolve("RELEASE-NOTES.md"));
        byte[] content = Files.readAllBytes(backup);
        Files.writeString(notes, "# Revised notes\n");
        assertThrows(IllegalStateException.class, this::stage);
        assertArrayEquals(content, Files.readAllBytes(backup));
    }

    @Test public void test_central_directory_payload_fails() throws Exception {
        signedFixture();
        var entries = centralEntries();
        entries.add(new VerificationFixtures.Entry("com/", "foreign".getBytes(StandardCharsets.UTF_8), 0040755));
        Path archive = root.resolve("directory-payload.zip");
        VerificationFixtures.zip(archive, entries);
        assertThrows(IllegalStateException.class, () -> verifyCentralBundle(archive, repository));
    }

    @Test public void test_identity_change_is_not_silently_accepted() throws Exception {
        stage();
        assertThrows(IllegalStateException.class, () -> verifyStage(output, null, Map.of("baseCommit", "different")));
    }

    @Test public void tracked_deletions_are_part_of_source_identity() throws Exception {
        Path source = root.resolve("verification/retired.py");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "fixture");
        Set<String> names = Set.of("verification/retired.py");
        var before = sourceFingerprint(root, names, names);
        Files.delete(source);
        var deleted = sourceFingerprint(root, names, names);
        assertNotEquals(before.get("sourceSha256"), deleted.get("sourceSha256"));
        assertEquals(1, deleted.get("deletedSourceCount"));
        assertEquals(deleted, sourceFingerprint(root, names, names));
        assertThrows(IllegalStateException.class, () -> sourceFingerprint(root, names, Set.of()));
    }
}
