package com.arboratum.beangen.verification;

import com.opencsv.CSVWriter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertThrows;

public class ExamplesOutputTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    Path directory;
    String output;

    @Before public void fixture() throws Exception {
        directory = temporary.getRoot().toPath();
        output = "OK: LongSummaryStatistics{count=1,\nKO: null\n".repeat(7)
                + "CUD checkpoint fixture\n".repeat(6) + "exported the db 3 files\n";
        List<String[]> persons = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            persons.add(new String[] {String.valueOf(i), "quoted, name\nwith newline"});
            persons.add(new String[] {String.valueOf(10000000 + i), "fixture"});
        }
        write("PERSON", persons);
        List<String[]> relations = new ArrayList<>();
        for (int i = 0; i < 20; i++) relations.add(new String[] {String.valueOf(i)});
        write("RELATION", relations);
        List<String[]> links = new ArrayList<>();
        for (int i = 0; i < 47; i++) links.add(new String[] {String.valueOf(i % 20), String.valueOf(i)});
        write("RELATION_PERSON", links);
    }

    private void write(String name, List<String[]> rows) throws Exception {
        try (var writer = new CSVWriter(Files.newBufferedWriter(directory.resolve(name + "_0.csv")))) {
            writer.writeAll(rows);
        }
    }

    @Test public void accepts_complete_output_and_quoted_multiline_csv() throws Exception {
        ExamplesVerification.verifyOutput(output, directory);
    }

    @Test public void rejects_failed_missing_or_extra_output_groups() {
        for (String invalid : List.of(output.replaceFirst("KO: null", "KO: failed"), output.replace("OK: ", "missing: "),
                output.replaceFirst("CUD checkpoint ", "missing checkpoint "), output.replace("exported the db 3 files", "missing export"))) {
            assertThrows(IllegalStateException.class, () -> ExamplesVerification.verifyOutput(invalid, directory));
        }
    }

    @Test public void rejects_missing_rows_and_invalid_link_targets() throws Exception {
        Files.writeString(directory.resolve("RELATION_PERSON_0.csv"), "0,0\n");
        assertThrows(IllegalStateException.class, () -> ExamplesVerification.verifyOutput(output, directory));
        List<String[]> links = new ArrayList<>();
        for (int i = 0; i < 47; i++) links.add(new String[] {String.valueOf(i % 20), "unknown"});
        write("RELATION_PERSON", links);
        assertThrows(IllegalStateException.class, () -> ExamplesVerification.verifyOutput(output, directory));
    }
}
