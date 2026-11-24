# Java verification driven by Gradle

Use the repository wrapper with `--no-daemon --console=plain --max-workers=1 -PjavaVersion=21` (or `25`). All active verification code/tests are Java; Python is not required. The root build has dedicated `verification` / `verificationTest` source sets, not another application module or publication. Build-only JSON/Ant ZIP facilities come from the SHA-pinned Gradle distribution; JUnit 4.13.2 and OpenCSV 5.12.0 reuse versions already used by the project. These dependencies do not enter either library's POM or JAR.

| Gradle task | Purpose |
| --- | --- |
| `verificationTest` | JUnit guard tests with temporary structural fixtures; also included in root `check`. |
| `verifyPublication` | JUnit inspection of actual locally published libraries and assembled example JARs; publication/assembly dependencies are explicit. |
| `verifyReference` | On Java 21: verify six official 0.2.17 pins, compare public descriptors, protect baseline inputs and characterize the original runtime's exact failures. |
| `verifySigningGuards` | JUnit launches the actual bundle task with absent, blank and deliberately unusable non-key fixture input; each must exit 1 for the intended signing reason and leave any existing ZIP unchanged. |
| `verifyExamples` | JUnit launches the existing example entry points using the Gradle-selected JVM/classpath and retains the shared 180-second execution deadline and every output/CSV assertion. |
| `stageRelease`, `verifyRelease` | Java local asset staging/current-input/identity verification. No GitHub operation or upload. |
| `bundleCentral`, `verifyCentralBundle` | Existing signed ZIP preparation, followed by Java structure/exact-content validation. Not an uploader or cryptographic verifier. |

JUnit XML/HTML are under `build/test-results/<task>/` and `build/reports/tests/<task>/`. Example, reference and signing child output is under `build/verification/`. Child processes are synchronous, have bounded waits, and are forcibly terminated and awaited on timeout. Signing checks use a separate generated project cache to avoid locking the parent Gradle build; no sources or global settings are changed. Only one such build/check sequence may run in this worktree at a time.

`verifyReference` does not use `ignoreFailures`: the original independent Gradle build really exits 1. Its fresh XML must report exactly 8 tests, 3 failures, 0 errors and 0 skipped, with the three named failure types. Five passes/three characterized defects are not a passing reference build. The finite public-descriptor comparison resolves the documented `DataSet` inheritance from `DataView`; it is not an exhaustive source/linkage/runtime certification. Protected-input inspection uses base commit `4a267527c030a1016c113714fc334c2c4844cbdd`; the seven database classes and explicitly authorized CUD caller are excluded, not historical tests/resources/PRNG/licensing/build inputs/archives.

The protected boundary includes 223 baseline paths. After release-preparation Archive, 222 remain fully byte-identical. The sole published `docs/comet/specs/modern-java-build/spec.md` may be either the exact original 10,245 bytes or the exact already accepted 19,515-byte target (SHA-256 `1669cc4cccff23336e6a8a1ce8c4f4190b5cd3beeed1a2c028f3fae6c12ed195`), whose first 10,245 bytes must still equal the historical file. This fixed target comes from the accepted release-preparation archive, not from the candidate under test. Arbitrary appendices, changed historical/added content and missing/truncated input are rejected. `ProtectedInputTest` exercises both accepted states and rejection paths on Java 21/25. A different future Spec publication needs an explicitly reviewed target update; the test does not discover or approve new content automatically. Previous archives remain immutable.

The independent [consumer](consumer/) remains POM-only with no remote FastBeanGen fallback. The [published reference](published-compatibility/README.md) retains its original dependency graph and Java 21 boundary.

## Guard-case migration map

The 24 former methods retain **identical method names** in the Java classes below. Every row includes all its original subcases and assertions, not merely a method-count check. The Java ZIP fixtures deliberately contain duplicate entries and Unix link modes where required; Python's fixture-writer `UserWarning` is not a verifier oracle and is replaced by the equivalent real duplicate-entry fixture and rejection assertion.

