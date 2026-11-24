package com.arboratum.beangen.verification;

import org.apache.tools.zip.ZipFile;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.spi.ToolProvider;

import static com.arboratum.beangen.verification.PublicationVerification.*;
import static com.arboratum.beangen.verification.ReleaseVerification.git;
import static com.arboratum.beangen.verification.ReleaseVerification.nulNames;
import static org.junit.Assert.*;

public class ReferenceIntegrationTest {
    static final String BASE = "4a267527c030a1016c113714fc334c2c4844cbdd";
    static final String PROTECTED_SPEC = "docs/comet/specs/modern-java-build/spec.md";

    static void assertProtectedInput(String name, byte[] original, byte[] current) {
        if (name.equals(PROTECTED_SPEC) && !Arrays.equals(original, current)) {
            assertEquals("Approved published Spec length", 19515, current.length);
            assertArrayEquals("Historical Spec prefix", original, Arrays.copyOf(current, original.length));
            assertEquals("Approved published Spec SHA-256", "1669cc4cccff23336e6a8a1ce8c4f4190b5cd3beeed1a2c028f3fae6c12ed195", digest(current));
        } else {
            assertArrayEquals("Protected input: " + name, original, current);
        }
    }

    private Map<String, Path> referenceArtifacts() throws Exception {
        var pins = json(Files.readAllBytes(ROOT.resolve("verification/published-compatibility/reference.json")));
        assertEquals("0.2.17", pins.get("version"));
        assertEquals("https://repo.maven.apache.org/maven2/com/arboratum", pins.get("repository"));
        var hashes = (Map<?, ?>) pins.get("sha256");
        assertEquals(6, hashes.size());
        var files = new TreeMap<String, Path>();
        var directory = ROOT.resolve("build/published-reference");
        Files.createDirectories(directory);
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        for (String module : MODULES) for (String suffix : List.of(".jar", "-sources.jar", ".pom")) {
            String name = module + "-0.2.17" + suffix;
            assertTrue("Missing pin: " + name, hashes.containsKey(name));
            Path file = directory.resolve(name);
            if (!Files.exists(file)) {
                var uri = URI.create(pins.get("repository") + "/" + module + "/0.2.17/" + name);
                var response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
                assertEquals("Reference download: " + uri, 200, response.statusCode());
                assertEquals("Downloaded reference hash: " + name, hashes.get(name), digest(response.body()));
                Files.write(file, response.body());
            }
            assertFalse(Files.isSymbolicLink(file));
            assertEquals("Pinned reference: " + name, hashes.get(name), digest(Files.readAllBytes(file)));
            files.put(name, file);
        }
        return files;
    }

    private String javap(String classpath, String name) {
        var output = new StringWriter();
        var error = new StringWriter();
        int result = ToolProvider.findFirst("javap").orElseThrow().run(new PrintWriter(output), new PrintWriter(error),
                "-public", "-s", "-classpath", classpath, name);
        assertEquals(error.toString(), 0, result);
        return output.toString();
    }

    private Set<String> descriptors(String output) {
        Set<String> result = new TreeSet<>();
        String member = null;
        for (String line : output.lines().map(String::strip).toList()) {
            if (line.startsWith("public ") && line.endsWith(";")) {
                String declaration = line.substring(0, line.length() - 1);
                int parameters = declaration.indexOf('(');
                String prefix = parameters < 0 ? declaration : declaration.substring(0, parameters);
                member = prefix.substring(prefix.lastIndexOf(' ') + 1);
            } else if (line.startsWith("descriptor:") && member != null) {
                result.add(member + " " + line.substring("descriptor:".length()).strip());
                member = null;
            }
        }
        return result;
    }

    @Test public void official_pins_and_public_descriptors_match_the_candidate() throws Exception {
        var files = referenceArtifacts();
        String reference = files.get("fastbeangen-0.2.17.jar") + java.io.File.pathSeparator + files.get("fastbeangen-doc-0.2.17.jar");
        String candidate = MODULES.stream().map(module -> ROOT.resolve("build/repository/com/arboratum/" + module + "/" + VERSION + "/" + module + "-" + VERSION + ".jar").toString())
                .collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        int classes = 0;
        int referenceCount = 0;
        int candidateCount = 0;
        for (String module : MODULES) {
            try (var jar = new ZipFile(files.get(module + "-0.2.17.jar").toFile())) {
                for (var entry : Collections.list(jar.getEntries())) {
                    if (!entry.getName().endsWith(".class")) continue;
                    String name = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                    String oldApi = javap(reference, name);
                    if (oldApi.lines().noneMatch(line -> line.startsWith("public ") && line.endsWith("{"))) continue;
                    var oldMembers = descriptors(oldApi);
                    String newApi = javap(candidate, name);
                    var currentMembers = descriptors(newApi);
                    classes++;
                    referenceCount += oldMembers.size();
                    candidateCount += currentMembers.size();
                    if (name.equals("com.arboratum.beangen.database.DataSet")) {
                        assertTrue(newApi.contains("com.arboratum.beangen.database.DataView"));
                        currentMembers.addAll(descriptors(javap(candidate, "com.arboratum.beangen.database.DataView")));
                    }
                    var missing = new TreeSet<>(oldMembers);
                    missing.removeAll(currentMembers);
                    assertTrue(name + " missing public descriptors: " + missing, missing.isEmpty());
                }
            }
        }
        assertEquals(59, classes);
        assertEquals(358, referenceCount);
        System.out.println("Finite public API comparison: " + classes + " public classes, " + referenceCount + " reference descriptors, " + candidateCount + " direct candidate descriptors; none missing after DataView inheritance resolution. Not exhaustive linkage/runtime evidence.");
    }

