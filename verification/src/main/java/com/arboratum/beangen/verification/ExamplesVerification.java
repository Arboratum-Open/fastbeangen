package com.arboratum.beangen.verification;

import com.opencsv.CSVReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.arboratum.beangen.verification.PublicationVerification.*;

/** Gradle-selected JVMs execute the existing examples, with one shared 180-second deadline. */
public final class ExamplesVerification {
    private ExamplesVerification() { }

    static void run(String executable, String classpath) throws Exception {
        Path log = ROOT.resolve("build/verification/examples.log");
        Files.createDirectories(log.getParent());
        Files.writeString(log, "");
        long deadline = System.nanoTime() + Duration.ofSeconds(180).toNanos();
        var scenarios = List.of(
                List.of("SimpleGenAndQueryTest", "--persons", "10", "--relations", "0", "generate"),
                List.of("SimpleGenAndQueryTest", "--persons", "10", "--relations", "0", "query"),
                List.of("SimpleGenAndQueryTest", "--persons", "10", "--relations", "0", "cud"),
                List.of("DatabaseGeneratorAsCSVTest", "--persons", "10000", "--relations", "20"));
        // Match the existing generation/CSV tasks' cleanup, leaving all workloads unchanged.
        try (var paths = Files.walk(ROOT.resolve("fastbeangen-example/build/demo-data"))) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
        } catch (java.nio.file.NoSuchFileException ignored) {
            // No preceding embedded database to remove on a fresh checkout.
        }
        try (var paths = Files.list(ROOT.resolve("fastbeangen-example/build"))) {
            for (Path path : paths.filter(p -> p.getFileName().toString().endsWith(".csv")).toList()) Files.delete(path);
        }
        for (var scenario : scenarios) {
            var command = new ArrayList<>(List.of(executable, "-Xmx8g", "-cp", classpath,
                    "com.arboratum.beangen.example1." + scenario.getFirst()));
            command.addAll(scenario.subList(1, scenario.size()));
            var process = new ProcessBuilder(command).directory(ROOT.resolve("fastbeangen-example").toFile())
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile())).start();
            boolean finished = false;
            try {
                long remaining = deadline - System.nanoTime();
                finished = remaining > 0 && process.waitFor(remaining, TimeUnit.NANOSECONDS);
                check(finished, "Examples exceeded the unchanged 180-second deadline: " + log);
                check(process.exitValue() == 0, "Example failed: " + scenario + "; full output: " + log);
            } finally {
                if (!finished) VerificationProcess.terminate(process);
            }
        }
        verifyOutput(Files.readString(log), ROOT.resolve("fastbeangen-example/build"));
        System.out.println("Java " + Runtime.version() + " examples passed: query, six CUD checkpoints, 20000 persons, 20 relations, 47 links; full output: " + log);
    }

    static void verifyOutput(String output, Path directory) throws Exception {
        var failed = Pattern.compile("^KO: (.*)$", Pattern.MULTILINE).matcher(output).results().map(r -> r.group(1)).toList();
        long successful = Pattern.compile("^OK: LongSummaryStatistics\\{count=([1-9][0-9]*),", Pattern.MULTILINE).matcher(output).results().count();
        check(failed.size() >= 7 && failed.stream().allMatch("null"::equals), "Example query/CUD failures");
        check(successful == failed.size(), "Missing successful groups");
        check(output.split("CUD checkpoint ", -1).length - 1 == 6, "Expected six CUD checkpoints");
        check(output.contains("exported the db 3 files"), "Missing CSV export");
        var persons = rows(directory, "PERSON", 20000);
        var relations = rows(directory, "RELATION", 20);
        var links = rows(directory, "RELATION_PERSON", 47);
        Set<String> personIds = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            personIds.add(String.valueOf(i));
            personIds.add(String.valueOf(10000000 + i));
        }
        Set<String> relationIds = IntStream.range(0, 20).mapToObj(String::valueOf).collect(Collectors.toSet());
        check(IntStream.range(0, persons.getFirst().length).filter(i -> column(persons, i).equals(personIds)).count() == 1,
                "Expected exactly one complete person ID column");
        check(relations.getFirst().length == 1 && column(relations, 0).equals(relationIds), "Invalid relations");
        check(links.getFirst().length == 2, "Invalid link columns");
        var relationColumns = IntStream.range(0, 2).filter(i -> column(links, i).equals(relationIds)).boxed().toList();
        check(relationColumns.size() == 1, "Expected one relation ID column");
        check(links.stream().allMatch(row -> personIds.contains(row[1 - relationColumns.getFirst()])), "Unknown linked person");
    }

    static List<String[]> rows(Path directory, String name, int count) throws Exception {
        List<String[]> rows;
        try (var reader = new CSVReader(Files.newBufferedReader(directory.resolve(name + "_0.csv")))) {
            rows = reader.readAll();
        }
        check(rows.size() == count && rows.stream().allMatch(row -> row.length == rows.getFirst().length), "CSV row count/shape: " + name);
        return rows;
    }

    private static Set<String> column(List<String[]> rows, int column) {
        return rows.stream().map(row -> row[column]).collect(Collectors.toSet());
    }
}