`test_publication_licenses.py` → `src/test/java/com/arboratum/beangen/verification/PublicationLicenseTest.java`:

| Identical method name | Preserved cases |
| --- | --- |
| `test_valid_pom` | Exact LGPL-3.0-only name and URL accepted. |
| `test_rejects_missing_pom_license` | No POM license rejected. |
| `test_rejects_wrong_pom_grant` | Both GPL-only and LGPL-or-later rejected. |
| `test_rejects_wrong_pom_url` | GPL URL rejected. |
| `test_rejects_multiple_pom_licenses` | Two first-party license entries rejected. |
| `test_valid_archive` | Both canonical packaged texts accepted. |
| `test_rejects_missing_or_incorrect_legal_texts` | Missing LGPL, missing GPL, GPL replacing LGPL, altered GPL rejected. |
| `test_rejects_duplicate_legal_text` | Actual duplicate META-INF/LICENSE rejected. |

`test_release.py` → `src/test/java/com/arboratum/beangen/verification/ReleaseTest.java`:

| Identical method name | Preserved cases |
| --- | --- |
| `test_unsigned_stage_and_manifest_verify_and_restage` | Byte-stable restaging, unverified status, exactly 11 files, no signatures. |
| `test_missing_and_modified_required_checksums_fail` | Missing and altered MD5, SHA-1 and SHA-256, restoring each fixture between subcases. |
| `test_missing_artifact_fails` | Required artifact absent, no output created. |
| `test_invalid_license_fails` | Invalid grant with recomputed valid checksums rejected. |
| `test_foreign_file_and_module_fail` | Private .env and unrelated namespace module rejected. |
| `test_symlink_input_fails` | Linked artifact rejected. |
| `test_central_requires_signature_for_every_artifact` | Unsigned rejected; complete structural fixtures accepted; malformed armor rejected. |
| `test_changed_staged_file_fails_and_is_not_overwritten` | Both verification and restaging reject changed notes, preserving bytes. |
| `test_foreign_staged_file_fails_and_is_preserved` | Foreign staged file rejected and retained. |
| `test_central_archive_rejects_foreign_duplicate_or_changed_entries` | Valid exact ZIP accepted; traversal, foreign file, actual duplicate and changed payloads rejected. |
| `test_crlf_signature_armor_is_structurally_accepted` | CRLF armor accepted structurally with matching checksums. |
| `test_symlink_namespace_fails` | Linked namespace rejected. |
| `test_central_archive_symlink_entry_fails` | Actual Unix symlink ZIP entry rejected. |
| `test_hardlinked_staging_is_rejected_without_changing_backup` | Hardlinked staged notes rejected before restaging, external backup unchanged. |
| `test_central_directory_payload_fails` | Allowed directory name with foreign bytes rejected. |
| `test_identity_change_is_not_silently_accepted` | Source-identity mismatch rejected. |

Additional Java tests cover tracked deletion identity, quoted/multiline CSV and output/CSV rejection paths, owned parent/child process termination on timeout, and the actual Gradle-selected JVM. Deletions of tracked verification sources have explicit fingerprint markers; staging does not fail merely because a legitimately retired tracked file is absent, nor silently omit that deletion. These added checks do not replace any of the 24 migrated methods. Existing artifact checks retain ZIP CRC/content checks, legal hashes/unicity, classfile/variant Java 21 targets, coordinates/metadata, source/Javadoc presence and checksums.

Signatures/class headers in guard fixtures are **structural test data only**, created exclusively in JUnit temporary directories. They are never used as real local publication inputs, real signatures or upload assets. Actual key ownership/cryptographic validity/public-key availability, namespace rights and Portal validation/acceptance remain separate unverified prerequisites. Historical archives and external administrative evidence are not rewritten by this migration.
