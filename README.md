# FastBeanGen - Fast Random Data Generator

FastBeanGen generates deterministic pseudo-random values, beans and datasets, with optional DOCX/XLSX generation.

## Java and build

**Java 21 or newer is required.** The supported build/test matrix is Java 21 and 25; library bytecode targets Java 21 in both cases. Use the checked-in Gradle 9.9.1 wrapper, not a global Gradle installation.

```sh
./gradlew --no-daemon --max-workers=1 -PjavaVersion=21 check assemble
./gradlew --no-daemon --max-workers=1 -PjavaVersion=25 check assemble
```

The Foojay resolver can provision a missing compilation/test toolchain. The existing large reproducibility tests need a 4 GiB test heap and can create about 2 GiB of temporary reference data under the module build directory.

The three modules are `fastbeangen`, `fastbeangen-doc`, and `fastbeangen-example`. See [modernization and migration notes](docs/modernization.md) for dependency versions, API adaptations, historical-reference coverage and bounded example checks.

## Local publication and consumption

**Version 0.3.0 is unreleased.** The following commands prepare and validate a local Maven repository; they do not upload artifacts or make this version available from Maven Central.

```sh
./gradlew --no-daemon --max-workers=1 -PjavaVersion=21 verificationTest assemble publishAllPublicationsToLocalRepository verifyPublication
./gradlew --project-dir verification/consumer --no-daemon -PjavaVersion=21 run
```

The repository is generated at `build/repository`. For a separate Gradle project:

```groovy
repositories {
    maven { url = uri('/absolute/path/to/fastbeangen/build/repository') }
    mavenCentral()
}
dependencies {
    implementation 'com.arboratum:fastbeangen:0.3.0'
    // Optional document generation:
    implementation 'com.arboratum:fastbeangen-doc:0.3.0'
}
```

For Maven, configure that same local file repository and use a normal JAR dependency, not `<type>pom</type>`:

```xml
<repositories>
  <repository>
    <id>fastbeangen-local</id>
    <url>file:///absolute/path/to/fastbeangen/build/repository</url>
  </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>com.arboratum</groupId>
    <artifactId>fastbeangen</artifactId>
    <version>0.3.0</version>
  </dependency>
</dependencies>
```

Maven Central is the intended release destination. Remove the local repository only after a release is actually published. Signing, namespace authorization and upload remain separate release prerequisites; see [publication preparation](docs/modernization.md#publication-preparation).

The [0.3.0 release notes and checklist](docs/releases/0.3.0.md) cover migration from the actual published 0.2.17 API, including bounded views, initial-entry provenance, entry-aware deletion and reference-aware updates. They distinguish the published reference's known defects from passing compatibility checks.

After local publication, prepare and inspect the unsigned GitHub asset set:

```sh
./gradlew --no-daemon --max-workers=1 -PjavaVersion=21 stageRelease verifyRelease
```

Outputs are confined to `build/release/0.3.0/`, with a source/artifact identity manifest and SHA-256 checksums. No tag, draft/release or upload is created. The Central bundle is a separate, signed path; unsigned local success does not establish key ownership, namespace rights or Portal acceptance.

## Examples

```sh
./gradlew --no-daemon --max-workers=1 -PjavaVersion=21 verifyExamples
```

This Gradle-driven Java/JUnit check runs the existing SQL generation, query, CUD and CSV entry points with bounded inputs/durations and embedded HSQLDB. It verifies successful results, not just process exit codes, within the unchanged shared 180-second deadline. The query phase takes one minute. The CUD example uses its existing 20 operations/second limiter and warm-up before processing each operation; six checkpoints and all CSV assertions remain unchanged. No separately provisioned database is needed.

All verification tests are Java/JUnit driven by Gradle, including publication/license/release guards; no Python runtime is required. See [verification commands and case mapping](verification/README.md) for the migrated guards and reference/signing checks. The verification sources are build-only, not a fourth application module or another publication.

## License

FastBeanGen's own code, examples, documentation and bundled dictionaries/document templates are licensed under the **GNU Lesser General Public License, version 3.0 only (`LGPL-3.0-only`)**, except where an existing third-party notice states otherwise. No automatic permission to use a later LGPL version is granted.

[LICENSE](LICENSE) contains the official LGPL v3 text. It incorporates GPL v3, whose full text is retained in [COPYING](COPYING). Both texts are included in the first-party binary, sources, Javadoc and example JARs produced by this build.

LGPL is weak copyleft, not a permissive MIT/Apache license. Applications may use other licenses, but distribution must meet the applicable LGPL conditions, including:

- retaining required notices, identifying use of the library, and providing both license texts;
- providing the corresponding source of the LGPL-covered library and modifications as required by the license;
- permitting modification of the library and reverse engineering to debug those modifications, and meeting the applicable replacement/relinking and installation-information requirements for combined works.

These points do not certify compliance for a particular application or packaging method; consult the complete license terms. The Gradle wrapper, dependencies and other identified third-party material retain their own licenses and notices. This change does not rewrite earlier releases or revoke GPL rights already granted. Version 0.3.0 remains unreleased.
