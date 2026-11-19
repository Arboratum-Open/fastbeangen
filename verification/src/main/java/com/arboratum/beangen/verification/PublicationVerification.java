package com.arboratum.beangen.verification;

import groovy.json.JsonSlurper;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

/** Local artifact inspection only: no uploads or signing-key access. */
public final class PublicationVerification {
    static final Path ROOT = Path.of(System.getProperty("fastbeangen.root", ".")).toAbsolutePath().normalize();
    static final String VERSION = "0.3.0";
    static final List<String> MODULES = List.of("fastbeangen", "fastbeangen-doc");
    static final List<String> SUFFIXES = List.of(".jar", "-sources.jar", "-javadoc.jar", ".pom", ".module");
    static final String LICENSE_NAME = "GNU Lesser General Public License, Version 3.0 only (LGPL-3.0-only)";
    static final String LICENSE_URL = "https://www.gnu.org/licenses/lgpl-3.0.html";
    static final String POM_NS = "http://maven.apache.org/POM/4.0.0";
    static final Map<String, String> LICENSE_HASHES = Map.of(
            "LICENSE", "e3a994d82e644b03a792a930f574002658412f62407f5fee083f2555c5f23118",
            "COPYING", "589ed823e9a84c56feb95ac58e7cf384626b9cbf4fda2a907bc36e103de1bad2");

    private PublicationVerification() { }

    static void check(boolean condition, Object message) {
        if (!condition) throw new IllegalStateException(String.valueOf(message));
    }

    static String digest(byte[] bytes, String algorithm) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String digest(byte[] bytes) {
        return digest(bytes, "SHA-256");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> json(byte[] bytes) {
        return (Map<String, Object>) new JsonSlurper().parseText(new String(bytes, StandardCharsets.UTF_8));
    }

    static Element pom(byte[] bytes) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
        factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes)).getDocumentElement();
    }

    static List<Element> elements(Element root, String path) {
        List<Element> current = List.of(root);
        for (String part : path.split("/")) {
            List<Element> next = new ArrayList<>();
            for (Element parent : current) {
                for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
                    if (node instanceof Element element && POM_NS.equals(element.getNamespaceURI())
                            && part.equals(element.getLocalName())) next.add(element);
                }
            }
            current = next;
        }
        return current;
    }

    static String text(Element root, String path) {
        var found = elements(root, path);
        return found.isEmpty() ? "" : found.getFirst().getTextContent();
    }

    static void verifyPomLicense(Element pom) {
        var licenses = elements(pom, "licenses/license");
        check(licenses.size() == 1, "Expected one first-party license");
        check(LICENSE_NAME.equals(text(licenses.getFirst(), "name")), "Expected LGPL-3.0-only");
        check(LICENSE_URL.equals(text(licenses.getFirst(), "url")), "Incorrect LGPL URL");
    }

    static byte[] read(ZipFile archive, ZipEntry entry) throws IOException {
        byte[] bytes;
        try (var input = archive.getInputStream(entry)) {
            bytes = input.readAllBytes();
        }
        var crc = new CRC32();
        crc.update(bytes);
        check(bytes.length == entry.getSize() && crc.getValue() == entry.getCrc(), "Corrupt ZIP entry: " + entry.getName());
        return bytes;
    }

    static void verifyArchive(ZipFile archive) throws IOException {
        var entries = Collections.list(archive.getEntries());
        check(!entries.isEmpty(), "Empty archive");
        for (var entry : entries) read(archive, entry);
        for (var legal : LICENSE_HASHES.entrySet()) {
            String name = "META-INF/" + legal.getKey();
            var matches = entries.stream().filter(e -> name.equals(e.getName())).toList();
            check(matches.size() == 1, "Expected exactly one " + name);
            check(legal.getValue().equals(digest(read(archive, matches.getFirst()))), "Incorrect " + name);
        }
    }

    static void verifyLibraries(Path repository) throws Exception {
        for (var legal : LICENSE_HASHES.entrySet()) {
            check(legal.getValue().equals(digest(Files.readAllBytes(ROOT.resolve(legal.getKey())))), legal.getKey());
        }
        for (String module : MODULES) {
            Path directory = repository.resolve("com/arboratum/" + module + "/" + VERSION);
            String stem = module + "-" + VERSION;
            Element pom = pom(Files.readAllBytes(directory.resolve(stem + ".pom")));
            for (var field : Map.of("groupId", "com.arboratum", "artifactId", module, "version", VERSION).entrySet()) {
                check(field.getValue().equals(text(pom, field.getKey())), module + ": " + field.getKey());
            }
            for (String field : List.of("name", "description", "url", "licenses/license/name", "licenses/license/url",
                    "developers/developer/id", "developers/developer/name", "scm/connection", "scm/url")) {
                check(!text(pom, field).isEmpty(), module + ": " + field);
            }
            verifyPomLicense(pom);
            for (String suffix : SUFFIXES) {
                Path file = directory.resolve(stem + suffix);
                check(Files.isRegularFile(file) && Files.size(file) > 0, file);
                check(Files.readString(file.resolveSibling(file.getFileName() + ".sha256")).strip()
                        .equals(digest(Files.readAllBytes(file))), file);
                if (suffix.endsWith(".jar")) {
                    try (var archive = new ZipFile(file.toFile())) {
                        verifyArchive(archive);
                        var entries = Collections.list(archive.getEntries());
                        if (suffix.equals(".jar")) {
                            var classes = entries.stream().filter(e -> e.getName().endsWith(".class")).toList();
                            check(!classes.isEmpty(), "No classes: " + file);
                            for (var entry : classes) {
                                byte[] bytes = read(archive, entry);
                                check(bytes.length >= 8 && ((bytes[6] & 255) << 8 | bytes[7] & 255) == 65,
                                        file + ": " + entry.getName());
                            }
                        } else if (suffix.equals("-sources.jar")) {
                            check(entries.stream().anyMatch(e -> e.getName().endsWith(".java")), file);
                        } else check(archive.getEntry("index.html") != null, file);
                    }
                }
            }
            var metadata = json(Files.readAllBytes(directory.resolve(stem + ".module")));
            var variants = (List<?>) metadata.get("variants");
            var libraries = variants.stream().map(v -> (Map<?, ?>) ((Map<?, ?>) v).get("attributes"))
                    .filter(a -> "library".equals(a.get("org.gradle.category"))
                            && List.of("java-api", "java-runtime").contains(a.get("org.gradle.usage"))).toList();
            check(libraries.size() == 2, module + ": runtime variants");
            check(libraries.stream().allMatch(a -> Integer.valueOf(21).equals(a.get("org.gradle.jvm.version"))), module);
            System.out.println("Verified " + module + ":" + VERSION + ": POM, checksums, Java 21 binaries, sources, Javadoc and LGPL/GPL texts");
        }
    }

    public static void main(String[] args) throws Exception {
        verifyLibraries(ROOT.resolve("build/repository"));
        List<Path> examples;
        try (var paths = Files.list(ROOT.resolve("fastbeangen-example/build/libs"))) {
            examples = paths.filter(p -> p.toString().endsWith(".jar")).sorted().toList();
        }
        check(!examples.isEmpty(), "Run assemble before verification");
        for (Path file : examples) {
            try (var archive = new ZipFile(file.toFile())) { verifyArchive(archive); }
            System.out.println("Verified example legal texts: " + file.getFileName());
        }
    }
}
