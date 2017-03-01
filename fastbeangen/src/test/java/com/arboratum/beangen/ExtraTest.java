package com.arboratum.beangen;

import com.arboratum.beangen.database.DataView;
import com.arboratum.beangen.util.RandomSequence;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by gpicron on 07/08/2016.
 */
public class ExtraTest {
    public static final int ROUNDS = 2000000;

    @Test
    public void generate() throws Exception {
        BitSet bs = new BitSet();
        byte[] versions = new byte[ROUNDS];

        Generator<DataView.OpCode> operationGenerator =  BaseBuilders.enumerated(DataView.OpCode.class)
                .values(DataView.OpCode.values())
                .weights(8714996, 4140789, 2012233)
                .build();



        int lastIndex = -1;

        Frequency previous = null;
        for (int i = 0; i < ROUNDS; i++) {
            switch (operationGenerator.generate(i))  {
                case CREATE: {
                    final int index = lastIndex + 1;// bs.nextClearBit(0);
                    if (index >= versions.length) {
                        versions = Arrays.copyOf(versions, versions.length + 1024);
                    }
                    if (versions[index] == 0) {
                        versions[index] = 1;
                    } else {
                        versions[index] = (byte) (-versions[index] + 1);
                    }

                    bs.set(index);
                    if (index > lastIndex) lastIndex = index;
                }
                    break;
                case UPDATE: {
                    if (lastIndex == -1) continue;
                    final RandomSequence randomSequence = new RandomSequence(i);

                    int select = randomSequence.nextInt(lastIndex);
                    final int index = bs.nextSetBit(select);

                    versions[index]++;
                }

                    break;
                case DELETE: {
                    if (lastIndex == -1) continue;
                    final RandomSequence randomSequence = new RandomSequence(i);

                    int select = randomSequence.nextInt(lastIndex);
                    final int index = bs.nextSetBit(select);

                    versions[index] = (byte)-versions[index];
                    bs.clear(index);

                    if (index == lastIndex) lastIndex = bs.previousSetBit(lastIndex);
                }

                    break;
            }


            if (i % 100000 == 0 && i != 0) {
                System.out.println("size of the db :" + bs.cardinality());

                Frequency frequencies = new Frequency();
                for (int j = 0; j < versions.length; j++) {
                    final byte version = versions[j];
                    if (version == 0) break;
                    frequencies.addValue(version);
                }

                System.out.println(frequencies);

                // convergence check
                if (previous != null) {
                    final double v = chiSquareTest(previous, frequencies);
                    System.out.println("chiSquareDataSetsComparison:" + v);

                    if (v > 0.999999) {
                        System.out.println("Converged !");
                        previous = frequencies;

                        break;
                    }
                }
                previous = frequencies;

            }
        }


        final Generator<Long> generator = BaseBuilders.enumerated(Long.class)
                .from(previous).build();
        
        double percentDeleted = previous.getCumPct(0);
        System.out.println("Percent deleted : " + percentDeleted);
        System.out.println("Percent active : " + (1d - percentDeleted));
        final int dbVectorSize = (int) (100000000 / (1d - percentDeleted));
        System.out.println("Db vector size : " + dbVectorSize);


        byte[] dbVector = new byte[dbVectorSize];
        for (int i = 0; i < dbVectorSize; i++) {
            dbVector[i] = generator.generate(i).byteValue();
        }
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


}