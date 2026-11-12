# Outcome

Make FastBeanGen usable as a normal Maven dependency in a 2026–2027 Java project by modernizing its build, dependency versions, and publication preparation. The minimum supported Java version is 21; compilation and tests must be verified on actual Java 21 and Java 25 runtimes.

The owner approved this outcome, all three modules, useful API incompatibilities, and the bounded scope during the Q1–Q9 interview. This change records those decisions; it does not reopen them. Final Native Shape confirmation binds the formal requirements and acceptance items below before implementation.

# Scope

Update the existing Gradle multi-project build and the `fastbeangen`, `fastbeangen-example`, and `fastbeangen-doc` modules. Preserve their existing functions and keep adaptations proportionate to the dependency/toolchain upgrades. Replace JCenter/Bintray-era build and publication assumptions with maintained, pinned stable versions and a Maven Central-ready publication path. Preserve the existing `com.arboratum` coordinates unless a concrete external constraint requires a new owner decision.

The GitHub URL identifies the repository to modernize and is an implementation reference, not a document specifying new functionality. Requirements come from the owner's interview and explicit scope confirmation. Repository sources, tests, and existing artifacts establish current behavior; they are not instructions to expand the scope.

## Directory structure

### Created

- `.comet/config.yaml` — official Native project configuration, in English, with explicit Archive confirmation required; authorized bootstrap already performed.
- `.pi/skills/comet/` and `.pi/skills/comet-native/` — unmodified official Comet 0.4.5 skills and resources; no neighboring repository's business rules.
- `.github/workflows/ci.yml` — automated build/test matrix for Java 21 and 25, without automatic publication.
- `fastbeangen/src/test/java/com/arboratum/beangen/` — focused historical-output reference tests/fixtures where existing assertions do not adequately capture the upgrade risks.
- `docs/comet/changes/modernize-java-21/specs/modern-java-build/spec.md` — complete target specification for the supported build and compatibility contract.
- `docs/modernization.md` — migration, pinned-version rationale, verification commands, publication prerequisites, and any owner-accepted behavior exceptions.

### Modified

- `build.gradle` and `settings.gradle` — modern Gradle configuration, Java 21 target, dependency/repository management, verification, and local publication preparation.
- `gradlew`, `gradlew.bat`, and `gradle/wrapper/` — official compatible wrapper and distribution integrity configuration.
- `fastbeangen/build.gradle`, `fastbeangen-example/build.gradle`, and `fastbeangen-doc/build.gradle` — modern dependency declarations and module-specific build compatibility.
- `fastbeangen/src/main/` and `fastbeangen/src/test/` — only necessary dependency/API adaptations and regression coverage; preserve generator contracts and test oracles.
- `fastbeangen-example/src/` — necessary framework/API adaptations, preserving the existing example functions and entry points.
- `fastbeangen-doc/src/` — necessary document-library adaptations and tests, preserving DOCX/XLSX behavior and existing size checks.
- `README.md` and `CHANGELOG.md` — accurate Java/build/consumption instructions and migration information; no claim that an unpublished version is already available.
- `.gitignore` — official project bootstrap entries and ordinary build-generated output only; no exclusions used to conceal failing checks or source changes.

### Deleted

None planned. Obsolete build/publication declarations are removed in the files above; generators, examples, assertions, and test scenarios are not removed to make the upgrade pass.

### Not created

- No replacement application architecture, Maven conversion, or generalized dependency-management framework: the existing Gradle/module structure remains suitable.
- No new generator features, benchmark platform, Sonar service integration, external Comet dependency, or automatic release workflow.
- No private credentials, global Pi configuration, neighboring-repository rules, or second independently created worktree.

# Non-goals

- New functionality, a general rewrite, or systematic API redesign unrelated to the version upgrades.
- A systematic Joda-Time-to-java.time migration or replacement of dependencies that remain suitable solely for stylistic reasons.
- Performance tuning, a benchmark campaign, a license change, or support for Java versions below 21.
- Remote publication, pushes, pull requests, merges, automatic Archive, or workspace cleanup without the corresponding distinct authorization.
- Changing expected seeded results, weakening assertions/tolerances, skipping existing scenarios, or accepting behavior differences without the owner's explicit decision for each exception.

# Constraints and invariants

