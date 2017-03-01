package com.arboratum.beangen.example1;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.database.DataSet;
import com.arboratum.beangen.example1.model.Person;
import com.beust.jcommander.JCommander;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import javax.persistence.EntityManagerFactory;
import java.io.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This is simple example show how generate a test SQL database using hibernate and H2
 *
 * Created by gpicron on 14/02/2017.
 *
 *
 */
@SpringBootApplication
@EnableTransactionManagement
public class SimpleGenAndQueryTest {
    public static void main(String[] args) {
        SpringApplication.run(SimpleGenAndQueryTest.class, args);
    }

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return this::run;
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    TestDbDAO dao;


    public void run(String[] args) {
        final ComplexDatabase base = new ComplexDatabase();
        JCommander jCommander = new JCommander(base);
        try {
            jCommander.parseWithoutValidation(Arrays.copyOf(args, args.length-1));
        } catch (Exception e) {
            System.err.println("Error:" + e.getMessage());
            jCommander.usage();
            System.exit(1);
        }

        try {
            base.initialize();
            final File file = new File("build/cud_counter");


            final String command = args[args.length - 1];
            if ("generate".equalsIgnoreCase(command)) {
                createInitialDB(base);
                if (file.exists()) file.delete();

            } else if ("query".equalsIgnoreCase(command)) {
                final long lastDone = getLastCUDDone(file);

                if (lastDone > 0)  {
                    System.out.println("Update version vectors with the cud performed in previous tests");
                    base.<Person>getDataView("persons")
                            .buildOperationFeed().take(lastDone).doOnNext(DataSet.Operation::ack).count().block();
                }

                final Flux<Map<Boolean, LongSummaryStatistics>> queryFlux =
                        createQueryFlux(base, RateLimiter.create(300, 30, TimeUnit.SECONDS));

                queryFlux
                        .take(Duration.ofMinutes(1)) // test is planned for 1 minutes
                        .last().block();
            } else if ("cud".equalsIgnoreCase(command)) {

                final Flux<Map<Boolean, LongSummaryStatistics>> cudFlux =
                        createCUDFlux(base, file, RateLimiter.create(20, 5, TimeUnit.SECONDS))
                                .take(6)
                                .subscribeOn(Schedulers.newSingle("cud"));


                cudFlux.last().block();


            } else {
                System.err.println("Bad command");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    private Flux<Map<Boolean, LongSummaryStatistics>> createQueryFlux(ComplexDatabase base, RateLimiter limiter) {
        Generator<Person> randomPerson = base.randomPerson();
        Stopwatch testStopwatch = Stopwatch.createStarted();
        final Scheduler scheduler = Schedulers.newParallel("query-invoke", 4);

        return Flux.range(0, Integer.MAX_VALUE)
                .map(i -> {
                    limiter.acquire();
                    return randomPerson.generate(i);
                })
                .flatMap((Person person) -> {
                    return Mono.defer(() -> {
                        Stopwatch stopwatch = Stopwatch.createStarted();

                        final Person loaded = dao.load(person.getId());
                        stopwatch.stop();

                        final boolean equals = person.equals(loaded);
                    /*if (!equals) {
                        System.out.println("generated:" + person);
                        System.out.println("indb:" + loaded);
                    }*/
                        return Mono.just(Tuples.of(equals, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
                    }).subscribeOn(scheduler);
                })
                .buffer(Duration.ofSeconds(10))
                .map(samples -> {
                    final Map<Boolean, LongSummaryStatistics> stats = samples.stream().collect(Collectors
                            .groupingBy(Tuple2::getT1, Collectors.summarizingLong(Tuple2::getT2)));
                    System.out.println("--------------");
                    System.out.println("elapsed: " + testStopwatch);
                    System.out.println("OK: " + stats.get(true));
                    System.out.println("KO: " + stats.get(false));
                    return stats;
                });
    }

    private Flux<Map<Boolean, LongSummaryStatistics>> createCUDFlux(ComplexDatabase base, File file, final RateLimiter rateLimiter) throws IOException {
        Stopwatch testStopwatch = Stopwatch.createStarted();

        final long lastDone = getLastCUDDone(file);

        System.out.println("Skipping cud:" + lastDone);

        final Flux<DataSet<Person>.Operation> cuds =

                base.<Person>getDataView("persons")
                    .buildOperationFeed()
                        .concatMap(operation -> {
                            if (operation.getSequenceId() <= lastDone) {
                                operation.ack();
                                return Mono.empty();
                            } else {
                                return Mono.just(operation);
                            }
                        });

        return cuds
                .map(operation -> {
                    Stopwatch stopwatch = Stopwatch.createStarted();

                    try {
                        final DataSet<Person>.Entry entry = operation.getEntry();
                        switch (entry.getLastOperation()) {
                            case CREATE:
                                dao.insert(entry.lastVersion().block());
                                break;

                            case DELETE:
                                dao.delete(entry.lastVersion().block());
                                break;

                            case UPDATE:
                                dao.update(entry.lastVersion().block());
                                break;
                        }
                        return Tuples.of(true, stopwatch.elapsed(TimeUnit.MILLISECONDS), operation);
                    } catch (Exception e) {
                        System.out.println("Failed processing " + operation + " : " + e.getMessage());
                        return Tuples.of(false, stopwatch.elapsed(TimeUnit.MILLISECONDS), operation);
                    }
                })
                .buffer(Duration.ofSeconds(1))
                .map(dataSetEntries -> {
                    dataSetEntries.forEach(t -> t.getT3().ack());
                    Tuple3<Boolean, Long, DataSet<Person>.Operation> lastOperationProcessed = dataSetEntries.get(dataSetEntries.size() - 1);
                    try {
                        final FileWriter fileWriter = new FileWriter(file);
                        fileWriter.write("" + lastOperationProcessed.getT3().getSequenceId() + "\n");
                        fileWriter.write("-- " + lastOperationProcessed + "\n");
                        fileWriter.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    final Map<Boolean, LongSummaryStatistics> stats = dataSetEntries.stream()
                            .map(t -> Tuples.of(t.getT1(), t.getT2(), t.getT3().getSequenceId()))
                            .collect(Collectors
                            .groupingBy(Tuple3::getT1, Collectors.summarizingLong(Tuple3::getT2)));
                    System.out.println("--------------");
                    System.out.println("CUD checkpoint " + lastOperationProcessed);
                    System.out.println("elapsed: " + testStopwatch);
                    System.out.println("OK: " + stats.get(true));
                    System.out.println("KO: " + stats.get(false));
                    return stats;
                });
    }

    private long getLastCUDDone(File file) throws IOException {
        final long lastDone;
        if (file.exists()) {
            final BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            final String s = bufferedReader.readLine();
            lastDone = Long.parseLong(s);
            bufferedReader.close();
        } else {
            lastDone = -1;
        }
        return lastDone;
    }


    private void createInitialDB(ComplexDatabase base) {

        Stopwatch stopwatch = Stopwatch.createStarted();

        final int transactionSize = 50;

        final Mono<LongSummaryStatistics> injectPersons = base.traversePersons()
                .buffer(transactionSize)
                .map(dao::store)
                .collect(LongSummaryStatistics::new, LongSummaryStatistics::combine);
        final Mono<LongSummaryStatistics> injectRelations = base.traverseRelations()
                .buffer(transactionSize)
                .map(dao::store)
                .collect(LongSummaryStatistics::new, LongSummaryStatistics::combine);

        final LongSummaryStatistics block = injectPersons.concatWith(injectRelations)
                .collect(LongSummaryStatistics::new, LongSummaryStatistics::combine)
                .block();

        jdbcTemplate.execute("SHUTDOWN COMPACT");

        System.out.println("exported the db  in " + stopwatch.stop());

        System.out.println(block);
    }
}