---
generated_from_state_version: 8
---

# Verification

## Current result

- Result: **Archived**
- Verification status: **Checks completed; result confirmed**
- Goal cycle: 1
- Iteration: 1
- Verifier attempt: 1
- Completed: 2026-11-16T13:19:47.018Z
- Summary: All five scoped acceptance items pass independent semantic review of this unchanged candidate. Reused seven bound Runtime check receipts and requested only the missing read-only diff evidence and post-Java25 publication/archive validation; both additional checks passed. Exact legal texts, LGPL-3.0-only metadata/docs, all eight existing JAR variants, retained nonlicensing checks, scope/history preservation and actual Java21/25 POM-only consumer execution are supported. No edits, direct tests, commits or remote publication were performed by this verifier. Known diagnostic failures, unrun heavy/remote checks, legal-attestation boundaries and bootstrap delivery exclusions remain explicitly disclosed.

## Acceptance

| ID | Result | Source | Criterion | Reason |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | The repository explicitly grants `LGPL-3.0-only` for the current first-party project. `LICENSE` is the canonical LGPL v3 text and `COPYING` exactly preserves the previous GPL v3 text; both are included rather than claiming LGPL is an unrelated standalone permissive license. | Independently read the complete brief, target Spec, LICENSE, COPYING and README grant. README and Gradle metadata explicitly select LGPL-3.0-only without later-version permission. Runtime publication-java21 and publication-java25 validate the LGPL SHA-256 against the confirmed official reference and both repository legal texts against pinned hashes; scope-invariants compares COPYING byte-for-byte with LICENSE at confirmed base 6416eddfe6b38499ba647cfbda1b584a8327e327. Both incorporated texts are supplied and LGPL is correctly described as weak copyleft. |
| A2 | passed | brief.md | Local core/document publication POMs consistently name and link the selected grant. All first-party JAR variants produced by existing build tasks, including binary, sources, Javadoc and executable examples where applicable, contain the two correct legal texts without duplicate first-party license entries. Existing coordinates and publication variants are retained. | Reviewed shared Jar-task packaging in build.gradle and independently read both actual generated publication POMs: the exact LGPL-3.0-only name and canonical GNU URL agree, and com.arboratum coordinates/version remain unchanged. Passed Runtime publication-java21 and added publication-java25 inspect the six core/document binary, sources and Javadoc JARs plus both example JARs, requiring exactly one META-INF/LICENSE and META-INF/COPYING with correct bytes. Existing checks also validate checksums, Java 21 classfile/variant targets, source/Javadoc contents and publication metadata. Reviewed Gradle diff retains existing publication variants and topology. |
| A3 | passed | brief.md | Current README, unreleased changelog and publication/migration guidance agree with the selected grant, describe the LGPL obligations without promising unrestricted proprietary redistribution, distinguish first-party and third-party material, and do not claim a Central release or independently verified legal ownership. The owner's rights attestation is retained in this brief. | Independently read README.md, CHANGELOG.md, docs/modernization.md and the owner attestation in the brief. Current licensing statements agree on LGPL-3.0-only, identify both legal texts, preserve third-party licensing and previous GPL rights, and describe notices, corresponding source, modification/reverse-engineering and replacement/relinking conditions without promising unrestricted proprietary redistribution. Guidance explicitly identifies 0.3.0 as unreleased and preserves signing/namespace/upload prerequisites; no independent legal-clearance or Central-release claim is made. |
| A4 | passed | brief.md | Wrapper/dependency notices, historical release/Comet evidence, and previously granted GPL rights are preserved. References to GPL as incorporated base terms or historical/dependency licensing remain valid; no blanket replacement removes them. No unrelated repository, private prototype material, or bootstrap file is added to the implementation change. | The passed scope-invariants receipt and independently reviewed Native diff show no Java/resources, wrapper, subproject dependencies/settings, CI, historical release entries, archived Comet evidence or published-Spec changes. Implementation additions are COPYING and the focused verification test; other implementation changes are confined to legal text, metadata/packaging, current documentation and the existing verifier. GPL references retained as incorporated terms/history are appropriate; earlier GPL rights are explicitly preserved. The .gitignore Comet block and local .comet/.pi resources are identified by the confirmed brief as separately authorized pre-existing bootstrap, not implementation deliverables; they must remain excluded from a later implementation commit. |
| A5 | passed | brief.md | Focused verifier self-checks demonstrate detection of wrong/missing license metadata or packaged texts. Local publication/archive checks and the existing independent consumer smoke run on actual Java 21 and 25, without remote publication. A reviewed diff confirms no changes to Java behavior, bundled data/templates, PRNG, behavioral fixtures/assertions, runtime versions or dependencies; existing publication checks remain effective. Unrun heavy suites are reported rather than claimed passed. | Reviewed focused self-tests and the verifier diff: all unrelated coordinate, required-metadata, file-presence, checksum, archive-integrity/content, classfile and Gradle-variant predicates remain effective; license checks are stronger. Runtime license-self-tests passes eight methods covering valid fixtures and rejection of missing/wrong grants, URLs, multiple grants and missing/altered/duplicate legal entries. Reused successful Java21/25 toolchain assembly/local-publication receipts and both independent POM-only consumer receipts; consumer source asserts actual runtime identity and historical PRNG plus DOCX/XLSX contents, with logs confirming Java 21 and 25. Added publication-java25 passed against the final Java25-built artifacts, complementing publication-java21. Java21 assembly reused valid up-to-date outputs rather than claiming fresh compilation. Reviewed diff and scope-invariants establish no behavioral/data/PRNG/fixture/runtime/dependency changes. Heavy suites are explicitly unrun, not passed; no remote publication occurred. |

