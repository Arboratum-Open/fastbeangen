package com.arboratum.beangen.example1;

import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.ResultSetHelper;
import au.com.bytecode.opencsv.ResultSetHelperService;
import com.arboratum.beangen.database.DataSet;
import com.arboratum.beangen.database.DataView;
import com.arboratum.beangen.example1.model.Person;
import com.arboratum.beangen.example1.model.Relation;
import com.beust.jcommander.JCommander;
import com.google.common.base.Stopwatch;
import com.google.common.io.Closeables;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.SoftReferenceObjectPool;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.orm.hibernate4.HibernateTemplate;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * This is example show how generate a test csv database using hibernate and H2
 *
 * Strategy to accelerate the production are coming from embulk project but is implemented using
 * Reactor.
 *
 * Here we use H2 as a buffer then batch generate files for each tables every 100000 entries.
 *
 *
 *
 * Created by gpicron on 14/02/2017.
 */

public class DatabaseGeneratorAsCSVTest {

    private final AtomicInteger dbInPath = new AtomicInteger();
    private final SoftReferenceObjectPool<TemporaryDatabase> dbs = new SoftReferenceObjectPool<>(new BasePooledObjectFactory<TemporaryDatabase>() {
        AtomicInteger id = new AtomicInteger();

        @Override
        public TemporaryDatabase create() throws Exception {
            return new TemporaryDatabase(id.getAndIncrement());
        }

        @Override
        public PooledObject<TemporaryDatabase> wrap(TemporaryDatabase obj) {
            return new DefaultPooledObject<>(obj);
        }

        @Override
        public void destroyObject(PooledObject<TemporaryDatabase> p) throws Exception {
            p.getObject().close();
            super.destroyObject(p);
        }

        @Override
        public void passivateObject(PooledObject<TemporaryDatabase> p) throws Exception {
            p.getObject().passivate();
            super.passivateObject(p);
        }
    });

    public static void main(String[] args)  {
        new DatabaseGeneratorAsCSVTest().run(args);
    }

    private static class TemporaryDatabase implements Closeable {
        private final int id;
        private final DriverManagerDataSource dataSource;
        private final JdbcTemplate jdbcTemplate;
        private final SessionFactory sessionFactory;
        private final HibernateTemplate hibernateTemplate;



        public TemporaryDatabase(int id) throws IOException {
            this.id = id;
            dataSource = new SingleConnectionDataSource("jdbc:hsqldb:mem:temp_"+ id + ";hsqldb.log_size=0;hsqldb.write_delay=false;", "sa","", true );
            dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");

            jdbcTemplate = new JdbcTemplate(dataSource);

            jdbcTemplate.execute("SET FILES LOG FALSE");
            jdbcTemplate.execute("SET DATABASE REFERENTIAL INTEGRITY FALSE");




            LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
            sessionFactoryBean.setAnnotatedClasses(Person.class, Relation.class);
            sessionFactoryBean.setDataSource(dataSource);

            final Properties hibernateProperties = new Properties();
            hibernateProperties.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
            hibernateProperties.put("hibernate.hbm2ddl.auto", "create");
            hibernateProperties.put("hibernate.show_sql", "false");
            hibernateProperties.put("hibernate.jdbc.batch_size", "1000");
            hibernateProperties.put("hibernate.bytecode.use_reflection_optimizer", "true");
            hibernateProperties.put("hibernate.cache.use_second_level_cache", "false");

            sessionFactoryBean.setHibernateProperties(hibernateProperties);

            sessionFactoryBean.afterPropertiesSet();

            sessionFactory = sessionFactoryBean.getObject();
            hibernateTemplate = new HibernateTemplate(sessionFactory);

        }

        public void close() throws IOException {
            Closeables.close(sessionFactory, true);
            jdbcTemplate.execute("SHUTDOWN");
        }

        public void passivate() throws MetaDataAccessException {
            try {
                jdbcTemplate.execute("TRUNCATE SCHEMA PUBLIC AND COMMIT NO CHECK");
            } catch (DataAccessException e) {
                e.printStackTrace();
            }

        }

        private List<String> getTables() {
            try {
                return (List<String>) JdbcUtils.extractDatabaseMetaData(dataSource, new DatabaseMetaDataCallback() {
                                @Override
                                public Object processMetaData(DatabaseMetaData dbmd) throws SQLException, MetaDataAccessException {
                                    ResultSet rs = dbmd.getTables(null, "PUBLIC", "%", null);
                                    List<String> tables1 = new ArrayList<>();
                                    while (rs.next()) {
                                        tables1.add(rs.getString(3));
                                    }
                                    return tables1;
                                }
                            });
            } catch (MetaDataAccessException e) {
                throw new RuntimeException(e);
            }
        }


        public int getId() {
            return this.id;
        }

        public DriverManagerDataSource getDataSource() {
            return this.dataSource;
        }

        public JdbcTemplate getJdbcTemplate() {
            return this.jdbcTemplate;
        }

        public SessionFactory getSessionFactory() {
            return this.sessionFactory;
        }

