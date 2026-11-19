package com.arboratum.beangen.verification;

import groovy.json.JsonOutput;
import org.apache.tools.zip.ZipFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.arboratum.beangen.verification.PublicationVerification.*;

/** Local release preparation; structural signature fixtures never establish cryptographic validity. */
public final class ReleaseVerification {
    static final List<String> HASHES = List.of("MD5", "SHA-1", "SHA-256", "SHA-512");
    static final Set<String> PAYLOAD_NAMES = new TreeSet<>();
    static {
        for (String module : MODULES) for (String suffix : SUFFIXES) {
            if (!suffix.equals(".module")) PAYLOAD_NAMES.add(module + "-" + VERSION + suffix);
        }
        PAYLOAD_NAMES.add("RELEASE-NOTES.md");
    }

    private ReleaseVerification() { }

    static String extension(String algorithm) {
        return algorithm.toLowerCase(java.util.Locale.ROOT).replace("-", "");
    }

    static Set<String> names(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return new TreeSet<>(files.map(p -> p.getFileName().toString()).toList());
        }
    }

    static void directory(Path path) {
        check(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS), "Not a regular directory: " + path);
    }

    static void regularFile(Path path) {
        check(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS), "Not a regular file: " + path);
    }

    static void verifyChecksum(Path path, String algorithm, boolean required) throws IOException {
        Path checksum = path.resolveSibling(path.getFileName() + "." + extension(algorithm));
        if (required || Files.exists(checksum, LinkOption.NOFOLLOW_LINKS)) {
            regularFile(checksum);
            check(Files.readString(checksum).strip().equals(digest(Files.readAllBytes(path), algorithm)), checksum);
        }
    }

    static Map<String, Path> verifyRepository(Path repository, boolean signed) throws Exception {
        directory(repository);
        directory(repository.resolve("com"));
        Path namespace = repository.resolve("com/arboratum");
        directory(namespace);
        check(names(namespace).equals(new TreeSet<>(MODULES)), "Unexpected Maven module/input");
        Map<String, Path> files = new TreeMap<>();
        for (String module : MODULES) {
            Path parent = namespace.resolve(module);
            Path version = parent.resolve(VERSION);
            directory(parent);
            directory(version);
            Set<String> artifacts = new TreeSet<>();
            for (String suffix : SUFFIXES) artifacts.add(module + "-" + VERSION + suffix);
            Set<String> allowed = new HashSet<>();
            for (String artifact : artifacts) {
                for (String ending : List.of("", ".asc")) {
                    allowed.add(artifact + ending);
                    for (String algorithm : HASHES) allowed.add(artifact + ending + "." + extension(algorithm));
                }
            }
            check(allowed.containsAll(names(version)), "Unexpected Maven version input");
            for (String name : names(version)) {
                Path file = version.resolve(name);
                regularFile(file);
                files.put(repository.relativize(file).toString().replace('\\', '/'), file);
            }
            for (String name : artifacts) {
                Path file = version.resolve(name);
                regularFile(file);
                check(Files.size(file) > 0, file);
                for (String algorithm : HASHES) verifyChecksum(file, algorithm, !algorithm.equals("SHA-512"));
                Path signature = version.resolve(name + ".asc");
                if (signed) regularFile(signature);
                if (Files.exists(signature, LinkOption.NOFOLLOW_LINKS)) {
                    regularFile(signature);
                    var lines = Files.readString(signature, StandardCharsets.US_ASCII).strip().lines().toList();
                    check(lines.size() > 2 && lines.getFirst().equals("-----BEGIN PGP SIGNATURE-----")
                            && lines.getLast().equals("-----END PGP SIGNATURE-----"), signature);
                    for (String algorithm : HASHES) verifyChecksum(signature, algorithm, false);
                }
            }
        }
        verifyLibraries(repository);
        return files;
    }

    static Map<String, byte[]> payloads(Path repository, Path notes) throws IOException {
        Map<String, byte[]> result = new TreeMap<>();
        for (String module : MODULES) for (String suffix : SUFFIXES) {
            if (!suffix.equals(".module")) {
                String name = module + "-" + VERSION + suffix;
                result.put(name, Files.readAllBytes(repository.resolve("com/arboratum/" + module + "/" + VERSION + "/" + name)));
            }
        }
        regularFile(notes);
        check(Files.size(notes) > 0, notes);
        result.put("RELEASE-NOTES.md", Files.readAllBytes(notes));
        return result;
    }

    static Map<String, String> digests(Map<String, byte[]> files) {
        Map<String, String> result = new TreeMap<>();
        files.forEach((name, content) -> result.put(name, digest(content)));
        return result;
    }

    static byte[] checksumManifest(Map<String, byte[]> files) {
        var result = new StringBuilder();
        digests(files).forEach((name, hash) -> result.append(hash).append("  ").append(name).append('\n'));
        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    static Map<String, Object> verifyStage(Path directory, Map<String, byte[]> inputs, Map<String, Object> identity) throws IOException {
        directory(directory);
        var expected = new TreeSet<>(PAYLOAD_NAMES);
        expected.addAll(List.of("release-manifest.json", "SHA256SUMS"));
        check(names(directory).equals(expected), "Unexpected staged input");
        for (String name : expected) {
            Path file = directory.resolve(name);
            regularFile(file);
            check(((Number) Files.getAttribute(file, "unix:nlink", LinkOption.NOFOLLOW_LINKS)).longValue() == 1,
                    "Hardlinked staged file: " + file);
        }
        var manifest = json(Files.readAllBytes(directory.resolve("release-manifest.json")));
        check(Integer.valueOf(1).equals(manifest.get("schema")) && VERSION.equals(manifest.get("version"))
                && "unverified".equals(manifest.get("signing")), "Invalid release status");
        var files = new TreeMap<String, byte[]>();
        for (String name : PAYLOAD_NAMES) files.put(name, Files.readAllBytes(directory.resolve(name)));
        check(digests(files).equals(manifest.get("files")), "Staged digest/manifest input mismatch");
        if (inputs != null) {
            check(files.keySet().equals(inputs.keySet()), "Unexpected current inputs");
            for (String name : files.keySet()) check(Arrays.equals(files.get(name), inputs.get(name)), "Assets changed: " + name);
        }
        files.put("release-manifest.json", Files.readAllBytes(directory.resolve("release-manifest.json")));
        check(Arrays.equals(Files.readAllBytes(directory.resolve("SHA256SUMS")), checksumManifest(files)), "Checksum manifest mismatch");
        if (identity != null) check(identity.equals(manifest.get("source")), "Source identity changed; restage validated assets");
        return manifest;
    }

    static Map<String, Object> stage(Path repository, Path destination, Path notes, Map<String, Object> identity) throws Exception {
        verifyRepository(repository, false);
        var inputs = payloads(repository, notes);
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) verifyStage(destination, null, null);
        else Files.createDirectories(destination);
        Map<String, Object> manifest = new TreeMap<>();
        manifest.put("schema", 1);
        manifest.put("version", VERSION);
        manifest.put("source", identity);
        manifest.put("signing", "unverified");
        manifest.put("files", digests(inputs));
        var files = new TreeMap<>(inputs);
        files.put("release-manifest.json", (JsonOutput.prettyPrint(JsonOutput.toJson(manifest)) + "\n").getBytes(StandardCharsets.UTF_8));
        files.put("SHA256SUMS", checksumManifest(files));
        for (var entry : files.entrySet()) Files.write(destination.resolve(entry.getKey()), entry.getValue());
        verifyStage(destination, inputs, identity);
        System.out.println("Prepared " + files.size() + " local files at " + destination + "; signing/publication remain unverified");
        return manifest;
    }

    static byte[] git(Path root, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", root.toString()));
        command.addAll(List.of(args));
        var process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        byte[] output = process.getInputStream().readAllBytes();
        check(process.waitFor() == 0, "Git source-identity inspection failed");
        return output;
    }

    static Set<String> nulNames(byte[] bytes) {
        var names = new TreeSet<>(List.of(new String(bytes, StandardCharsets.UTF_8).split("\0")));
        names.remove("");
        return names;
    }

    static Map<String, Object> sourceFingerprint(Path root, Set<String> names, Set<String> baseline) throws Exception {
        var fingerprint = MessageDigest.getInstance("SHA-256");
        int count = 0;
        int deleted = 0;
        for (String name : new TreeSet<>(names)) {
            var parts = Set.copyOf(List.of(name.split("/")));
            String first = name.split("/", 2)[0];
            if (!Collections.disjoint(parts, Set.of("build", ".gradle", "__pycache__")) || name.startsWith("docs/comet/")) continue;
            if (!List.of("fastbeangen", "fastbeangen-doc", "fastbeangen-example", "verification", "gradle", "docs", ".github").contains(first)
                    && !Set.of("build.gradle", "settings.gradle", "gradle.properties", "gradlew", "gradlew.bat", "LICENSE", "COPYING", "README.md", "CHANGELOG.md").contains(name)) continue;
            Path source = root.resolve(name);
            fingerprint.update((name + "\0").getBytes(StandardCharsets.UTF_8));
            if (Files.exists(source, LinkOption.NOFOLLOW_LINKS)) {
                regularFile(source);
                fingerprint.update(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source)));
            } else {
                check(baseline.contains(name), "Missing untracked input: " + name);
                fingerprint.update("DELETED\0".getBytes(StandardCharsets.UTF_8));
                deleted++;
            }
            count++;
        }
        check(count > 0, "No source inputs identified");
        Map<String, Object> result = new TreeMap<>();
        result.put("sourceSha256", java.util.HexFormat.of().formatHex(fingerprint.digest()));
        result.put("sourceFileCount", count);
        result.put("deletedSourceCount", deleted);
        return result;
    }

    static Map<String, Object> sourceIdentity(Path root) throws Exception {
        var baseline = nulNames(git(root, "ls-tree", "-r", "--name-only", "-z", "HEAD"));
        var names = nulNames(git(root, "ls-files", "--cached", "--others", "--exclude-standard", "-z"));
        names.addAll(baseline);
        var result = sourceFingerprint(root, names, baseline);
        result.put("baseCommit", new String(git(root, "rev-parse", "HEAD"), StandardCharsets.UTF_8).strip());
        result.put("workingTreeModified", git(root, "status", "--porcelain").length > 0);
        result.put("boundary", "Product source/docs/build inputs, including tracked deletions; excludes generated output, Native formals and local bootstrap. Not an authorized release tag.");
        return result;
    }

    static void verifyCentralBundle(Path path, Path repository) throws Exception {
        var files = verifyRepository(repository, true);
        regularFile(path);
        check(Files.size(path) < 1_000_000_000, path);
        Set<String> directories = new HashSet<>();
        for (String name : files.keySet()) {
            for (Path parent = Path.of(name).getParent(); parent != null; parent = parent.getParent()) {
                directories.add(parent.toString().replace('\\', '/') + "/");
            }
        }
        try (var archive = new ZipFile(path.toFile())) {
            Set<String> seen = new HashSet<>();
            Set<String> payloads = new HashSet<>();
            for (var entry : Collections.list(archive.getEntries())) {
                String name = entry.getName();
                check(seen.add(name), "Duplicate ZIP entry: " + name);
                check((entry.isDirectory() ? directories : files.keySet()).contains(name), "Unexpected ZIP entry: " + name);
                int type = entry.getUnixMode() & 0170000;
                check(type == 0 || type == (entry.isDirectory() ? 0040000 : 0100000), "Special/link ZIP entry: " + name);
                if (entry.isDirectory()) {
                    check(entry.getSize() == 0, "Directory entry contains foreign bytes");
                    read(archive, entry);
                } else {
                    Path original = files.get(name);
                    check(entry.getSize() == Files.size(original), "ZIP size changed: " + name);
                    check(Arrays.equals(read(archive, entry), Files.readAllBytes(original)), "ZIP content changed: " + name);
                    payloads.add(name);
                }
            }
            check(payloads.equals(files.keySet()), "Incomplete Central bundle");
        }
        System.out.println("Central structure/content checked; cryptographic signatures and Portal rights/acceptance NOT verified");
    }

    public static void main(String[] args) throws Exception {
        check(args.length == 1, "Expected stage|verify|central-inputs|central-bundle");
        for (String relative : List.of("build", "build/repository", "build/release", "build/distributions")) {
            check(!Files.isSymbolicLink(ROOT.resolve(relative)), relative);
        }
        Path repository = ROOT.resolve("build/repository");
        Path destination = ROOT.resolve("build/release/" + VERSION);
        Path notes = ROOT.resolve("docs/releases/" + VERSION + ".md");
        switch (args[0]) {
            case "stage" -> stage(repository, destination, notes, sourceIdentity(ROOT));
            case "verify" -> {
                verifyRepository(repository, false);
                verifyStage(destination, payloads(repository, notes), sourceIdentity(ROOT));
                System.out.println("Local assets verified; no upload, tag or publication performed");
            }
            case "central-inputs" -> {
                verifyRepository(repository, true);
                System.out.println("Signed input structure checked; real signature verification/Portal rights remain external gates");
            }
            case "central-bundle" -> verifyCentralBundle(ROOT.resolve("build/distributions/fastbeangen-central.zip"), repository);
            default -> throw new IllegalArgumentException("Unknown release action: " + args[0]);
        }
    }
}
