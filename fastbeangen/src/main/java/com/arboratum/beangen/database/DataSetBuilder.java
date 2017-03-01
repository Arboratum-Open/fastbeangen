package com.arboratum.beangen.database;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import com.google.common.primitives.Bytes;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by gpicron on 11/02/2017.
 */
public class DataSetBuilder<T> {


    private int numInitialEntries;
    private Generator<DataView.OpCode> initOpGenerator;
    private Generator<DataView.OpCode> updateOpGenerator;
    private Generator<T> entryGenerator;
    private UpdateGenerator<T> updateGenerator;
    private List<DataSet.CreateTrigger<T>> createTriggers = new ArrayList<>();
    private List<DataSet.UpdateTrigger<T>> updateTriggers = new ArrayList<>();
    private Scheduler scheduler = Schedulers.single();

    public DataSetBuilder<T> of(int numEntries) {
        this.numInitialEntries = numEntries;
        return this;
    }

    public DataSetBuilder<T> initiallyGeneratedBy(double weightCreate, double weigthUpdate, double weightDelete) {
        initOpGenerator =  BaseBuilders.enumerated(DataView.OpCode.class)
                .values(DataView.OpCode.values())
                .weights(weightCreate, weigthUpdate, weightDelete)
                .build();
        return this;
    }

    public DataSetBuilder<T> thenUpdatedWith(double weightCreate, double weigthUpdate, double weightDelete) {
        updateOpGenerator =  BaseBuilders.enumerated(DataView.OpCode.class)
                .values(DataView.OpCode.values())
                .weights(weightCreate, weigthUpdate, weightDelete)
                .build();
        return this;
    }


    public DataSetBuilder<T> withEntryGenerator(Generator<T> entryGenerator) {
        this.entryGenerator = entryGenerator;
        return this;
    }

    public DataSetBuilder<T> withUpdateGenerator(UpdateGenerator<T> updateGenerator) {
        this.updateGenerator = updateGenerator;
        return this;
    }


    public DataSet<T> build() {
        Frequency previous = new Frequency();
        previous.addValue(1);
        if (initOpGenerator != null) {
            DataSet monteCarloSet = new DataSet<>(initOpGenerator);
            final Iterable<DataSet.Entry> dataSetOperations = monteCarloSet.buildOperationFeed().toIterable();
            int i = 0;

            System.out.print("Performing montecarlo procedure to estimate the version distribution");

            for (DataSet.Entry entry : dataSetOperations) {
                i++;
                if (i % 100000 == 0) {
                    System.out.print(".");
                    Frequency frequencies = new Frequency();
                    for (int j = 0; j <= monteCarloSet.getLastIndex(); j++) {
                        final byte version = monteCarloSet.getVersions()[j];
                        frequencies.addValue(version);
                    }

                    // convergence check
                    if (previous != null) {
                        final double v = chiSquareTest(previous, frequencies);

                        if (v > 0.999999) {
                            System.out.println(". Converged !");
                            previous = frequencies;

                            break;
                        }
                    }
                    previous = frequencies;

                }

            }
        }

        final Generator<Long> generator = BaseBuilders.enumerated(Long.class).from(previous).build();

        double percentDeleted = previous.getCumPct(0);
        System.out.println("Percent deleted : " + percentDeleted);
        System.out.println("Percent active : " + (1d - percentDeleted));
        System.out.println("Target active : " + numInitialEntries);

        int required = numInitialEntries;

        byte[] versions = new byte[(int) (numInitialEntries / (1d - percentDeleted))];
        int j = 0;
        for (; required > 0; j++) {
            versions = Bytes.ensureCapacity(versions, j+1, 1024);
            final byte v = generator.generate(j).byteValue();
            versions[j] = v;
            if (v > 0) required--;
        }

        return new DataSet<T>(updateOpGenerator, versions, j-1, entryGenerator, updateGenerator,
                (createTriggers.size() == 0) ? null : createTriggers.toArray(new DataSet.CreateTrigger[0]),
                (updateTriggers.size() == 0) ? null : updateTriggers.toArray(new DataSet.UpdateTrigger[0]), scheduler);
    }



    private double chiSquareTest(Frequency previous, Frequency current) {
        final int uniqueCount = current.getUniqueCount();
        long[] countsCurrent = new long[uniqueCount];
        long[] countsPrevious = new long[uniqueCount];

        final Iterator<Map.Entry<Comparable<?>, Long>> entryIterator = current.entrySetIterator();
        int i = 0;
        while (entryIterator.hasNext()) {
            final Map.Entry<Comparable<?>, Long> e = entryIterator.next();
            countsCurrent[i] = e.getValue();
            countsPrevious[i] = previous.getCount(e.getKey());
            i++;
        }

        return new ChiSquareTest().chiSquareTestDataSetsComparison(countsCurrent, countsPrevious);
    }

    public DataSetBuilder<T> onCreate(DataSet.CreateTrigger<T> trigger) {
        createTriggers.add(trigger);
        return this;
    }
    public DataSetBuilder<T> onUpdate(DataSet.UpdateTrigger<T> trigger) {
        updateTriggers.add(trigger);
        return this;
    }
}
