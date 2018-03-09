package com.arboratum.beangen.database;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Created by gpicron on 13/02/2017.
 */
public class DataSetTest {


    private static class Pojo {
    }

    @Test(expected = IllegalStateException.class)
    public void buildOperationFeedCanBeCalledOnceOnly() throws Exception {
        DataSet<Pojo> pojo = new DataSet<>(BaseBuilders.enumerated(DataView.OpCode.class)
                .values(DataView.OpCode.values())
                .weights(1, 1, 1)
                .build());
        final Flux<DataSet<Pojo>.Operation> call1 = pojo.buildOperationFeed(true);
        final Flux<DataSet<Pojo>.Operation> call2 = pojo.buildOperationFeed(true);
    }

    @Test
    public void buildOperationFeedFromEmptyDataSet() throws Exception {
        DataSet pojo = new DataSet(BaseBuilders.enumerated(DataView.OpCode.class)
                .values(DataView.OpCode.values())
                .weights(1, 1, 1)
                .build());
        final Flux<DataSet.Operation> flux = pojo.buildOperationFeed(true);

        StepVerifier.create(flux.take(10) .map(op -> Tuples.of(op.getEntry().getElementIndex(), op.getEntry().getElementVersion())))
                .expectNext(
                        Tuples.of(0, (byte) 1),
                        Tuples.of(0, (byte) 2),
                        Tuples.of(1, (byte) 1),
                        Tuples.of(2, (byte) 1),
                        Tuples.of(1, (byte) 2),
                        Tuples.of(3, (byte) 1),
                        Tuples.of(0, (byte) -2),
                        Tuples.of(4, (byte) 1),
                        Tuples.of(4, (byte) 2),
                        Tuples.of(4, (byte) 3))
                .verifyComplete();
    }


    @Test
    public void selectOneOnEmptyDataSet() throws Exception {
        DataSet<Pojo> pojo = new DataSet<>(BaseBuilders.enumerated(DataView.OpCode.class)
                .values(DataView.OpCode.values())
                .weights(1, 4, 5)
                .build());

        Assert.assertEquals(0, pojo.getSize());

        Assert.assertNull(pojo.selectOne(new RandomSequence(1L)));

    }


    @Test
    public void buildOperationFeedFromEmptyDataSetWithLowCreateProb() throws Exception {
        DataSet<Pojo> pojo = new DataSet<>(BaseBuilders.enumerated(DataView.OpCode.class)
                .values(DataView.OpCode.values())
                .weights(1, 4, 5)
                .build());
        final Flux<DataSet<Pojo>.Operation> flux = pojo.buildOperationFeed(true);

        StepVerifier.create(flux.take(10).map(op -> Tuples.of(op.getEntry().getElementIndex(), op.getEntry().getElementVersion())))
                .expectNext(
                        Tuples.of(0, (byte)1),
                        Tuples.of(0, (byte)-1),
                        Tuples.of(1, (byte)1),
                        Tuples.of(2, (byte)1),
                        Tuples.of(2, (byte)2),
                        Tuples.of(2, (byte)3),
                        Tuples.of(1, (byte)-1),
                        Tuples.of(2, (byte)4),
                        Tuples.of(2, (byte)5),
                        Tuples.of(2, (byte)6))
                .verifyComplete();
    }

