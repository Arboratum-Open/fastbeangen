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
./gradlew --no-daemon publishAllPublicationsToLocalRepository
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

## Examples and license

```sh
python3 verification/verify-examples.py 21
```

This runs the existing SQL generation, query, CUD and CSV entry points with bounded inputs/durations and embedded HSQLDB. It verifies successful results, not just process exit codes. The query phase takes one minute. No separately provisioned database is needed.

The existing GNU GPL version 3 license is unchanged; see [LICENSE](LICENSE).
