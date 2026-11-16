# FastBeanGen modern Java build and compatibility contract

## Purpose and supported project

FastBeanGen is a deterministic pseudo-random data-generation library with its existing core module, executable examples, and document-generation module. The supported project consists of `fastbeangen`, `fastbeangen-example`, and `fastbeangen-doc`, built together with the repository's Gradle wrapper. This capability modernizes the technical platform without adding generator functions or replacing the existing project architecture.

## Java and Gradle support

The minimum supported consumer runtime is Java 21. Library compilation must target Java 21 bytecode rather than the JDK version incidentally selected on a developer's machine. The project must compile and run its applicable test suites on actual Java 21 and Java 25 runtimes. Java 27 or a configured but unexecuted CI matrix is not evidence that Java 25 tests passed.

A checked-in official compatible Gradle wrapper, pinned distribution, integrity configuration, and documented commands must support the build. Build/test checks must not depend on a global Gradle installation or require secrets. The existing Gradle multi-project structure remains the build entry point; no Maven conversion is introduced.

## Dependency and repository contract

Direct dependencies and build plugins must use stable pinned compatible versions. Transitive versions may be governed by a pinned supported BOM or other standard Gradle dependency constraints. Dynamic versions, snapshots, milestones, and release candidates are not acceptable substitutes for a supported stable upgrade.

Dependency and plugin resolution must not rely on JCenter or Bintray. Obsolete Gradle declarations and publication configuration must be replaced with supported equivalents. If a necessary dependency has no suitable supported upgrade, document the concrete limitation and do not claim the modernization is fully verified until its disposition is resolved.

Prefer the existing library/component when it remains suitable. Replacing a dependency or changing public APIs is permitted when it materially enables the confirmed upgrade, not as an invitation to redesign unrelated functions. API incompatibilities must be documented for consumers.

## Historical output compatibility and reproducibility

The existing pseudo-random algorithm and generated data contracts are preserved. For an identical seed and generator configuration, preserve historical generated values as far as possible. The new version must itself remain reproducible for identical seeds/configuration under its supported environment.

Before upgrading behavior-affecting dependencies, establish a traceable historical reference from baseline commit `7cfaf9bfb699f38f02d5cb87cfe81fe162348c47`, existing test expectations, and executable comparisons where possible. Record the original source/dependency combinations actually exercised and the representative seeds, boundaries, generators, distributions, and data-set operations covered. A failed or unavailable historical build does not provide passing evidence. Finite reference coverage does not justify a claim of exhaustive identity for all seeds.

Every detected observable behavior difference must identify the scenario, seed/configuration, old/new result, cause, and alternatives considered. Resolve the difference or obtain explicit owner acceptance for that specific exception before treating it as allowed. General permission to break API compatibility is not acceptance of output differences. Initially there are no accepted exceptions.

Do not remove scenarios, change golden values without an accepted explanation, widen size/statistical tolerances, or weaken assertions to make dependency upgrades pass. Reuse existing literal expectations instead of duplicating them. Focused new reference tests supplement the existing suite rather than replacing it. A reference file auto-created by the implementation under test, or a comparison of that implementation against itself, is not historical compatibility evidence; use a genuine pre-upgrade reference with recorded provenance.

## Existing modules and operational behavior

The core module continues to provide its existing primitive/value, string/regular-expression, date, bean, distribution, and data-set generation functions. Necessary dependency API adaptations must retain validation, error handling, selection/update semantics, and supported seeded behavior.

The example module retains its existing entry points and example functions after necessary framework and persistence API adaptations. Routine verification must use bounded inputs and must not launch unbounded bulk generation or require a separately provisioned external database/service.

The document module retains DOCX/XLSX generation, existing test scenarios, and their size/content oracles. Necessary XDocReport/Jxls/POI API changes do not authorize removal of document functionality or relaxation of its checks. Document-output differences discovered during verification follow the same explicit exception process as other observable changes.

## Publication-ready distribution

The build must prepare consumable core and document-library artifacts and Maven metadata for the intended `com.arboratum` coordinates, together with source and Javadoc artifacts and accurate project/license/SCM metadata. Preserve coordinates unless an external constraint requires a new owner decision.

A local publication validation path must allow inspection/consumption of generated artifacts without uploading to Maven Central and without real publication credentials. Document the real signing, namespace authorization, and upload prerequisites for later publication. Missing credentials or namespace rights must not be hidden or described as verified.

The target distribution channel is Maven Central. Publication-ready is not published: this change does not authorize remote artifact upload, Git push, PR creation, merge, or automatic release. Keep the existing GPL-3.0 license.

## CI and consumer documentation

CI must declare build/test jobs for Java 21 and Java 25. Record actual executed checks and their candidate identity separately from merely configured jobs. Missing runtimes, failed checks, timeouts, and unexecuted jobs remain visible and cannot count as passed.

README, changelog, and focused migration documentation must describe the supported Java versions, wrapper/check commands, relevant dependency/API changes, reference-test coverage and limitations, individually accepted behavior exceptions, and the publication prerequisites. Do not instruct users to depend on a new version as if it were already remotely available.

## Verification boundaries

Builder checks focus on reference capture and relevant changed paths; final registered Runtime checks verify the submitted candidate without an unnecessary duplicate full-suite run. Preserve a single writer/build process in the worktree. Independent verification assesses every acceptance item and records real failures/blockers.

The change excludes new functions, general architecture or random-algorithm replacement, a performance-tuning campaign, license changes, and remote delivery without a separate mandate.