    @Test
    public void traverseDataSet() throws Exception {
        final Generator generator = BaseBuilders.enumerated(DataView.OpCode.class)
                .values(DataView.OpCode.values())
                .weights(1, 1, 1)
                .build();
        final Generator<Integer> updateGenerator = new Generator<Integer>(Integer.TYPE) {
            @Override
            public Integer generate(RandomSequence register) {
                return 1;
            }
        };
        DataSet<Integer> pojo = new DataSet<>(generator, new byte[]{1, -3, 2}, 2, updateGenerator, (previousValue, randomSequence) -> (UpdateOf<Integer>) integer -> integer + 1, null, null, Schedulers.single(), 0);




        final Flux<Entry<Integer>> flux = pojo.traverseDataSet(true);

        StepVerifier.create(flux.map(op -> Tuples.of(op.getElementIndex(), op.getElementVersion())))
                .expectNext(Tuples.of(0, (byte)1),
                        Tuples.of(1, (byte)-3),
                        Tuples.of(2, (byte)2))
                .verifyComplete();

        final Flux<Entry<Integer>> fluxNoDeleted = pojo.traverseDataSet(false);

        StepVerifier.create(fluxNoDeleted.map(op -> Tuples.of(op.getElementIndex(), op.getElementVersion())))
                .expectNext(Tuples.of(0, (byte)1),
                        Tuples.of(2, (byte)2))
                .verifyComplete();

        final Iterator<Entry<Integer>> entries = pojo.traverseDataSet(true).toIterable().iterator();

        Entry<Integer> entry = entries.next();
        StepVerifier.create(entry.allVersions())
                .expectNext(1)
                .expectComplete();
        StepVerifier.create(entry.lastVersion())
                .expectNext(1)
                .expectComplete();

        entry = entries.next();
        StepVerifier.create(entry.allVersions())
                .expectNext(1)
                .expectNext(2)
                .expectNext(3)
                .expectComplete();
        StepVerifier.create(entry.lastVersion())
                .expectComplete();

        entry = entries.next();
        StepVerifier.create(entry.allVersions())
                .expectNext(1)
                .expectNext(2)
                .expectComplete();
        StepVerifier.create(entry.lastVersion())
                .expectNext(2)
                .expectComplete();
    }


