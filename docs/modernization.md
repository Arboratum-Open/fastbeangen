# Modern Java build and migration

## Supported build

FastBeanGen requires Java 21. Compile/test on Java 21 and 25 using the repository wrapper:

```sh
./gradlew --no-daemon --console=plain --max-workers=1 -PjavaVersion=21 check assemble publishAllPublicationsToLocalRepository
./gradlew --no-daemon --console=plain --max-workers=1 -PjavaVersion=25 check assemble publishAllPublicationsToLocalRepository
```

`javaVersion` selects the compiler, test launcher and example launcher; it is not just a test-runtime switch. `--release 21` remains fixed, including when compiling with Java 25. The checked-in Foojay resolver can download a missing toolchain. Java 27 is not a replacement for Java 25 verification.

The original large test scenarios are retained: the reproducibility test creates two arrays of 100 million doubles, and its file-based variant can write approximately 2 GiB. Test JVMs have a 4 GiB maximum heap. Use `--max-workers=1` to avoid concurrent large test JVMs. Generated test references stay under `fastbeangen/build/test-work`, not in source directories. These are existing tests, not a new benchmark campaign.

The wrapper is Gradle **9.9.1** with a pinned distribution SHA-256. Its official wrapper JAR SHA-256 is `621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8`. Compare with `shasum -a 256 gradle/wrapper/gradle-wrapper.jar` on macOS, or `sha256sum` on Linux. No global Gradle installation is required.

## Pinned dependencies

Resolution uses Maven Central and the Gradle plugin portal, not JCenter/Bintray. Plugins and direct dependencies are fixed versions; the example's transitive framework versions come from its pinned Spring Boot BOM.

| Area | Version / disposition |
| --- | --- |
| Spring Boot plugin and example BOM | 4.1.2 |
| Git changelog plugin | 3.4.2 |
| Foojay toolchain resolver | 1.0.0 |
| ReflectASM | 1.11.9 |
| dk.brics automaton | 1.13-1, new coordinate `dk.brics:automaton` |
| Guava | 33.8.0-jre |
| Commons Math | 3.6.1, retained stable API; no prerelease Math 4 substitution |
| Joda-Time | 2.15.1, not systematically replaced with java.time |
| Reactor core/test | 3.8.8 |
| RoaringBitmap | 1.6.29 |
| SLF4J and bridges / Logback | 2.0.20 / 1.6.6 |
| XDocReport components | 2.2.0 |
| Jxls / Jxls-POI | 3.2.0; POI transitives selected by this pinned dependency |
| Commons Pool / JCommander / OpenCSV | 2.13.1 / 3.0 / 5.12.0 |
| JUnit 4 / Hamcrest | 4.13.2 / 3.0 |
| Test-only PalDB / Snappy | 1.2.0 / 1.1.10.12 |

Commons Math and test-only PalDB retain their available stable versions; this is not a claim of active upstream maintenance or a comprehensive vulnerability audit. The old test scenarios are not removed to avoid testing them. HSQLDB, Hibernate and Spring components in the example are governed by Spring Boot 4.1.2. Library dependencies needed by public signatures remain exported; implementation dependencies remain runtime dependencies for published consumers.

## Necessary API and runtime adaptations

- Consumers need Java 21 bytecode/runtime support. Java 8 is no longer a supported consumer target.
- The examples use Jakarta persistence and transaction packages instead of `javax.persistence` / `javax.transaction`, current JPA operations instead of removed Hibernate Session methods, and the current JCommander/OpenCSV packages and construction APIs. Existing entry-point class names and Gradle task names remain available, including the historical `runGenCVS` spelling.
- Reactor no longer provides `TopicProcessor`. Manual operation acknowledgements use a bounded queue of eight elements, a single asynchronous consumer, and serialized producers that wait on overflow instead of dropping operations.
- The default dataset scheduler is now Reactor's managed `boundedElastic` scheduler because value generation, update selection and user triggers can involve synchronous blocking. Custom schedulers supplied with `setScheduler` must permit those operations. Union feeds and CSV database buffering also use blocking-capable workers. Operation generation remains ordered within each feed.
- Union feeds distinguish actual Reactor cancellation from other errors; an unrelated `IllegalStateException` is no longer silently mistaken for successful completion. This exposed and allowed repair of the new-Reactor blocking restriction rather than disguising empty CUD output as success.
- The CSV export's temporary Hibernate session uses managed person references for relationship IDs. It does not duplicate person records in relation buffers or disable Hibernate's transient-reference validation. The existing temporary database's disabled SQL foreign-key checks remain part of its pre-existing export-buffer design, not a newly added workaround.
- Updates continue to mutate generated entries in place. This change does not add immutable-entry update semantics.
- Obsolete HTML `<tt>` Javadoc tags were replaced by `{@code ...}`. Javadoc validation remains enabled. Existing missing-comment warnings do not mean the API is comprehensively documented or warning-free.

## Historical references and approved legacy repair

Baseline: `7cfaf9bfb699f38f02d5cb87cfe81fe162348c47`.

