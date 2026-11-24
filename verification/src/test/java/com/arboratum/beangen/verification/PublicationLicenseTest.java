package com.arboratum.beangen.verification;

import org.apache.tools.zip.ZipFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Element;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.arboratum.beangen.verification.PublicationVerification.*;
import static org.junit.Assert.assertThrows;

public class PublicationLicenseTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private Element pom(String name, String url) throws Exception {
        return PublicationVerification.pom(("<project xmlns=\"" + POM_NS + "\"><licenses><license><name>" + name
                + "</name><url>" + url + "</url></license></licenses></project>").getBytes(StandardCharsets.UTF_8));
    }

    private List<VerificationFixtures.Entry> legal() throws Exception {
        return List.of(new VerificationFixtures.Entry("META-INF/LICENSE", Files.readAllBytes(ROOT.resolve("LICENSE"))),
                new VerificationFixtures.Entry("META-INF/COPYING", Files.readAllBytes(ROOT.resolve("COPYING"))));
    }

    private void verify(List<VerificationFixtures.Entry> entries) throws Exception {
        var path = temporary.newFile().toPath();
        var all = new ArrayList<>(entries);
        all.add(new VerificationFixtures.Entry("fixture.txt", "Not a compiled application".getBytes(StandardCharsets.UTF_8)));
        VerificationFixtures.zip(path, all);
        try (var archive = new ZipFile(path.toFile())) { verifyArchive(archive); }
    }

    @Test public void test_valid_pom() throws Exception {
        verifyPomLicense(pom(LICENSE_NAME, LICENSE_URL));
    }

    @Test public void test_rejects_missing_pom_license() throws Exception {
        var document = PublicationVerification.pom(("<project xmlns=\"" + POM_NS + "\"/>").getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalStateException.class, () -> verifyPomLicense(document));
    }

    @Test public void test_rejects_wrong_pom_grant() throws Exception {
        for (String name : List.of("GNU General Public License, Version 3",
                "GNU Lesser General Public License, Version 3 or later (LGPL-3.0-or-later)")) {
            var document = pom(name, LICENSE_URL);
            assertThrows(name, IllegalStateException.class, () -> verifyPomLicense(document));
        }
    }

    @Test public void test_rejects_wrong_pom_url() throws Exception {
        var document = pom(LICENSE_NAME, "https://www.gnu.org/licenses/gpl-3.0.html");
        assertThrows(IllegalStateException.class, () -> verifyPomLicense(document));
    }

    @Test public void test_rejects_multiple_pom_licenses() throws Exception {
        var document = pom(LICENSE_NAME, LICENSE_URL);
        var licenses = elements(document, "licenses").getFirst();
        licenses.appendChild(elements(document, "licenses/license").getFirst().cloneNode(true));
        assertThrows(IllegalStateException.class, () -> verifyPomLicense(document));
    }

    @Test public void test_valid_archive() throws Exception {
        verify(legal());
    }

    @Test public void test_rejects_missing_or_incorrect_legal_texts() throws Exception {
        var legal = legal();
        for (var entries : List.of(List.of(legal.get(1)), List.of(legal.get(0)),
                List.of(new VerificationFixtures.Entry("META-INF/LICENSE", legal.get(1).bytes()), legal.get(1)),
                List.of(legal.get(0), new VerificationFixtures.Entry("META-INF/COPYING", "Altered GPL text".getBytes(StandardCharsets.UTF_8))))) {
            assertThrows(IllegalStateException.class, () -> verify(entries));
        }
    }

    @Test public void test_rejects_duplicate_legal_text() throws Exception {
        var entries = new ArrayList<>(legal());
        entries.add(entries.getFirst());
        assertThrows(IllegalStateException.class, () -> verify(entries));
    }
}
