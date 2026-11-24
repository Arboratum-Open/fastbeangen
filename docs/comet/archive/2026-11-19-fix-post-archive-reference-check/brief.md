# Outcome

Correct the FastBeanGen 0.3.0 PR verification defect exposed after legitimate Native Archive publication. The protected-input check must accept the exact historical state or the exact already accepted/published Spec, while still rejecting any unapproved modification. Resume delivery of PR #3 only after successful verification and CI; the existing merge/publication mandate is not a waiver of checks.

# Scope

Repair only the Java verification of protected inputs, its focused regression coverage and the relevant verification guidance. Reuse the current Gradle/JUnit source sets and selected Java 21/25 runtimes. Work on `comet/prepare-release-0-3-0` in its existing worktree under this new current-isolation change. Do not reopen or edit the completed `prepare-release-0-3-0` archive.

## Directory structure

### Created

- `verification/src/test/java/com/arboratum/beangen/verification/ProtectedInputTest.java` — small JUnit tests for accepted initial/published states and rejection of altered/truncated/unapproved input, using the existing verification-test source set.

### Modified

- `verification/src/test/java/com/arboratum/beangen/verification/ReferenceIntegrationTest.java` — retain the complete baseline boundary and byte comparisons; handle the sole legitimately published Spec with exact historical-prefix and approved-content protection. Keep all API and historical-runtime assertions unchanged. Make the protection summary distinguish unchanged inputs from the approved published Spec.
- `verification/README.md` — explain the narrow pre/post-Archive protection and its fixed accepted identity, without claiming that the expanded Spec is byte-identical to the shorter baseline.

### Deleted

None

### Not created

- No application module, Maven publication, general snapshot framework, dependency, CI bypass or broad exclusion. Existing Gradle tasks run the tests.
- No new product requirement or Spec modification: the automatically copied complete target Spec and its empty association delta remain identical to the already published capability.

# Non-goals

No library/example behavior, public API, PRNG, data/template, generated-value fixture, historical assertion/tolerance, dependency, Java target, license or release-coordinate change. No timeout increase, skipped protection, rewritten historical baseline, blanket allowance for appended text, accepted-Spec rewrite, change to earlier archives, force-push, merge before green CI, or publication without its actual prerequisites. Existing indentation diagnostics and signing/Portal limitations remain visible.

# No product behavior change

This is a correction to a verification test, not to the FastBeanGen product or its confirmed requirements. Archive legitimately appended the four already accepted release-preparation requirements to the published `modern-java-build` Spec. The current complete target and the empty association delta preserve that published Spec byte-for-byte; this change adds no capability requirement and must not publish another Spec appendage. It corrects the test's mistaken assumption that a deliberately published formal document can never grow. The accepted library and example implementation remains unchanged.

# Constraints and invariants

- The Git baseline remains `4a267527c030a1016c113714fc334c2c4844cbdd`; do not move it to the candidate or generate a new baseline from the implementation.
- The current boundary is 223 baseline paths. All 222 paths other than `docs/comet/specs/modern-java-build/spec.md` remain subject to the same full byte-for-byte comparison, with the same seven database-class and CUD-caller exceptions. Do not add a new source/archive exclusion.
- The historical Spec is exactly 10,245 bytes. The already accepted/published target is exactly 19,515 bytes with SHA-256 `1669cc4cccff23336e6a8a1ce8c4f4190b5cd3beeed1a2c028f3fae6c12ed195`, and its first 10,245 bytes equal the historical Spec exactly. Accept only the exact initial state or this exact approved published state. Reject arbitrary additions, changes, deletion or truncation. A future different target requires its own approved update; no dynamic self-baseline.
- Preserve the existing official six-artifact reference pins, 59-class/358-descriptor assertions and the original published runtime's eight probes/five passes/three exact characterized failures.
- The original preparation archive and all earlier archives are immutable. Test corrections must not edit formal historical evidence to make it agree with the test.

# Decisions

- CI push `44526103581` and PR `44526114328`, attempt 1 at head `a54e5a6da120750b0ed2865277d3e7e699cd0343`, both failed Java 21 in `protected_baseline_inputs_are_byte_identical`; both Java 25 jobs passed. Do not relabel or rerun those historical failures.
- A local targeted Java 21 run reproduced the failure with exit 1. Independent inspection of all 223 protected paths found only the published Spec different; its old content is an exact prefix and its whole current content equals the accepted archived target. Logs are administrative evidence under workspace `working/fastbeangen-release-0.3.0`, outside product sources.
- Keep one tightly scoped change. No independent deliverables justify Supervisor decomposition. Existing remote delivery/merge/publication authorization remains valid, but this new complete Shape and its verification/result gates remain separate.

# Acceptance examples

- A1: The real protected-input integration check passes both before and after the already approved publication. All 223 paths remain checked: 222 unchanged paths are fully byte-identical to the unchanged Git baseline; the single expanded Spec retains the exact 10,245-byte historical prefix and matches only the fixed approved 19,515-byte target/hash. No unapproved document/source/archive allowance or rewritten baseline is introduced.
- A2: Small genuine JUnit regressions pass on actual Java 21 and 25 for the exact initial and approved published states, and reject altered historical bytes, truncated/missing content, altered accepted added content and arbitrary additional content. Demonstrate a real failing regression or targeted reproduction before the correction, without changing protected expectations to the implementation's output.
- A3: Java 21 `verifyReference` passes all its checks, including the unchanged official API/pins and exactly characterized failing 0.2.17 runtime. Existing verification guards pass on actual Java 21 and 25; applicable local release verification passes with honest source identity. The code diff is confined to the stated verification files; product implementation, fixed dependency inputs, oracles and previous archives remain byte-identical to PR head `a54e5a6da120750b0ed2865277d3e7e699cd0343`. Guidance and output accurately distinguish the 222 unchanged paths from the approved expanded Spec.

# Verification expectations

Register focused Runtime checks for the protected-input regressions and existing verification guards on actual Java 21/25, the complete Java 21 reference characterization, applicable local staging/verification and a read-only scope/immutable-input audit. Reuse valid unchanged evidence where Runtime permits; do not rerun complete module suites before CI without need. A fresh read-only Native Verifier must assess A1–A3 and the owner must accept its result.

Delivery is subsequent work under the existing mandate, not a future remote-success claim inside this correction's acceptance criteria. After separately accepted verification and Archive/commit, publish only the scoped correction normally to PR #3, then require successful new Java 21/25 CI on the exact new head before a normal guarded merge and CI of the actual merge commit. Preserve branch/worktree, old failed runs and known limitations. Do not claim a GitHub/Central publication without its separate actual results.
