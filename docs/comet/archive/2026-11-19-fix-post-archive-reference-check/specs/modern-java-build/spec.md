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

## Requirement: project-licensing First-party licensing and redistribution

This requirement is the authoritative current licensing contract. It explicitly supersedes the preceding historical instruction "Keep the existing GPL-3.0 license." in Publication-ready distribution and the historical exclusion of license changes in Verification boundaries. All other preceding Java, build, compatibility, module, publication and verification requirements remain effective. The archived modernization evidence is not rewritten or reopened.

The current first-party project is distributed under GNU Lesser General Public License version 3.0 only (SPDX: LGPL-3.0-only), covering its sources, examples, documentation and bundled dictionaries/document templates, subject to third-party rights. All project declarations and POM license names/links must consistently identify this grant, using the canonical GNU LGPL v3 link and an unambiguous version-3.0-only name/SPDX identifier. Do not silently add an or-later permission, dual license, or claim that LGPL is a permissive license.

The repository supplies the unmodified official LGPL v3 text as LICENSE and retains the previous GPL v3 text byte-for-byte as COPYING, since LGPL v3 incorporates GPL v3. Existing first-party binary, sources, Javadoc and executable-example JAR variants include both texts, without duplicate first-party license entries or replacement of dependencies' own notices. Preserve existing coordinates, the unreleased version, and publication variants.

The owner's confirmed rights/consents are the basis for this first-party relicensing, not evidence of an independent legal/provenance audit. Dependencies, the Gradle wrapper and other identified third-party material retain their licenses and notices. Preserve historical releases, granted GPL rights and archived Comet evidence. Describe LGPL as weak copyleft: integration/redistribution with applications under other terms remains subject to LGPL requirements, including applicable notices, source and modification/relinking conditions; do not promise unrestricted proprietary redistribution.

Current README, changelog and publication guidance must agree with the selected grant and identify the incorporated GPL text, third-party boundary and unreleased/publication status. Publication verification must inspect actual POMs and packaged legal texts and detect incorrect/missing metadata or texts, while preserving all unrelated checks. Changes limited to this licensing contract must not modify Java behavior, bundled data/templates, PRNG, behavioral fixtures/assertions, supported runtimes or dependencies. Verify local publication/archive contents and the independent consumer on actual Java 21 and Java 25; do not claim unrun heavy suites passed.

Relicensing does not authorize Git commits, publication, workspace cleanup or rewriting historical releases. Do not add local bootstrap files, private prototype material or unrelated work to the implementation change.

## Requirement: published-0217-compatibility Published 0.2.17 API and behavior restoration

The 0.3.0 distribution must restore the missing published 0.2.17 core database API, rather than treating the older Git modernization baseline as the latest published API. Compare the official core/document binaries, sources and POMs with pinned URL/version/hash provenance. Preserve all unrelated existing Java, module, dependency, licensing and historical-output requirements. Restoring already published functions is an explicit, limited exception to the preceding historical exclusion of new functions; it does not authorize unrelated generator features or architecture changes.

Restore bounded filtered/transformed-view overloads with maxTryPerSelect, including their applicable union/view propagation. Retain the existing convenience overloads with the published default budget. Exhausted and non-positive budgets and empty input return null as published; a selection attempt must not become an unbounded retry under an explicitly finite budget. Restore Entry.wasInInitialDataSet() and consistent initial-entry provenance through entries/references and applicable transformed views. Subsequent creation does not make an entry part of the initial dataset.

Restore DataSetBuilder.withDeletionCheck(Predicate<Entry<T>>) and pass the actual entry to deletion checks, and restore UpdateGenerator.canApplyOn(value, EntryRef) with the matching reference during update applicability checks. Retain the current one-argument update hook where compatible. The published entry-aware deletion signature takes precedence over the incompatible unpublished value-predicate variant with the same Java erasure; document that migration rather than add an unsafe generic workaround. Adapt all affected implementations and callers while preserving scenarios and assertions.

Reference-backed tests and an independent local-publication consumer must exercise the restored API, successful/rejected applicability, bounded-selection exhaustion, initial versus subsequently created entries and relevant view propagation. Preserve existing baseline seeded fixtures, PRNG draw order, acknowledgement/backpressure/cancellation behavior, data/templates and test tolerances. Do not blindly import unrelated old implementation defects. A discovered conflict between published behavior and protected outputs requires an explicit scenario/seed/result analysis and specific owner decision; neither source similarity nor a blocked baseline execution proves behavioral compatibility. Finite comparisons do not prove identity for every seed.

## Requirement: release-030-preparation GitHub and Maven Central preparation without publication

Prepare version 0.3.0 for the existing com.arboratum:fastbeangen and com.arboratum:fastbeangen-doc coordinates, keeping the three-module Gradle structure, current dependencies, Java 21 minimum/bytecode and actual Java 21/25 checks. The executable examples are not a new Maven publication. Reuse local publication, its strict artifact/license checks and the independent POM-only consumer without remote FastBeanGen fallback. Both legal texts and first-party/third-party/historical license boundaries remain unchanged.

