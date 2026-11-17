"""Validate the unsigned local Maven repository; never upload or read signing keys."""
from pathlib import Path
import hashlib
import json
import struct
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = "0.3.0"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
LICENSE_NAME = "GNU Lesser General Public License, Version 3.0 only (LGPL-3.0-only)"
LICENSE_URL = "https://www.gnu.org/licenses/lgpl-3.0.html"
# Pin the official LGPL text and the GPL text retained from the pre-relicensing commit.
LICENSE_HASHES = {
    "LICENSE": "e3a994d82e644b03a792a930f574002658412f62407f5fee083f2555c5f23118",
    "COPYING": "589ed823e9a84c56feb95ac58e7cf384626b9cbf4fda2a907bc36e103de1bad2",
}
if not __debug__:
    raise RuntimeError("Run this verification without Python optimization")


def verify_pom_license(pom):
    licenses = pom.findall("m:licenses/m:license", NS)
    assert len(licenses) == 1, "Expected one first-party license"
    assert licenses[0].findtext("m:name", namespaces=NS) == LICENSE_NAME, "Expected LGPL-3.0-only"
    assert licenses[0].findtext("m:url", namespaces=NS) == LICENSE_URL, "Incorrect LGPL URL"


def verify_archive(archive):
    assert archive.testzip() is None, archive.filename
    names = archive.namelist()
    assert names, archive.filename
    for name, expected in LICENSE_HASHES.items():
        entry = f"META-INF/{name}"
        assert names.count(entry) == 1, (archive.filename, entry)
        assert hashlib.sha256(archive.read(entry)).hexdigest() == expected, (archive.filename, entry)


def main():
    for name, expected in LICENSE_HASHES.items():
        assert hashlib.sha256((ROOT / name).read_bytes()).hexdigest() == expected, name
    for artifact in ("fastbeangen", "fastbeangen-doc"):
        directory = ROOT / "build/repository/com/arboratum" / artifact / VERSION
        stem = f"{artifact}-{VERSION}"
        pom = ET.parse(directory / f"{stem}.pom").getroot()
        for field, expected in (("groupId", "com.arboratum"), ("artifactId", artifact), ("version", VERSION)):
            assert pom.findtext(f"m:{field}", namespaces=NS) == expected, (artifact, field)
        for field in ("name", "description", "url", "licenses/license/name", "licenses/license/url",
                      "developers/developer/id", "developers/developer/name", "scm/connection", "scm/url"):
            assert pom.findtext("m:" + field.replace("/", "/m:"), namespaces=NS), (artifact, field)
        verify_pom_license(pom)
        for suffix in (".jar", "-sources.jar", "-javadoc.jar", ".pom", ".module"):
            path = directory / f"{stem}{suffix}"
            assert path.is_file() and path.stat().st_size > 0, path
            assert path.with_name(path.name + ".sha256").read_text().strip() == hashlib.sha256(path.read_bytes()).hexdigest(), path
            if suffix.endswith(".jar"):
                with zipfile.ZipFile(path) as archive:
                    verify_archive(archive)
                    if suffix == ".jar":
                        classes = [name for name in archive.namelist() if name.endswith(".class")]
                        assert classes, path
                        for name in classes:
                            assert struct.unpack(">H", archive.read(name)[6:8])[0] == 65, (path, name)
                    elif suffix == "-sources.jar":
                        assert any(name.endswith(".java") for name in archive.namelist()), path
                    else:
                        assert "index.html" in archive.namelist(), path
        metadata = json.loads((directory / f"{stem}.module").read_text())
        runtime_variants = [variant for variant in metadata["variants"]
                            if variant["attributes"].get("org.gradle.category") == "library"
                            and variant["attributes"].get("org.gradle.usage") in ("java-api", "java-runtime")]
        assert len(runtime_variants) == 2, artifact
        assert all(variant["attributes"]["org.gradle.jvm.version"] == 21 for variant in runtime_variants), artifact
        print(f"Verified {artifact}:{VERSION}: POM, checksums, binaries (Java 21), sources, Javadoc and LGPL/GPL texts")
    examples = sorted((ROOT / "fastbeangen-example/build/libs").glob("*.jar"))
    assert examples, "Run assemble to build example JARs before verification"
    for path in examples:
        with zipfile.ZipFile(path) as archive:
            verify_archive(archive)
        print(f"Verified {path.name}: LGPL/GPL texts")


if __name__ == "__main__":
    main()
