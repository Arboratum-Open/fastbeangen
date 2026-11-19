# Published 0.2.17 reference

This standalone harness compiles the same fixed `PublishedApiCompatibilityTest` assertions against the **official, unmodified Maven Central 0.2.17 binary**, not the candidate's sources. It uses actual Java 21 and the published runtime dependency graph; JUnit 4.13.2 is a test-harness dependency only. The published POM exposes Reactor with runtime scope, so the harness also uses that runtime classpath for compilation. This is not a claim that the old POM is a correct compile-time consumer contract.

Run from the repository root, with Java 21 available:

```sh
./gradlew --project-dir verification/published-compatibility --no-daemon --console=plain --max-workers=1 test
```

**Expected reference result: nonzero, eight tests, five passes and three known failures.** Do not call this an all-green reference or enable `ignoreFailures`. Inspect the XML report under `build/test-results/test` in this harness:

| Test | Published 0.2.17 | Required 0.3.0 behavior |
| --- | --- | --- |
| `finiteBudgetsPropagateThroughComposedAndUnionViews` | Null lock on a composed transformed selection | Propagate the explicit zero budget and return null |
| `transformedSelectionRetainsEntryAndReferenceProvenance` | Null lock on successful transformed selection | Select successfully and preserve entry/reference provenance |
| `initialProvenanceIncludesDeletedSlots` | Marks an initial index after a deleted slot as non-initial | Initial index boundary includes deleted slots, not just live count |

The five other tests independently establish published bounded-selection success/null behavior, basic initial-versus-created provenance, entry-aware deletion and reference-aware accepted/rejected updates. All eight assertions must pass against the candidate in the normal core test suite. Fixing the three defects is required by the confirmed bounded-view/provenance contract, not evidence that the old implementation passed those cases. The constructor reflection in one fixture only accommodates the old extra live-count parameter; both versions receive the same explicit version slots and fixed expectations.

`reference.json` pins the official core/document binary, sources and POM SHA-256 values and records the reference execution boundary. The harness verifies the core binary hash before executing tests and prints the resolved dependency coordinates/hashes for the execution log. These original dependencies belong only to this reference project, not the product runtime.

During release preparation, `javap -public -s` inspected 358 original public JVM method/field descriptors across 59 public classes in the core/document JARs. No descriptor was missing from the candidate after accounting for the `DataSet` methods inherited from `DataView`; the candidate additionally retains the pre-release one-argument update hook. This is a finite API-descriptor check, not proof of exhaustive source, linkage or generated-output compatibility across every dependency, seed and configuration. The older Git baseline `7cfaf9bfb699f38f02d5cb87cfe81fe162348c47` remains a separate historical-output reference.