    @Test public void protected_baseline_inputs_are_byte_identical() throws Exception {
        Set<String> changedDatabase = Set.of("DataSet", "DataSetBuilder", "DataView", "Entry", "FilteredDataView", "UnionDataView", "UpdateGenerator");
        var paths = nulNames(git(ROOT, "ls-tree", "-r", "--name-only", "-z", BASE));
        var process = new ProcessBuilder("git", "-C", ROOT.toString(), "cat-file", "--batch")
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        int count = 0;
        int publishedSpecs = 0;
        try (var input = new BufferedInputStream(process.getInputStream()); var requests = process.getOutputStream()) {
            for (String name : paths) {
                boolean protectedPath = name.startsWith("docs/comet/archive/") || name.equals(PROTECTED_SPEC)
                        || Set.of("LICENSE", "COPYING", "settings.gradle", "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties").contains(name);
                for (String module : List.of("fastbeangen", "fastbeangen-doc", "fastbeangen-example")) {
                    if (name.startsWith(module + "/src/") || name.equals(module + "/build.gradle")) protectedPath = true;
                }
                if (name.startsWith("fastbeangen/src/main/java/com/arboratum/beangen/database/")
                        && changedDatabase.stream().anyMatch(type -> name.endsWith("/" + type + ".java"))) protectedPath = false;
                if (name.equals("fastbeangen-example/src/main/java/com/arboratum/beangen/example1/SimpleGenAndQueryTest.java")) protectedPath = false;
                if (!protectedPath) continue;
                requests.write((BASE + ":" + name + "\n").getBytes(StandardCharsets.UTF_8));
                requests.flush();
                var header = new ByteArrayOutputStream();
                for (int b; (b = input.read()) != '\n';) {
                    assertTrue("Unexpected Git EOF", b >= 0);
                    header.write(b);
                }
                String[] fields = header.toString(StandardCharsets.UTF_8).split(" ");
                assertEquals("blob", fields[1]);
                int length = Integer.parseInt(fields[2]);
                byte[] original = input.readNBytes(length);
                assertEquals(length, original.length);
                assertEquals('\n', input.read());
                byte[] current = Files.readAllBytes(ROOT.resolve(name));
                assertProtectedInput(name, original, current);
                if (name.equals(PROTECTED_SPEC) && !Arrays.equals(original, current)) publishedSpecs++;
                count++;
            }
        } finally {
            process.getOutputStream().close();
            if (process.isAlive()) process.destroy();
            process.waitFor();
        }
        assertTrue("Unexpectedly empty protection boundary", count > 200);
        System.out.println("Protected inputs from " + BASE + ": " + count + " checked; " + (count - publishedSpecs) + " fully byte-identical; " + publishedSpecs + " exact approved published Spec with unchanged historical prefix; seven database classes and the explicitly authorized CUD caller excluded.");
    }

    @Test public void published_runtime_reference_has_exactly_the_three_characterized_failures() throws Exception {
        assertEquals("Reference characterization uses the original Java 21 harness", 21, Runtime.version().feature());
        referenceArtifacts();
        Path report = ROOT.resolve("verification/published-compatibility/build/test-results/test/TEST-com.arboratum.beangen.database.PublishedApiCompatibilityTest.xml");
        Path log = ROOT.resolve("build/verification/published-reference.log");
        Files.createDirectories(log.getParent());
        long start = System.currentTimeMillis();
        var builder = new ProcessBuilder(ROOT.resolve("gradlew").toString(), "--project-dir", "verification/published-compatibility",
                "--no-daemon", "--console=plain", "--max-workers=1", "--rerun-tasks", "test").directory(ROOT.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
        var process = builder.start();
        boolean finished = false;
        try {
            finished = process.waitFor(240, TimeUnit.SECONDS);
            assertTrue("Reference execution timeout; " + log, finished);
            assertEquals("Published reference must remain a failed build; " + log, 1, process.exitValue());
        } finally {
            if (!finished) VerificationProcess.terminate(process);
        }
        assertTrue("Missing/stale reference XML", Files.getLastModifiedTime(report).toMillis() >= start);
        var suite = pom(Files.readAllBytes(report));
        for (var expected : Map.of("tests", "8", "failures", "3", "errors", "0", "skipped", "0").entrySet()) {
            assertEquals(expected.getKey(), expected.getValue(), suite.getAttribute(expected.getKey()));
        }
        Map<String, String> failures = new TreeMap<>();
        var tests = suite.getElementsByTagName("testcase");
        for (int i = 0; i < tests.getLength(); i++) {
            var test = (org.w3c.dom.Element) tests.item(i);
            var failed = test.getElementsByTagName("failure");
            if (failed.getLength() != 0) failures.put(test.getAttribute("name"), ((org.w3c.dom.Element) failed.item(0)).getAttribute("type"));
        }
        assertEquals(Map.of("finiteBudgetsPropagateThroughComposedAndUnionViews", "java.lang.NullPointerException",
                "transformedSelectionRetainsEntryAndReferenceProvenance", "java.lang.NullPointerException",
                "initialProvenanceIncludesDeletedSlots", "java.lang.AssertionError"), failures);
        System.out.println("Official unmodified 0.2.17 runtime: 8 tests, 5 passed, 3 exact characterized failures; actual Gradle exit 1 retained. Characterization success is not a passing reference build. Full log: " + log);
    }
}