        public HibernateTemplate getHibernateTemplate() {
            return this.hibernateTemplate;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof TemporaryDatabase)) return false;
            final TemporaryDatabase other = (TemporaryDatabase) o;
            if (!other.canEqual((Object) this)) return false;
            if (this.getId() != other.getId()) return false;
            return true;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + this.getId();
            return result;
        }

        protected boolean canEqual(Object other) {
            return other instanceof TemporaryDatabase;
        }

        public String toString() {
            return "com.arboratum.beangen.example1.Step1TestDatabaseGeneratorAsCSV.TemporaryDatabase(id=" + this.getId() + ")";
        }
    }




    public void run(String[] args)  {
        final ComplexDatabase base = new ComplexDatabase();
        JCommander jCommander = new JCommander(base);
        try {
            jCommander.parse(args);
        } catch (Exception e) {
            jCommander.usage();
            System.exit(1);
        }

        base.initialize();


        Stopwatch stopwatch = Stopwatch.createStarted();

        final int numberOfItemPerCSV = 100000;

        final DataView<Person> persons = base.getDataView("persons");
        final DataView<Relation> relations = base.getDataView("relations");

        final Flux personEntryFlux = persons.traverseDataSet(false);
        Flux<TemporaryDatabase> personDbFlux =
                bufferInMemorySQLDbUsingHibernate(personEntryFlux)
                .subscribeOn(Schedulers.newSingle("persons-to-db"));

        final Flux relationEntryFlux = relations.traverseDataSet(false);
        final Flux<TemporaryDatabase> relationsDbFlux =
                bufferInMemorySQLDbUsingHibernate(relationEntryFlux)
                .subscribeOn(Schedulers.newSingle("relations-to-db"));

        Flux<TemporaryDatabase> temporaryDatabaseFlux = Flux.merge(4, personDbFlux, relationsDbFlux)
                .publishOn(Schedulers.newSingle("dump-to-csv"), 8);


        final Flux<Tuple2<String, String[]>> rows = dumpAllRows(temporaryDatabaseFlux);
        final Long filesWritten = writeRowsToCSV(numberOfItemPerCSV, rows).block();

        stopwatch.stop();
        long rate = (persons.getSize() +  base.getDataView("relations").getSize()) / stopwatch.elapsed(TimeUnit.SECONDS);

        System.out.println("exported the db "+filesWritten+" files in " + stopwatch + " rate " +  rate + " entries/s");

        System.exit(0);
    }

    private Mono<Long> writeRowsToCSV(int numberOfItemPerCSV, Flux<Tuple2<String, String[]>> rows) {
        return rows
                // group by table
                .groupBy(Tuple2::getT1, Tuple2::getT2)
                // write to CSV files
                .flatMap(perTablePlux -> {
                    final String tableName = perTablePlux.key();
                    final Flux<Tuple2<Flux<String[]>, Integer>> rowBlocks = perTablePlux
                            // csv of 100000 rows
                            .window(numberOfItemPerCSV)
                            // associate a sequence id
                            .zipWith(Flux.range(0, Integer.MAX_VALUE));
                    return rowBlocks
                            // produce the file
                            .flatMap(flux -> {
                                final String fileName = tableName + "_" + flux.getT2() + ".csv";
                                final CSVWriter csvWriter;
                                try {
                                    csvWriter = new CSVWriter(new FileWriter("build/" + fileName));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }

                                return flux.getT1()
                                        .map(strings -> { csvWriter.writeNext(strings); return true; })
                                        .count().map(c -> Tuples.of(fileName, c))
                                        .doOnTerminate((keyCount, throwable) -> {
                                            try {
                                                csvWriter.close();
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        });
                            });
                })
                .log()
                .count();
    }

    private Flux<Tuple2<String, String[]>> dumpAllRows(Flux<TemporaryDatabase> temporaryDatabaseFlux) {
        return temporaryDatabaseFlux  // limit the number Temp DB in memory
                .concatMap(resource ->
                    Flux.create((Consumer<FluxSink<Tuple2<String, String[]>>>) fluxSink -> {
                        Stopwatch stopwatch1 = Stopwatch.createStarted();

                        for (String table : resource.getTables()) {
                            System.out.println("-- traversing table " + table);
                            final ResultSetHelper resultService = new ResultSetHelperService();

                            final Integer count = resource.getJdbcTemplate().query("select * from PUBLIC." + table, rs -> {
                                int counter = 0;
                                try {
                                    while (rs.next()) {
                                        counter++;
                                        fluxSink.next(Tuples.of(table, resultService.getColumnValues(rs)));
                                    }
                                } catch (IOException e) {
                                    fluxSink.error(e);
                                }
                                return counter;
                            });
                            System.out.println("-- completed traversing table " + table + " count:" + count);
                        }

                        try {
                            System.out.println("- droping " + dbInPath.decrementAndGet());
                            //resource.close();
                            dbs.returnObject(resource);
                        } catch (Exception e) {
                            fluxSink.error(e);
                        }

                        fluxSink.complete();
                        System.out.println("Fully dumped " + resource + " in " + stopwatch1.stop());
                    }));
    }

    private Flux<TemporaryDatabase> bufferInMemorySQLDbUsingHibernate(Flux<DataSet.Entry> source) {
        return source
                .buffer(20000)
                .map(peopleBlock -> {
                    final TemporaryDatabase resource;
                    try {
                        resource = dbs.borrowObject();

                        System.out.println("- creating " + dbInPath.incrementAndGet());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    final Session session = resource.getSessionFactory().openSession();

                    final Transaction transaction = session.beginTransaction();


                    for (int i = 0, peopleBlockSize = peopleBlock.size(); i < peopleBlockSize; i++) {
                        DataSet.Entry e = peopleBlock.get(i);
                        session.persist(e.lastVersion().block());
                        if (i % 50 == 0) {
                            session.flush();
                            session.clear();
                        }
                    }
                    transaction.commit();
                    session.close();

                    return resource;
                });
    }

}