## Checks

| Check | Command | Working directory | Status | Exit | Duration |
| --- | --- | --- | --- | ---: | ---: |
| Focused positive and negative license-verifier self-checks | -E -B -m unittest discover -s verification -p [REDACTED] -v | . | passed | 0 | 92 ms |
| Assemble and publish unsigned locally using Java21 compiler | -u [REDACTED] -u [REDACTED] JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew --offline --no-daemon --console=plain --max-workers=1 -PjavaVersion=21 assemble publishAllPublicationsToLocalRepository | . | passed | 0 | 7295 ms |
| Inspect Java21 local POMs, eight JARs, hashes and license entries | -E -B verification/verify-publication.py | . | passed | 0 | 173 ms |
| Published POM-only dependency consumer on actual Java21 | -u [REDACTED] -u [REDACTED] JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew --project-dir verification/consumer --offline --no-daemon --console=plain --max-workers=1 -PjavaVersion=21 run | . | passed | 0 | 5366 ms |
| Assemble and publish unsigned locally using Java25 compiler | -u [REDACTED] -u [REDACTED] JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew --offline --no-daemon --console=plain --max-workers=1 -PjavaVersion=25 assemble publishAllPublicationsToLocalRepository | . | passed | 0 | 11306 ms |
| Published POM-only dependency consumer on actual Java25 | -u [REDACTED] -u [REDACTED] JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew --project-dir verification/consumer --offline --no-daemon --console=plain --max-workers=1 -PjavaVersion=25 run | . | passed | 0 | 6236 ms |
| Check preserved source/dependency/history scope and original GPL bytes | -E -B -c from pathlib import Path; import subprocess; changed=subprocess.check_output(['git','diff','--name-only','HEAD'],text=True).splitlines(); allowed={'.gitignore','LICENSE','README.md','CHANGELOG.md','docs/modernization.md','build.gradle','verification/verify-publication.py'}; assert set(changed)<=allowed,changed; new=subprocess.check_output(['git','ls-files','--others','--exclude-standard'],text=True).splitlines(); implementation=[p for p in new if not p.startswith(('.pi/','.comet/','docs/comet/changes/relicense-lgpl-3/'))]; assert set(implementation)=={'COPYING','verification/test_publication_licenses.py'},implementation; assert Path('COPYING').read_bytes()==subprocess.check_output(['git','show','6416eddfe6b38499ba647cfbda1b584a8327e327:LICENSE']); print('Preserved all Java, data/templates, behavioral fixtures/oracles, wrapper, subproject dependency/settings files, CI, archives and published Spec; exact previous GPL retained; no private/foreign implementation files.') | . | passed | 0 | 120 ms |
| Expose focused read-only diff for independent scope and preserved-check review | --no-pager diff --no-ext-diff --no-color HEAD -- .gitignore build.gradle README.md CHANGELOG.md docs/modernization.md verification/verify-publication.py | . | passed | 0 | 27 ms |
| Inspect actual Java25-built local POMs and all existing JAR variants | -E -B verification/verify-publication.py | . | passed | 0 | 85 ms |

### Builder-reported evidence

These are Builder reports, not Runtime check receipts or independent verification results.

