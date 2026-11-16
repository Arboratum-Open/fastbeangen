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
if not __debug__:
    raise RuntimeError("Run this verification without Python optimization")

for artifact in ("fastbeangen", "fastbeangen-doc"):
    directory = ROOT / "build/repository/com/arboratum" / artifact / VERSION
    stem = f"{artifact}-{VERSION}"
    pom = ET.parse(directory / f"{stem}.pom").getroot()
    for field, expected in (("groupId", "com.arboratum"), ("artifactId", artifact), ("version", VERSION)):
        assert pom.findtext(f"m:{field}", namespaces=NS) == expected, (artifact, field)
    for field in ("name", "description", "url", "licenses/license/name", "licenses/license/url",
                  "developers/developer/id", "developers/developer/name", "scm/connection", "scm/url"):
        assert pom.findtext("m:" + field.replace("/", "/m:"), namespaces=NS), (artifact, field)
    assert "General Public License" in pom.findtext("m:licenses/m:license/m:name", namespaces=NS)
    for suffix in (".jar", "-sources.jar", "-javadoc.jar", ".pom", ".module"):
        path = directory / f"{stem}{suffix}"
        assert path.is_file() and path.stat().st_size > 0, path
        assert path.with_name(path.name + ".sha256").read_text().strip() == hashlib.sha256(path.read_bytes()).hexdigest(), path
        if suffix.endswith(".jar"):
            with zipfile.ZipFile(path) as archive:
                assert archive.testzip() is None, path
                assert archive.namelist(), path
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
    print(f"Verified {artifact}:{VERSION}: POM, checksums, binaries (Java 21), sources and Javadoc")