    public static class Pojo2 {
        private long id;
        private int version;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }
    }

    @Test
    public void buildOperationFeedUpgradeVersionVector() throws Exception {
        final Generator opGen = BaseBuilders.enumerated(DataView.OpCode.class)
                .values(DataView.OpCode.values())
                .weights(1, 1, 1)
                .build();

        final Generator<Pojo2> build = BaseBuilders.aBean(Pojo2.class)
                .injectIdIn("id")
                .with("version", new Generator<Integer>(Integer.class) {
                    @Override
                    public Integer generate(RandomSequence register) {
                        return 1;
                    }
                })
                .build();

        final UpdateGenerator<Pojo2> updateGenerator = new UpdateGenerator<Pojo2>() {
            @Override
            public UpdateOf<Pojo2> generate(Pojo2 previousValue, RandomSequence randomSequence) {
                return new UpdateOf<Pojo2>() {
                    @Override
                    public Pojo2 apply(Pojo2 pojo2) {
                        pojo2.version++; // just increment the version number at each update
                        return pojo2;
                    }
                };
            }
        };
        DataSet<Pojo2> pojo2DataSet = new DataSet<Pojo2>(
                opGen,
                new byte[]{1, 1, 1, 1},
                3,
                build,
                updateGenerator,
                null,
                null,
                Schedulers.immediate(),
                0);

        // check all entries
        for (int i = 0; i < 4; i++) {
            final Entry<Pojo2> entry = pojo2DataSet.get(i);
            Assert.assertEquals(i, entry.getElementIndex());
            Assert.assertEquals(1, entry.getElementVersion());
            Assert.assertEquals(1, entry.allVersions().count().block().intValue());
            Assert.assertEquals(DataView.OpCode.CREATE, entry.getLastOperation());
            Assert.assertEquals(true, entry.isLive());
            Assert.assertEquals(false, entry.isDeleted());
            final Pojo2 lastVersion = entry.lastVersion().block();
            Assert.assertEquals(i, lastVersion.id);
            Assert.assertEquals(1, lastVersion.version);

        }

        final Iterator<DataSet<Pojo2>.Operation> iterator = pojo2DataSet.buildOperationFeed(false).toIterable(1).iterator();


        {
            DataSet<Pojo2>.Operation op = iterator.next();

            // element 2 is deleted
            {
                Assert.assertEquals(0, op.getSequenceId());
                final Entry<Pojo2> entry = op.getEntry();
                Assert.assertEquals(2, entry.getElementIndex());
                Assert.assertEquals(-1, entry.getElementVersion());
                Assert.assertEquals(1, entry.allVersions().count().block().intValue());
                Assert.assertEquals(DataView.OpCode.DELETE, entry.getLastOperation());
                Assert.assertEquals(false, entry.isLive());
                Assert.assertEquals(true, entry.isDeleted());
                final Pojo2 lastVersion = entry.lastVersion().block();
                Assert.assertEquals(2, lastVersion.id);
                Assert.assertEquals(1, lastVersion.version);
            }

            // before we ack, no change when we do get or random select it
            {
                final Entry<Pojo2> entry = pojo2DataSet.get(2);
                Assert.assertEquals(2, entry.getElementIndex());
                Assert.assertEquals(1, entry.getElementVersion());
                Assert.assertEquals(1, entry.allVersions().count().block().intValue());
                Assert.assertEquals(DataView.OpCode.CREATE, entry.getLastOperation());
                Assert.assertEquals(true, entry.isLive());
                Assert.assertEquals(false, entry.isDeleted());
                final Pojo2 lastVersion = entry.lastVersion().block();
                Assert.assertEquals(2, lastVersion.id);
                Assert.assertEquals(1, lastVersion.version);
            }

            op.ack();

            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);

            // after we ack, no change when we do get or random select it
            {
                final Entry<Pojo2> entry = pojo2DataSet.get(2);
                Assert.assertEquals(2, entry.getElementIndex());
                Assert.assertEquals(-1, entry.getElementVersion());
                Assert.assertEquals(1, entry.allVersions().count().block().intValue());
                Assert.assertEquals(DataView.OpCode.DELETE, entry.getLastOperation());
                Assert.assertEquals(false, entry.isLive());
                Assert.assertEquals(true, entry.isDeleted());
                final Pojo2 lastVersion = entry.lastVersion().block();
                Assert.assertEquals(2, lastVersion.id);
                Assert.assertEquals(1, lastVersion.version);
            }

            op = iterator.next();

            // element 0 is updated
            {
                Assert.assertEquals(1, op.getSequenceId());
                final Entry<Pojo2> entry = op.getEntry();
                Assert.assertEquals(0, entry.getElementIndex());
                Assert.assertEquals(2, entry.getElementVersion());
                Assert.assertEquals(2, entry.allVersions().count().block().intValue());
                Assert.assertEquals(DataView.OpCode.UPDATE, entry.getLastOperation());
                Assert.assertEquals(true, entry.isLive());
                Assert.assertEquals(false, entry.isDeleted());
                final Pojo2 lastVersion = entry.lastVersion().block();
                Assert.assertEquals(0, lastVersion.id);
                Assert.assertEquals(2, lastVersion.version);
            }

            // before we ack, no change when we do get or random select it
            {
                final Entry<Pojo2> entry = pojo2DataSet.get(0);
                Assert.assertEquals(0, entry.getElementIndex());
                Assert.assertEquals(1, entry.getElementVersion());
                Assert.assertEquals(1, entry.allVersions().count().block().intValue());
                Assert.assertEquals(DataView.OpCode.CREATE, entry.getLastOperation());
                Assert.assertEquals(true, entry.isLive());
                Assert.assertEquals(false, entry.isDeleted());
                final Pojo2 lastVersion = entry.lastVersion().block();
                Assert.assertEquals(0, lastVersion.id);
                Assert.assertEquals(1, lastVersion.version);
            }

            op.ack();

            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);

            // after we ack, no change when we do get or random select it
            {
                final Entry<Pojo2> entry = pojo2DataSet.get(0);
                Assert.assertEquals(0, entry.getElementIndex());
                Assert.assertEquals(2, entry.getElementVersion());
                Assert.assertEquals(2, entry.allVersions().count().block().intValue());
                Assert.assertEquals(DataView.OpCode.UPDATE, entry.getLastOperation());
                Assert.assertEquals(true, entry.isLive());
                Assert.assertEquals(false, entry.isDeleted());
                final Pojo2 lastVersion = entry.lastVersion().block();
                Assert.assertEquals(0, lastVersion.id);
                Assert.assertEquals(2, lastVersion.version);
            }
        }


    }


}