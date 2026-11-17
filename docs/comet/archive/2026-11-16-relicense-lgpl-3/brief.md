# Outcome

Change FastBeanGen's current first-party distribution grant from GPL version 3 to GNU Lesser General Public License version 3.0 only (`LGPL-3.0-only`), making integration and redistribution with applications under other licenses possible subject to the LGPL's conditions. LGPL is weak copyleft, not a permissive MIT/Apache license. No automatic permission to elect later LGPL versions is granted.

This applies to the project's current sources and next locally built artifacts, including the still-unreleased `0.3.0`; it neither revokes previously granted GPL rights nor rewrites historical releases. The owner has explicitly confirmed the necessary rights or consents for the affected contributions and resources. Record that attestation without claiming an independent legal clearance.

# Scope

Use the existing three-module Gradle project and publication layout. Align the project license declaration, verbatim license texts, first-party artifact notices, published POMs, current documentation, and publication verification. First-party sources, examples, documentation, and bundled name dictionaries/document templates are covered subject to preserved third-party rights. Do not blanket-relicense dependencies or the Gradle wrapper.

LGPL v3 incorporates GPL v3. Distribute both texts: the canonical LGPL v3 in `LICENSE`, and the existing GPL v3 text retained byte-for-byte as `COPYING`. Include both in the first-party binary, sources, Javadoc, and example JAR variants that the existing build produces, without replacing notices belonging to dependencies. Preserve artifact coordinates, the unreleased version, and existing publication topology.

Update this change's complete `modern-java-build` target Spec to supersede the former GPL-only constraint. Runtime alone publishes the current project Spec at Archive; the modernization archive remains historical and untouched.

## Directory structure

### Created

- `COPYING` — retain the previous GPL v3 license text as the incorporated base terms.

### Modified

- `LICENSE` — canonical, unmodified LGPL v3 text; the precise project grant is declared consistently in metadata and documentation.
- `build.gradle` — shared first-party POM license metadata and legal-text packaging for the existing modules/artifact variants.
- `README.md` — current LGPL grant, scope, both license files, and concise redistribution/integration obligations.
- `CHANGELOG.md` — record the licensing change in the unreleased version without rewriting older release history.
- `docs/modernization.md` — remove stale current-license claims while retaining modernization evidence and publication limitations.
- `verification/` — extend the existing publication verifier and focused self-checks for license metadata and packaged texts, retaining existing checks and consumer smoke coverage.
- `docs/comet/changes/relicense-lgpl-3/brief.md` and `specs/modern-java-build/{spec.md,delta.yaml}` — requirements and the complete updated target contract with its Runtime-created association metadata.

### Deleted

None

### Not created

- A separate module, license framework, dependency, or standalone legal guide — existing Gradle configuration, verifier, and README suffice.
- New publication destinations, credentials, or release automation — only local artifact verification is in scope.

# Constraints and invariants

Keep Java 21 minimum/bytecode compatibility, Java 21/25 support, all three modules, public APIs, PRNG, generated outputs, bundled data/templates, and behavioral test fixtures/assertions unchanged. License-related verification expectations may change to reflect the new grant; existing unrelated assertions must not be weakened. Preserve wrapper/dependency copyright and license notices and all archived Comet evidence. Do not copy private prototype/DB materials into the repository.

Use a single writer/build in the dedicated Runtime-owned worktree. The local skill maintenance authorized separately by the owner is bootstrap only, not an implementation change or final Shape approval. Do not include bootstrap resources in a later implementation commit without a separate mandate.

# Decisions

- Q1 — owner selected a dedicated worktree. Runtime created `relicense-lgpl-3` on `comet/relicense-lgpl-3` from `master` at `6416eddfe6b38499ba647cfbda1b584a8327e327`; the previous modernization worktree was preserved.
- Q2 — owner selected LGPL-3.0 rather than Apache-2.0 or MIT. Do not substitute a permissive license or dual-license the project.
- Q3 — owner explicitly answered that the rights or required consents cover the contributions and resources concerned, excluding third-party components that retain their own licenses. This is the owner's attestation, not a completed provenance audit.
- Q4 — owner explicitly selected `LGPL-3.0-only`, not `LGPL-3.0-or-later`. README and POM licensing statements must identify version 3.0 only, using the canonical GNU LGPL v3 link and an unambiguous grant name/SPDX identifier.
- The complete existing `modern-java-build` capability is updated only for the licensing/distribution contract. Its legacy baseline has no stable requirement IDs: preserve that baseline and add the stable `project-licensing` requirement, which explicitly supersedes the two historical GPL-preservation/license-change-exclusion clauses. Unrelated technical and compatibility requirements remain effective. The association delta records this addition without fabricating IDs or hashes for legacy paragraphs.

# Non-goals

No generator/API/runtime/dependency changes, source-data regeneration, behavioral oracle changes, benchmarks, full vulnerability or chain-of-title audit, or legal certification. No changes to another repository, the private administrative prototype, historical tags/releases, or the modernization archive. No remote artifact upload, signing claim, Git commit/push, PR, merge, or cleanup is authorized by this Shape. Archive and delivery remain separate human gates.

# Acceptance examples

- The repository explicitly grants `LGPL-3.0-only` for the current first-party project. `LICENSE` is the canonical LGPL v3 text and `COPYING` exactly preserves the previous GPL v3 text; both are included rather than claiming LGPL is an unrelated standalone permissive license.
- Local core/document publication POMs consistently name and link the selected grant. All first-party JAR variants produced by existing build tasks, including binary, sources, Javadoc and executable examples where applicable, contain the two correct legal texts without duplicate first-party license entries. Existing coordinates and publication variants are retained.
- Current README, unreleased changelog and publication/migration guidance agree with the selected grant, describe the LGPL obligations without promising unrestricted proprietary redistribution, distinguish first-party and third-party material, and do not claim a Central release or independently verified legal ownership. The owner's rights attestation is retained in this brief.
- Wrapper/dependency notices, historical release/Comet evidence, and previously granted GPL rights are preserved. References to GPL as incorporated base terms or historical/dependency licensing remain valid; no blanket replacement removes them. No unrelated repository, private prototype material, or bootstrap file is added to the implementation change.
- Focused verifier self-checks demonstrate detection of wrong/missing license metadata or packaged texts. Local publication/archive checks and the existing independent consumer smoke run on actual Java 21 and 25, without remote publication. A reviewed diff confirms no changes to Java behavior, bundled data/templates, PRNG, behavioral fixtures/assertions, runtime versions or dependencies; existing publication checks remain effective. Unrun heavy suites are reported rather than claimed passed.

# Verification expectations

Use the official GNU LGPL v3 text at `https://www.gnu.org/licenses/lgpl-3.0.txt`; the inspected reference SHA-256 is `e3a994d82e644b03a792a930f574002658412f62407f5fee083f2555c5f23118`. Compare GPL `COPYING` to `LICENSE` from the confirmed base commit, not to the candidate's own regenerated text. Inspect generated POMs/JAR contents rather than only build-script declarations. Keep targeted development checks separate from the final registered Runtime plan and independent Verifier assessment; do not rerun heavy behavioral suites merely for legal-text/metadata changes.
