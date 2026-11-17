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
python3 -m unittest discover -s verification -p 'test_publication_licenses.py'
./gradlew --no-daemon --max-workers=1 assemble publishAllPublicationsToLocalRepository
python3 verification/verify-publication.py
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

## Examples

```sh
python3 verification/verify-examples.py 21
```

This runs the existing SQL generation, query, CUD and CSV entry points with bounded inputs/durations and embedded HSQLDB. It verifies successful results, not just process exit codes. The query phase takes one minute. No separately provisioned database is needed.

## License

FastBeanGen's own code, examples, documentation and bundled dictionaries/document templates are licensed under the **GNU Lesser General Public License, version 3.0 only (`LGPL-3.0-only`)**, except where an existing third-party notice states otherwise. No automatic permission to use a later LGPL version is granted.

[LICENSE](LICENSE) contains the official LGPL v3 text. It incorporates GPL v3, whose full text is retained in [COPYING](COPYING). Both texts are included in the first-party binary, sources, Javadoc and example JARs produced by this build.

LGPL is weak copyleft, not a permissive MIT/Apache license. Applications may use other licenses, but distribution must meet the applicable LGPL conditions, including:

- retaining required notices, identifying use of the library, and providing both license texts;
- providing the corresponding source of the LGPL-covered library and modifications as required by the license;
- permitting modification of the library and reverse engineering to debug those modifications, and meeting the applicable replacement/relinking and installation-information requirements for combined works.

These points do not certify compliance for a particular application or packaging method; consult the complete license terms. The Gradle wrapper, dependencies and other identified third-party material retain their own licenses and notices. This change does not rewrite earlier releases or revoke GPL rights already granted. Version 0.3.0 remains unreleased.