Generate an inspectable local GitHub asset set with the two libraries' binary, sources and Javadoc JARs, POMs, release notes and a verified SHA-256 manifest. Identify source and artifact provenance honestly: a modified worktree is not an exact release commit. Keep generated outputs under build, allow only intended release inputs, and reject missing, altered or extraneous/private assets. Bootstrap, IDE, administrative prototype, credentials and unrelated repository material must not be packaged or committed.

Validate the existing Central bundle's intended versioned Maven layout and required MD5/SHA-1 checksums; SHA-256 release hashes do not replace Central's required checksums. The signed-bundle path must reject missing or unusable signing material rather than produce a misleading signed/ready result. Test normal unsigned publication/staging and negative artifact/hash/layout/license/signing-input cases without creating a real key. When no real key/authorization is supplied, report unsigned preparation only: real signing and cryptographic verification, public-key availability, namespace rights and Portal validation/acceptance remain unverified external prerequisites. No synthetic signature or local layout check is evidence of Central acceptance.

English release notes, README/changelog and focused publication guidance must accurately cover the published-0.2.17 migration/restored API, Java requirements, LGPL-3.0-only weak-copyleft obligations and third-party/historical boundaries, exact checks performed and their limits, and subsequent GitHub/tag/Central publication steps. Keep the version prepared/unreleased until publication actually occurs. This preparation authorizes no tag or GitHub draft/release creation, upload, Git commit/push, PR, merge, workspace cleanup or alteration/reopening of archived changes. Archive, result acceptance and delivery remain separate human gates.

## Requirement: bounded-cud-example-cadence Existing CUD limiter activation

The existing CUD example must consume its already supplied RateLimiter before processing each emitted operation. Preserve its configured rate of 20 operations/second and existing warm-up rather than introducing a new rate option or changing library feed pacing. This owner-selected correction is a narrow exception to the release brief's caller-adaptation-only boundary. It addresses unpaced growing batches also characterized with the seven exact base-commit database classes on the same Mac/Java 21 and unchanged remaining classes/resources/dependencies; it does not assert a passing historical build or establish that the restored API caused the slowdown.

Add a small regression that fails for the unpaced implementation and passes for the minimal correction, following the existing JUnit and test-dependency conventions without requiring an external database. Keep the existing bounded example-verifier timeout of 180 seconds, six CUD checkpoints, one-minute query, CSV workload of 20,000 persons, 20 relations and 47 valid links, and all existing assertions. The applicable checks must pass on actual Java 21 and Java 25; increasing timeouts, reducing scenarios or suppressing failures is not a correction.

The CUD example's per-wall-clock operation counts and batch timing intentionally become limited. Document that change without promising identical asynchronous interleavings. Preserve library PRNG draw order, deterministic operation-feed scenarios/oracles and acknowledgement/backpressure/cancellation implementation; this is not acceptance of changed seeded generator values or permission to throttle the library globally. Preserve all unrelated release-preparation, dependency, licensing, publication and human-gate constraints.

## Requirement: java-gradle-verification Java verification driven by Gradle

Implement the publication, license, release/staging, Central input/bundle and bounded-example verification in Java, with genuine JUnit tests driven by Gradle. Use dedicated verification sources/tasks in the existing build, not a fourth application module or publication. Gradle must be the shared local, CI and Runtime entry point, using actual selected Java 21/25 toolchains and existing dependency versions. Keep the independent consumer and published-reference builds isolated and Gradle-driven. Invoking Python from Gradle or testing only a Java launcher around Python does not meet this requirement. Reuse existing build facilities without an unnecessary dependency or general release framework.

Preserve the complete case/assertion inventory of the current 24 Python publication/license/release test methods, including all subcases, and explicitly map it to the Java tests. Preserve valid unsigned staging and repeatability; rejection of missing/altered artifacts and required checksums, incorrect or duplicate legal texts/grants, foreign/private inputs, symlink files/namespaces/ZIP entries, altered/foreign staging, hardlinked staging without changing external backups, and source-identity mismatch. Preserve per-artifact signature requirements, malformed-armor rejection and structural CRLF acceptance, and Central ZIP allowlisting, uniqueness, exact bytes, traversal rejection and empty-directory-payload enforcement. Keep all existing publication bytecode/metadata checks and example workloads, checkpoints, timeout and assertions. Synthetic signature fixtures remain unit-test-only structural evidence, never proof of cryptographic validity or release readiness.

Remove the five superseded Python verification implementations/tests and active Python verification commands from build, CI, current documentation and the new Runtime check plan after equivalent Java paths exist. Keep historical archives and external administrative evidence unchanged. Record genuine Gradle/JUnit reports and actual Java 21/25 outcomes for the migrated checks; old Python results are historical evidence, not validation of the replacement. Preserve all unrelated API, seeded-output, licensing, release-boundary and human-confirmation requirements.

