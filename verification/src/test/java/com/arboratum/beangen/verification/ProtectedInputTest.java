package com.arboratum.beangen.verification;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.util.Arrays;

import static com.arboratum.beangen.verification.PublicationVerification.ROOT;
import static com.arboratum.beangen.verification.ReferenceIntegrationTest.*;
import static com.arboratum.beangen.verification.ReleaseVerification.git;
import static org.junit.Assert.assertThrows;

public class ProtectedInputTest {
    private byte[] original;
    private byte[] approved;

    @Before public void loadFixedReferences() throws Exception {
        original = git(ROOT, "show", BASE + ":" + PROTECTED_SPEC);
        approved = Files.readAllBytes(ROOT.resolve("docs/comet/archive/2026-11-18-prepare-release-0-3-0/specs/modern-java-build/spec.md"));
    }

    @Test public void exact_initial_and_approved_published_states_are_accepted() {
        assertProtectedInput(PROTECTED_SPEC, original, original.clone());
        assertProtectedInput(PROTECTED_SPEC, original, approved);
    }

    @Test public void all_other_inputs_still_require_full_byte_identity() {
        assertProtectedInput("LICENSE", original, original.clone());
        assertThrows(AssertionError.class, () -> assertProtectedInput("LICENSE", original, approved));
    }

    @Test public void altered_historical_bytes_are_rejected_in_both_states() {
        for (byte[] state : new byte[][]{original, approved}) {
            byte[] changed = state.clone();
            changed[0] ^= 1;
            assertThrows(AssertionError.class, () -> assertProtectedInput(PROTECTED_SPEC, original, changed));
        }
    }

    @Test public void missing_and_truncated_content_is_rejected() {
        for (byte[] changed : new byte[][]{new byte[0], Arrays.copyOf(original, original.length - 1), Arrays.copyOf(approved, approved.length - 1)}) {
            assertThrows(AssertionError.class, () -> assertProtectedInput(PROTECTED_SPEC, original, changed));
        }
    }

    @Test public void altered_approved_addition_is_rejected() {
        byte[] changed = approved.clone();
        changed[original.length] ^= 1;
        assertThrows(AssertionError.class, () -> assertProtectedInput(PROTECTED_SPEC, original, changed));
    }

    @Test public void unapproved_appendices_are_rejected_in_both_states() {
        for (byte[] state : new byte[][]{original, approved}) {
            byte[] changed = Arrays.copyOf(state, state.length + 1);
            changed[state.length] = '!';
            assertThrows(AssertionError.class, () -> assertProtectedInput(PROTECTED_SPEC, original, changed));
        }
    }
}
