package com.arboratum.beangen.example1;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.core.BeanGeneratorBuilder;
import com.arboratum.beangen.database.DataSet;
import com.arboratum.beangen.database.DataView;
import com.arboratum.beangen.database.Database;
import com.arboratum.beangen.database.UpdateOf;
import com.arboratum.beangen.example1.model.*;
import com.arboratum.beangen.util.RandomSequence;
import com.beust.jcommander.Parameter;
import com.google.common.base.Stopwatch;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.arboratum.beangen.BaseBuilders.*;

/**
 * Created by gpicron on 16/02/2017.
 */
public class ComplexDatabase {
    @Parameter(names = "--persons", required = true, echoInput = true, description = "Number of persons in the initial database")
    private int initNumPersons = 1000;
    @Parameter(names = "--relations", required = true, echoInput = true, description = "Number of relations in the initial database")
    private int initNumRelations = 1000;
    private Database database;

    public ComplexDatabase() {

    }

    public void initialize() {
        database = new Database();

        // configure the generators of for persons dataset
        final Generator addressGenerator = aBean(Person.Address.class)
                .with("street", aString().matching("[A-Z][a-z]{2,10} street"))
                .with("number", aString().matching("[0-9]{1,3}[A-Z]?"))
                .with("city", enumerated(String.class).values("Chicago", "New York"))
                .build();

        final Generator<Person> personOfBEGenerator = aBean(Person.class)
                .injectIdIn("id")
                .with("identity", anIdentity().country("be"))
                .with("address", addressGenerator)
                .build();

        final BeanGeneratorBuilder<Person> basePeresonOfFRGenerator = aBean(Person.class)
                .injectIdIn("id")
                .with("identity", anIdentity().country("fr"))
                .with("address", addressGenerator);
        final Generator<Person> personOfFRGenerator = basePeresonOfFRGenerator
                .postProcess((person, randomSequence) -> { person.setId(person.getId() + 10000000); return person;}) // to avoid PK conflict
                .build();

        final Generator<UpdateOf<Person>> personUpdateGenerator = enumerated(UpdateOf.class)
                .generators(
                        aBean(FixTypoErrorInLastNameCommand.class).withDefaultFieldPopulators(),
                        aBean(ChangeAddressCommand.class).with("newAddress", addressGenerator))
                .weights(1, 2)
                .build();


        // configure the generators of relations
        final Generator<Relation> relationGenerator = aBean(Relation.class)
                .injectIdIn("id") // we don't generate the list of persons as we will have to memoize it because it is state dependent
                .build();
        final Generator<UpdateOf<Relation>> relationUpdateGenerator = enumerated(UpdateOf.class)
                .generators(
                        aBean(JoinRelationCommand.class), // we don't generate the added person as we will have to memoize it because it is state dependent
                        aBean(LeaveRelationCommand.class).with("index", aInteger().uniform(0, 1000)))
                .weights(2, 1)
                .build();

        // the triggers is used to memoize the generated relations
        final DataSet.CreateTrigger<Relation> personCreateTrigger = new DataSet.CreateTrigger<Relation>() {
            private Map<Integer, DataSet.Entry[]> memory = new HashMap<>(); // to support large maps, prefer off-heap Chronicle Map

            @Override
            public void apply(int index, Relation value) {
                final DataView<Person> persons = database.getDataView("persons");

                DataSet.Entry[] refs = memory.get(index);
                if (refs == null) {
                    final RandomSequence r = new RandomSequence(index);
                    final int numRel = r.nextInt(2) + 2;
                    refs = new DataSet.Entry[numRel];

                    for (int i = 0; i < numRel; i++) {
                        refs[i] = persons.selectOne(r);
                    }
                    memory.put(index, refs);
                }
                final ArrayList<Person> people = new ArrayList<>(refs.length);
                for (DataSet.Entry ref : refs) {
                    people.add((Person) ref.lastVersion().block());
                }

                value.setPersons(people);

            }
        };
        final DataSet.UpdateTrigger<Relation> personUpdateTrigger = new DataSet.UpdateTrigger<Relation>() {

            private Map<Tuple2<Integer,Byte>, DataSet.Entry> memory = new HashMap<>(); // to support large maps, prefer off-heap Chronicle Map

            @Override
            public void apply(int index, byte version, UpdateOf<Relation> value) {
                if (value instanceof JoinRelationCommand) {
                    final DataView<Person> persons = database.getDataView("persons");

                    final Tuple2<Integer, Byte> key = Tuples.of(index, version);
                    DataSet<Person>.Entry ref = memory.get(key);
                    if (ref == null) {
                        final RandomSequence r = new RandomSequence(index);
                        ref = persons.selectOne(r);

                        memory.put(key, ref);
                    }

                    ((JoinRelationCommand) value).setAddedPerson(ref.lastVersion().block());
                }
            }
        };

        // register the dataset in the database
        database.addDataSet("persons_BE", DataSet.<Person>builder()
                .withEntryGenerator(personOfBEGenerator)
                .withUpdateGenerator((previousValue, randomSequence) -> personUpdateGenerator.generate(randomSequence))
                .of(initNumPersons)
                .thenUpdatedWith(30,50,20));
        database.addDataSet("persons_FR", DataSet.<Person>builder()
                .withEntryGenerator(personOfFRGenerator)
                .withUpdateGenerator((previousValue, randomSequence) -> personUpdateGenerator.generate(randomSequence))
                .of(initNumPersons)
                .thenUpdatedWith(30,50,20));

        database.createUnionView("persons", "persons_BE", "persons_FR");

        database.addDataSet("relations", DataSet.<Relation>builder()
                .withEntryGenerator(relationGenerator)
                .withUpdateGenerator((previousValue, randomSequence) -> relationUpdateGenerator.generate(randomSequence))
                .of(initNumRelations)
                .onCreate(personCreateTrigger)
                .onUpdate(personUpdateTrigger));

        database.initialize();

        Stopwatch stopwatch = Stopwatch.createStarted();

        database.getDataView("relations").traverseDataSet(true)
                .parallel()
                .runOn(Schedulers.parallel())
                .map(DataSet.Entry::lastVersion)
                .sequential()
                .count().block();

        System.out.println("Initializing 'relations' tooks : " + stopwatch.stop());
    }

    public Flux<Person> traversePersons() {
        return database.<Person>getDataView("persons").traverseDataSet(true).flatMap(DataSet.Entry::lastVersion);
    }
    public Flux<Relation> traverseRelations() {
        return database.<Relation>getDataView("relations").traverseDataSet(true).flatMap(DataSet.Entry::lastVersion);
    }

    public <T> DataView<T> getDataView(String name) {
        return database.<T>getDataView(name);
    }

    public Generator<Person> randomPerson() {
        return database.<Person>getDataView("persons").random();
    }

    public int getInitNumPersons() {
        return this.initNumPersons;
    }

    public int getInitNumRelations() {
        return this.initNumRelations;
    }

    public Database getDatabase() {
        return this.database;
    }
}
