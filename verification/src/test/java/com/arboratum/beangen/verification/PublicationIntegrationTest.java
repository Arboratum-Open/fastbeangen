package com.arboratum.beangen.verification;

import org.junit.Test;

public class PublicationIntegrationTest {
    @Test public void actual_published_artifacts_retain_all_contracts() throws Exception {
        PublicationVerification.main(new String[0]);
    }
}