The original Gradle 2.14.1 wrapper cannot run on Java 21. The development reference harness therefore uses Gradle 9.9.1 / Java 21 with frozen original module sources, original dependency versions and Java 8 source/target settings. This is not a passing execution of the original Gradle runtime, nor proof that the entire historical project passed.

`fastbeangen/src/test/resources/historical-output.tsv` records genuine original-output vectors for IDs `0, 1, 2, 31, 100, 10000, 2147483647, 9223372036854775807`. The fixture covers successive PRNG draws, regex generation and date epoch milliseconds. Tests compare fixed values and repeated new-version generation, and preserve rejection of negative IDs. The pseudo-random implementation itself is unchanged.

Existing literal PRNG, bean, string, distribution and dataset expectations remain intact. Supplementary tests were also exercised against the frozen old implementation to establish concurrent acknowledgement, mutable-entry update, union-feed and bounded DOCX/XLSX content contracts. Modern-Reactor scheduler regressions were reproduced before repairing the shared source.

The owner explicitly approved repairing an inherited internal dataset-constructor bug: the short constructor passed a null update-generator list, preventing four existing dataset tests and the Monte Carlo initialization path from running. The repair supplies an empty list and restores the internal version-only update path without materializing entries or adding an update-generator RNG draw. The four existing scenarios keep all original assertions, including their literal ten-operation sequences. The Monte Carlo test keeps its expectations of 10,000,000 live entries and last index 12,489,486. The existing working weighted-update path and the PRNG are not replaced.

No exception has been accepted for changed generated values in previously working seeded scenarios. The approved legacy-constructor repair is not permission to change other outputs or weaken an oracle.

Coverage is finite. The self-created `working` / `working-mathcing` files in older tests are not historical compatibility evidence on a fresh checkout. The fixed fixture is independent evidence; it does not certify every seed, generator configuration, locale, asynchronous interleaving, or byte-for-byte Office/CSV serialization. Document content and original size tolerances remain checked. Any subsequently discovered behavior difference requires explanation and separate owner acceptance.

## Bounded examples and CI

```sh
python3 verification/verify-examples.py 21
python3 verification/verify-examples.py 25
```

The verifier runs the existing entry points with embedded HSQLDB: 10 initial persons per country for generation/query/CUD, zero relations for that SQL fixture, a one-minute query phase, and six one-second CUD batches. The CSV fixture uses 10,000 persons per country and 20 relations. It checks successful query/CUD groups, exactly six CUD checkpoints, 20,000 exported person IDs, 20 relation IDs and 47 valid relationship links. Workloads are bounded; no external database/service is required. Generated SQL/CSV files are confined to the example's build directory. These commands recreate those demo outputs; do not use that directory for personal data.

`.github/workflows/ci.yml` declares Java 21/25 jobs for build/tests, local publication, independent dependency consumption and the bounded examples. Official checkout/setup-java actions are pinned to release commits. No release/upload job or secrets are configured. A configured matrix is not evidence that a remote CI run occurred; actual execution results belong in the corresponding verification records. This local preparation does not push a branch or trigger remote CI.

## Publication preparation

The intended coordinates remain `com.arboratum:fastbeangen:0.3.0` and `com.arboratum:fastbeangen-doc:0.3.0`. **0.3.0 is unreleased, not remotely available as a result of this change.** The GPL version 3 license is unchanged.

```sh
./gradlew --no-daemon publishAllPublicationsToLocalRepository
python3 verification/verify-publication.py
./gradlew --project-dir verification/consumer --no-daemon -PjavaVersion=21 run
./gradlew --project-dir verification/consumer --no-daemon -PjavaVersion=25 run
```

The build only configures a local Maven repository at `build/repository`. It produces core/document binaries, sources, Javadoc, POMs, Gradle module metadata and checksums. The publication verifier checks artifact contents, Java 21 classfile/variant targets, SHA-256 values, coordinates and license/developer/SCM metadata. The separate consumer forces the `com.arboratum` group to resolve locally, then compiles/runs against published dependencies and generates readable DOCX/XLSX content. It explicitly reads Maven POM metadata instead of relying on Gradle metadata redirection. It does not use a project dependency or silently fall back to a remote FastBeanGen release.

Ordinary checks require no signing credentials. If `SIGNING_KEY` is injected, Gradle uses that ASCII-armored private key in memory, with optional `SIGNING_PASSWORD`, to sign the publications. Do not commit keys or expose them in command history/logs. To explicitly validate unsigned local output in an environment that already has signing variables, use `env -u SIGNING_KEY -u SIGNING_PASSWORD ./gradlew publishAllPublicationsToLocalRepository` on Unix.

A later authorized operator can securely inject those variables and run `./gradlew bundleCentral` to create `build/distributions/fastbeangen-central.zip`. The bundle task refuses unsigned operation; it is not an uploader. Before any real release, verify signing-key ownership, namespace authorization for `com.arboratum`, Central Portal token/access and the signed bundle's actual acceptance. Those rights, real-key signing and portal acceptance are not verified by unsigned local checks.

Use the [Central Portal publishing documentation](https://central.sonatype.org/publish/publish-portal-api/) for the separately authorized upload. No remote publication repository, token, automatic upload, push, PR, merge or release publication is configured or performed here.
