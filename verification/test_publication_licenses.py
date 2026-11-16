"""Focused negative tests for the publication verifier's legal-text checks."""
import importlib.util
import io
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
import zipfile

SPEC = importlib.util.spec_from_file_location("publication", Path(__file__).with_name("verify-publication.py"))
publication = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(publication)
NAME = "GNU Lesser General Public License, Version 3.0 only (LGPL-3.0-only)"
URL = "https://www.gnu.org/licenses/lgpl-3.0.html"
LEGAL = {f"META-INF/{name}": (publication.ROOT / name).read_bytes() for name in ("LICENSE", "COPYING")}


def pom(name=NAME, url=URL):
    return ET.fromstring(f'<project xmlns="{publication.NS["m"]}"><licenses><license>'
                         f'<name>{name}</name><url>{url}</url></license></licenses></project>')


def verify_entries(entries):
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        archive.writestr("fixture.txt", "Not a compiled application; exercises archive checks only.")
        for name, content in entries:
            archive.writestr(name, content)
    with zipfile.ZipFile(buffer) as archive:
        publication.verify_archive(archive)


class PublicationLicenseTest(unittest.TestCase):
    def test_valid_pom(self):
        publication.verify_pom_license(pom())

    def test_rejects_missing_pom_license(self):
        with self.assertRaises(AssertionError):
            publication.verify_pom_license(ET.fromstring(f'<project xmlns="{publication.NS["m"]}"/>'))

    def test_rejects_wrong_pom_grant(self):
        for name in ("GNU General Public License, Version 3",
                     "GNU Lesser General Public License, Version 3 or later (LGPL-3.0-or-later)"):
            with self.subTest(name=name), self.assertRaises(AssertionError):
                publication.verify_pom_license(pom(name=name))

    def test_rejects_wrong_pom_url(self):
        with self.assertRaises(AssertionError):
            publication.verify_pom_license(pom(url="https://www.gnu.org/licenses/gpl-3.0.html"))

    def test_rejects_multiple_pom_licenses(self):
        document = pom()
        licenses = document.find("m:licenses", publication.NS)
        licenses.append(pom().find("m:licenses/m:license", publication.NS))
        with self.assertRaises(AssertionError):
            publication.verify_pom_license(document)

    def test_valid_archive(self):
        verify_entries(LEGAL.items())

    def test_rejects_missing_or_incorrect_legal_texts(self):
        cases = {
            "missing LGPL": {"META-INF/COPYING": LEGAL["META-INF/COPYING"]},
            "missing GPL": {"META-INF/LICENSE": LEGAL["META-INF/LICENSE"]},
            "incorrect LGPL": {**LEGAL, "META-INF/LICENSE": LEGAL["META-INF/COPYING"]},
            "incorrect GPL": {**LEGAL, "META-INF/COPYING": b"Altered GPL text"},
        }
        for name, entries in cases.items():
            with self.subTest(name=name), self.assertRaises(AssertionError):
                verify_entries(entries.items())

    def test_rejects_duplicate_legal_text(self):
        with self.assertWarns(UserWarning), self.assertRaises(AssertionError):
            verify_entries([*LEGAL.items(), ("META-INF/LICENSE", LEGAL["META-INF/LICENSE"])])


if __name__ == "__main__":
    unittest.main()