- Focused verifier RED: failed — Expected RED before strengthening old checks: 7 test methods, 9 failed negative subcases, no errors; administrative log working/fastbeangen-relicense-lgpl-3/red-tests.log.
- Focused verifier final GREEN: passed — 8 tests, exit0, final-unit-tests.log. Source-hash and legal metadata/archive rejection checks.
- Java21 development local build/publication: passed — Temurin21.0.11 host and -PjavaVersion=21, offline assemble publishAllPublicationsToLocalRepository: exit0. No behavioral suites or remote upload.
- Java21 development publication contents: passed — verify-publication.py exit0: six core/doc variants plus both example JARs, POMs, checksums, JVM21 classfile/variant targets and both exact legal texts.
- Scope and original-check review: passed — No tracked Java/resources/wrapper/subproject dependencies/CI/archives/project-Spec changes; only two new implementation files; 11 original assertion predicates retained, two strengthened/equivalently refactored.
- Git whitespace diagnostic: failed — git diff --check HEAD exit2, 43 indent-with-spaces diagnostics from existing global core.whitespace versus canonical GNU text/project space indentation. No check or Git configuration weakened; not presented as passed.
- Full final matrix/heavy suites/remote checks: not-run — Java25 and Java21/25 independent consumers delegated below to Runtime. Heavy behavioral suites, SQL/CUD examples, remote CI, real signing and Central upload are not rerun or claimed.
- Known limitation: Owner rights/consents are an attestation; no independent legal, provenance or full dependency-license/vulnerability audit.
- Known limitation: git diff --check HEAD remains exit2 with 43 indent-with-spaces diagnostics. Canonical GNU text and existing project indentation are retained; no Git setting/control was weakened. This diagnostic is not reported as passed.
- Known limitation: No heavy behavioral suite, large statistical/file tests, SQL/CUD example run, remote CI, real signing or Central upload is claimed. Java/data/dependencies/oracles are unchanged; focused artifact and consumer checks are the confirmed plan.
- Known limitation: CodeGraph preparation failed ownership routing and context-mode failed storage I/O; no retry/repair/borrowed index or global configuration change. Targeted current-source reads and explicit diff review were used.
- Known limitation: Existing Native bootstrap .gitignore/.comet/.pi remains local and preserved; it is not intended for an implementation commit. No commit, Archive, push, PR, merge, publication or cleanup is authorized/performed.

## Blockers

_None._

## Risks and skipped work

- Relicensing relies on the owner's recorded rights/consents attestation. This review is not an independent chain-of-title, legal-compliance, full dependency-license or vulnerability audit.
- Heavy behavioral/statistical/file suites, SQL/CUD example execution, remote CI, real-key signing and Central upload were not rerun or verified for this licensing-only change. Focused local artifact checks and actual Java21/25 consumer smoke are the acceptance evidence.
- The Builder reports git diff --check HEAD exit 2 with 43 indent-with-spaces diagnostics. This remains a failed diagnostic, not passing Runtime evidence; this verifier did not rerun it or alter Git/check configuration.
- Separately authorized local Native bootstrap resources (.gitignore Comet block, .comet and .pi) remain present and are not implementation deliverables. Any later delivery must exclude them unless separately authorized; no commit, Archive, push, PR, merge or cleanup was performed by this verifier.
- Builder-reported CodeGraph preparation/context failures were not repaired or treated as evidence. Independent review used current-source reads and the Native-executed read-only diff instead.
- Build/consumer logs contain existing invalid-autodetected-JDK, compilation/Javadoc and missing Log4j-provider warnings. The required commands and actual Java21/25 consumers nevertheless succeeded; this is not a warning-free-build claim.
- Coordination is skill-coordinated. The semantic review was performed in this verifier conversation, but host-attested independence is not established by Native.

## Previous iterations

| Goal cycle | Iteration | Attempt | Outcome | Unresolved | Summary | Completed |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 1 | pass | — | All five scoped acceptance items pass independent semantic review of this unchanged candidate. Reused seven bound Runtime check receipts and requested only the missing read-only diff evidence and post-Java25 publication/archive validation; both additional checks passed. Exact legal texts, LGPL-3.0-only metadata/docs, all eight existing JAR variants, retained nonlicensing checks, scope/history preservation and actual Java21/25 POM-only consumer execution are supported. No edits, direct tests, commits or remote publication were performed by this verifier. Known diagnostic failures, unrun heavy/remote checks, legal-attestation boundaries and bootstrap delivery exclusions remain explicitly disclosed. | 2026-11-16T13:19:47.018Z |



## Conclusion

All five scoped acceptance items pass independent semantic review of this unchanged candidate. Reused seven bound Runtime check receipts and requested only the missing read-only diff evidence and post-Java25 publication/archive validation; both additional checks passed. Exact legal texts, LGPL-3.0-only metadata/docs, all eight existing JAR variants, retained nonlicensing checks, scope/history preservation and actual Java21/25 POM-only consumer execution are supported. No edits, direct tests, commits or remote publication were performed by this verifier. Known diagnostic failures, unrun heavy/remote checks, legal-attestation boundaries and bootstrap delivery exclusions remain explicitly disclosed.