- Preserve the original pseudo-random algorithm and historical seeded outputs as far as possible. Necessary API incompatibilities do not authorize a change to generated values.
- Establish historical references before upgrading behavior-affecting dependencies, using the baseline commit `7cfaf9bfb699f38f02d5cb87cfe81fe162348c47`, its existing assertions, and an executable comparison where feasible. Record exactly which original source/dependency combinations were executed. A blocked historical build is not a successful comparison.
- For every detected behavior difference, record the affected scenario, seed/configuration, old/new result, cause, and candidate alternatives. Keep it unresolved until the owner explicitly accepts that specific exception. Do not silently update a golden file or widen a size/statistical tolerance.
- Reproducibility within the new version is mandatory. Do not claim exhaustive identity across all seeds from a finite reference sample; state coverage and limitations.
- Use stable, pinned versions compatible with Java 21; no dynamic, snapshot, milestone, or release-candidate versions. If no suitable supported upgrade exists, expose the limitation rather than presenting it as resolved.
- Preserve the existing GPL-3.0 license and the functions of all three modules.
- Work only in Native's `comet/modernize-java-21` worktree. Preserve the original checkout and its uncommitted bootstrap files. Keep one writer/build process in the worktree.
- Use the repository's Gradle wrapper for checks. CodeGraph has no independent worktree index and cannot initialize one during Shape; prepare it only in authorized active Build. Do not query a parent index as if it represented this worktree.

# Decisions

- Q1/Q3/Q9: a bounded, incremental technical modernization, without new features or a general rewrite; scope explicitly approved.
- Q2: intended consumption in a 2026–2027 project, not a separate public relaunch/product-development effort.
- Q4: Java 21 minimum, verified on Java 21 and Java 25.
- Q5: preserving the old public API is not mandatory; useful upgrade-driven breaks must be documented.
- Q6: Maven Central is the intended distribution channel; the current mandate prepares publication but does not publish.
- Q7: all three existing modules are in scope.
- Q8: avoid historical output differences as far as possible; only individually explained exceptions explicitly accepted by the owner are allowed. There are currently no accepted exceptions.
- Workspace choice: the owner selected a Native-managed dedicated worktree. Native created `comet/modernize-java-21`, targeting `master`.
- One Native change is appropriate because the module dependency graph, shared Gradle build, seeded compatibility checks, and publication metadata are tightly coupled. Supervisor decomposition would add coordination without an independent delivery boundary.

# Verification expectations

- Inventory and preserve the existing test cases and oracles. Existing RandomSequence, bean, string, and distribution tests already contain literal expected values; reuse them rather than duplicating their assertions. Add a small focused set of deterministic references only for inadequately covered upgrade-sensitive paths.
- `GeneratorTest` and parts of `CharSequenceGeneratorBuilderTest` create a reference file if absent. Such a self-generated reference or a same-implementation repeat is not evidence of old/new compatibility: capture a genuine pre-upgrade reference first and preserve its provenance. Current DOCX/XLSX tests mainly check existence/size, so add bounded content checks where needed without weakening those size oracles.
- Verify original/reference behavior before changing relevant dependencies; run focused Builder checks during implementation, then let Runtime execute the final registered checks for the submitted candidate without an unnecessary duplicate full-suite run.
- Verify all module compilation/tests and publication artifact generation. Use bounded examples/smoke checks; do not launch unbounded bulk generation or require an external database/service for routine verification.
- Java 21 is available locally. Java 25 was not present in the inspected JDK inventory; obtain an appropriate task-scoped/toolchain runtime or actual matching-candidate CI evidence before claiming its checks passed. Java 27 is not a substitute. Missing runtime/network/credentials remain explicit blockers or unexecuted checks, never successes.
- Publication validation must be local/offline with respect to upload: inspect generated binaries, source/Javadoc artifacts, POM metadata and coordinates, and the documented signing/upload prerequisites. Do not upload artifacts or require real credentials for ordinary build/test checks.

# Acceptance examples

- All three modules compile and their applicable existing and added tests pass on actual Java 21 and actual Java 25; produced library bytecode targets Java 21, and the Gradle wrapper/check commands are reproducible and documented.
- The build uses pinned stable compatible dependencies and supported Gradle declarations, resolves from supported repositories without JCenter/Bintray, and does not introduce dynamic/pre-release versions or conceal unresolved dependency limitations.
- Upgrade-sensitive seeded generator/data-set behavior matches recorded historical references; the pseudo-random algorithm is preserved, new-version reproducibility is tested, and every detected difference is either resolved or individually documented with explicit owner acceptance. No test oracle, scenario, size tolerance, or statistical check is weakened to obtain a pass.
- Existing example entry points and DOCX/XLSX generation remain usable after necessary API adaptations; module tests and bounded smoke checks cover the relevant upgrade surfaces without removing existing functions or requiring unbounded generation/external services.
- Local publication preparation produces consumable library/document-module artifacts and valid Maven metadata with the intended coordinates, sources/Javadoc and signing/upload instructions suitable for Maven Central; no remote artifact upload is performed and ordinary checks require no secrets.
- CI declares Java 21 and Java 25 build/test jobs; README, changelog, and migration documentation accurately describe supported Java versions, commands, useful API changes, historical-reference coverage and accepted exceptions, and the distinction between publication-ready and actually published.
