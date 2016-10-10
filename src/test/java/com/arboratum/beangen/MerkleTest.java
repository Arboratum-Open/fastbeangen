package com.arboratum.beangen;

import com.arboratum.beangen.util.RandomSequence;
import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.linkedin.paldb.api.PalDB;
import com.linkedin.paldb.api.Serializer;
import com.linkedin.paldb.api.StoreWriter;
import lombok.Data;
import lombok.ToString;
import lombok.Value;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.apache.flink.api.common.functions.GroupCombineFunction;
import org.apache.flink.api.common.functions.GroupReduceFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.operators.GroupCombineOperator;
import org.apache.flink.api.java.operators.GroupReduceOperator;
import org.apache.flink.api.java.operators.MapOperator;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.junit.Test;
import org.rocksdb.*;
import org.xerial.snappy.SnappyOutputStream;
import org.xerial.snappy.buffer.BufferAllocatorFactory;
import org.xerial.snappy.buffer.CachedBufferAllocator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by gpicron on 11/08/2016.
 */
public class MerkleTest {

    @Test
    public void testComparison() throws NoSuchAlgorithmException {
        RandomSequence r1 = new RandomSequence(0);

        Stopwatch stopwatch = Stopwatch.createStarted();
        int size = 65536;
        int[] buffer = new int[size * 20];
        int[] buffer2 = new int[size * 20];

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) r1.nextLong();
            buffer2[i] = buffer[i];
        }

        System.out.println("Creating the buffer takes : " + stopwatch.stop());
        stopwatch.reset();
        stopwatch.start();

        for (int i = 0; i < size; i++) {
            final int start = i * 20;
            for (int j = 0; j < 20; j++) {
                final int index = start + j;
                if (buffer[index] != buffer2[index]) {
                    System.out.println("error:" + i);
                    break;
                }
            }
        }
        System.out.println("Comparing the buffer takes : " + stopwatch.stop());

    }

    @Value
    public static final class AlertHash implements Comparable<AlertHash> {
        private final byte[] id = new byte[10];
        private final byte[] hash = new byte[20];

        @Override
        public int compareTo(AlertHash o) {
            for (int i = 0; i < 10; i++) {
                int r = Byte.compare(id[i], o.id[i]);
                if (r != 0) return r;
            }
            return 0;
        }
    }

    @Test
    public void testUpdating() throws NoSuchAlgorithmException {
        RandomSequence r1 = new RandomSequence(0);

        Stopwatch stopwatch = Stopwatch.createStarted();
        int size = 2000;

        AlertHash[] alerts = new AlertHash[size];

        for (int i = 0; i < size; i++) {
            byte[] id = Longs.toByteArray(i);

            AlertHash al = alerts[i] = new AlertHash();
            System.arraycopy(id, 0, al.getId(), 0, 8);
            r1.nextBytes(al.getHash());
        }

        Arrays.sort(alerts);

        System.out.println("Creating the buffer takes : " + stopwatch.stop());
        stopwatch.reset();
        stopwatch.start();


        final Hasher hashFunction = Hashing.sha1().newHasher(size * 20);

        for (AlertHash al : alerts) {
            hashFunction.putBytes(al.getHash());
        }
        System.out.println(hashFunction.hash());

        System.out.println("Hashing the buffer takes : " + stopwatch.stop());

    }


    @Test
    public void testConsistentHash() throws NoSuchAlgorithmException {
        HashFunction f = Hashing.murmur3_32();

        String[] ids = new String[10000000];
        for (int i = 0; i < 10000000; i++) {
            int country = i % 30;
            int id = i / 30;

            ids[i] =
                    Strings.padStart(Integer.toString(country), 4, '0') + ".01-"
                            + Strings.padStart(Integer.toString(id), 13, '0')
                            + "-1234-0000.01";
        }

        Stopwatch stopwatch = Stopwatch.createStarted();

        int buckets = 65536;
        int[] counts = new int[buckets];


        for (int i = 0; i < 1000000; i++) {
            String fakeSchengenId = ids[i];
            final HashCode hashCode = f.hashString(fakeSchengenId, Charsets.UTF_8);
            final int bucket = hashCode.asInt() & 0xFFFF;
            counts[bucket]++;
        }
        System.out.println(Arrays.toString(counts));

        System.out.println("Distributing into buckets takes : " + stopwatch.stop());


    }

    @Test
    public void testRocksDBHash() throws NoSuchAlgorithmException, RocksDBException {
        RandomSequence r1 = new RandomSequence(0);
        RocksDB.loadLibrary();
        Options dbOptions = new Options();
        dbOptions.setCompactionStyle(CompactionStyle.UNIVERSAL);
        RocksDB db = RocksDB.open("test4");

        db.enableFileDeletions(true);
        HashFunction mm32 = Hashing.murmur3_32(123);
        Stopwatch stopwatch = Stopwatch.createStarted();

        final int BATCH = 100000;
        int batchSize = BATCH;
        WriteBatch batch = new WriteBatch(BATCH * 35);
        for (byte i = 0; i < 30; i++) {
            for (long j = 0; j < 400000 * 8; j++) {


                byte[] key = new byte[12];
                key[2] = i;
                System.arraycopy(Ints.toByteArray(i), 2, key, 2, 2);
                System.arraycopy(Longs.toByteArray(j), 0, key, 4, 8);
                final Hasher hasher = mm32.newHasher();
                hasher.putBytes(key, 2, 10);

                final int bucket = hasher.hash().asInt() & 0xFFFF;
                addBucketToKey(key, bucket);
                assert bucket == extractBucket(key);

                byte[] fakeHash = new byte[20];
                r1.nextBytes(fakeHash);

                batch.put(key, fakeHash);

                if (--batchSize == 0) {
                    putBatch(db, batch);
                    batch = new WriteBatch();
                    batchSize = BATCH;
                }
            }
        }
        putBatch(db, batch);
        final FlushOptions flushOptions = new FlushOptions();
        flushOptions.setWaitForFlush(true);
        db.flush(flushOptions);
        db.compactRange();


        System.out.println("Inserting into rocksdb takes : " + stopwatch.stop());
        stopwatch.reset();
        stopwatch.start();

        HashFunction f = Hashing.sha1();
        final ReadOptions readOptions = new ReadOptions();

        final RocksIterator rocksIterator = db.newIterator(readOptions);
        rocksIterator.seekToFirst();
        int currentBucket = -1;
        Hasher bucketHashing = null;
        int count = 0;
        int totalCount = 0;
        while (rocksIterator.isValid()) {
            totalCount++;
            final byte[] key1 = rocksIterator.key();
            //System.out.println(Arrays.toString(key1));
            int actualBucket = extractBucket(key1);

            if (actualBucket != currentBucket) {
                if (bucketHashing != null) {
                    //System.out.print(",count:" + count);
                    //System.out.println(", hash:" + bucketHashing.hash().toString());
                    bucketHashing.hash().toString();
                }

                bucketHashing = f.newHasher();
                count = 0;
                currentBucket = actualBucket;
                //System.out.print("bucket:" + currentBucket);
            }

            bucketHashing.putBytes(rocksIterator.value());
            count++;
            rocksIterator.next();
        }
        //System.out.print(", count:" + count);
        System.out.println(", hash:" + bucketHashing.hash().toString());

        System.out.println("total:" + totalCount);
        rocksIterator.close();
        db.close();

        System.out.println("Computing bucket hashes into rocksdb takes : " + stopwatch.stop());

    }

    private void putBatch(RocksDB db, WriteBatch batch) throws RocksDBException {
        final WriteOptions writeOpts = new WriteOptions();
        writeOpts.setSync(false);
        writeOpts.setDisableWAL(true);
        db.write(writeOpts, batch);
        batch.close();
        writeOpts.close();
    }

    private int extractBucket(byte[] key1) {
        byte[] bucket = new byte[4];
        System.arraycopy(key1, 0, bucket, 2, 2);
        return Ints.fromByteArray(bucket);
    }

    private void addBucketToKey(byte[] key, int bucket) {
        System.arraycopy(Ints.toByteArray(bucket), 2, key, 0, 2);
    }

    @Data
    @ToString(exclude = "packet")
    public static class BucketOfHashAlert {
        private int id;
        private int count;
        private byte[] hash;
        private byte[] packet;
    }
    @Data
    @ToString(exclude = {"packets", "hash" })
    public static class Block {
        private int id;
        private int count;
        private byte[] hash;
        private BucketOfHashAlert[] packets;
    }


    @Test
    public void testFlinkHash() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();

        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(6);

        final int NUM_ALERTS = 96000000;
        final DataSource<Long> longDataSource = env.generateSequence(1, NUM_ALERTS).setParallelism(1);
        final MapOperator<Long, AlertHash> genAlerts = longDataSource.map(value -> {
            RandomSequence r1 = new RandomSequence(value);
            AlertHash al = new AlertHash();
            System.arraycopy(Longs.toByteArray(value), 0, al.getId(), 0, 8);
            r1.nextBytes(al.getHash());
            return al;
        });

        final int NUM_BUCKETS = 65536;

        HashFunction mm3 = Hashing.murmur3_32();
        final MapOperator<AlertHash, Tuple3<byte[], byte[], Integer>> extractBucket = genAlerts
                .map(new MapFunction<AlertHash, Tuple3<byte[], byte[], Integer>>() {
                    @Override
                    public Tuple3<byte[], byte[], Integer> map(AlertHash value) throws Exception {
                        return Tuple3.of(value.getId(), value.getHash(), Math.abs(mm3.hashBytes(value.getId()).asInt() % NUM_BUCKETS));
                    }
                });


        HashFunction sha1 = Hashing.sha1();

        final GroupReduceOperator<Tuple3<byte[], byte[], Integer>, BucketOfHashAlert> packets = extractBucket
                .groupBy(2)
                .sortGroup(0, Order.ASCENDING)
                .reduceGroup(new GroupReduceFunction<Tuple3<byte[],byte[],Integer>, BucketOfHashAlert>() {
                    @Override
                    public void reduce(Iterable<Tuple3<byte[], byte[], Integer>> values, Collector<BucketOfHashAlert> out) throws Exception {
                        final ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput(NUM_ALERTS / NUM_BUCKETS * 30);
                        final Hasher hasher = sha1.newHasher();

                        Iterator<Tuple3<byte[], byte[], Integer>> iterator = values.iterator();
                        Tuple3<byte[], byte[], Integer> b = null;
                        int count = 0;
                        while (iterator.hasNext()) {
                            b = iterator.next();

                            dataOutput.write(b.f0);
                            dataOutput.write(b.f1);
                            hasher.putBytes(b.f1);
                            count++;
                        }

                        BucketOfHashAlert r = new BucketOfHashAlert();
                        r.setId(b.f2);
                        r.setHash(hasher.hash().asBytes());
                        r.setPacket(dataOutput.toByteArray());
                        r.setCount(count);
                        out.collect(r);

                    }
                });


        //packets.output(new BucketOfHashAlertOutputFormat());

        final int sizeInBytes = sha1.bits() / Byte.SIZE;
        final int BLOCK_SIZE = 100;

        final GroupCombineOperator<Tuple3<Integer, Integer, BucketOfHashAlert>, Block> blocks = packets
                .map(new MapFunction<BucketOfHashAlert, Tuple3<Integer, Integer, BucketOfHashAlert>>() {
                    @Override
                    public Tuple3<Integer, Integer, BucketOfHashAlert> map(BucketOfHashAlert value) throws Exception {
                        return Tuple3.of(value.getId() / BLOCK_SIZE, value.getId() % BLOCK_SIZE, value);
                    }
                })
                .groupBy(0)
                //.sortGroup(1, Order.ASCENDING)
                .combineGroup(new GroupCombineFunction<Tuple3<Integer, Integer, BucketOfHashAlert>, Block>() {

                    @Override
                    public void combine(Iterable<Tuple3<Integer, Integer, BucketOfHashAlert>> values, Collector<Block> out) throws Exception {
                        byte[] hashSummary = new byte[sizeInBytes * BLOCK_SIZE];
                        BucketOfHashAlert[] packets = new BucketOfHashAlert[BLOCK_SIZE];
                        Iterator<Tuple3<Integer, Integer, BucketOfHashAlert>> iter = values.iterator();
                        Tuple3<Integer, Integer, BucketOfHashAlert> next = null;
                        int count = 0;
                        while (iter.hasNext()) {
                            count++;
                            next = iter.next();
                            final int i = next.f1;
                            packets[i] = next.f2;
                            System.arraycopy(next.f2.getHash(), 0, hashSummary, i * sizeInBytes, sizeInBytes);
                        }
                        Block b = new Block();
                        b.setId(next.f0);
                        b.setCount(count);
                        b.setHash(hashSummary);
                        b.setPackets(packets);
                        out.collect(b);
                    }
                });

        //blocks.print();
        blocks.output(new BlockOutputFormat2("testFL/palBlock96-"+BLOCK_SIZE));

        env.execute();

        System.out.println("Computing bucket hashes  takes : " + stopwatch.stop());

    }

    private static class BucketOfHashAlertOutputFormat implements OutputFormat<BucketOfHashAlert>, Serializable {
        private static LZ4Factory factory = LZ4Factory.fastestInstance();
        private Path root;

        @Override
        public void configure(Configuration parameters) {

            root = Paths.get("testFL/noComp2");
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void open(int taskNumber, int numTasks) throws IOException {

        }

        @Override
        public void writeRecord(BucketOfHashAlert record) throws IOException {
            String file = Strings.padStart(Integer.toHexString(record.getId()), 4, '0');
            file = file.substring(0,2) + '/' + file.substring(2);
            Path child = root.resolve(file);
            Files.createDirectories(child.getParent());

            final OutputStream outputStream = Files.newOutputStream(child, StandardOpenOption.CREATE);
            outputStream.write(Ints.toByteArray(record.getCount()));

            if (false) {
                final LZ4Compressor lz4Compressor = factory.highCompressor();
                outputStream.write(lz4Compressor.compress(record.getPacket()));
            } else {
                outputStream.write(record.getPacket());
            }
            outputStream.close();
        }

        @Override
        public void close() throws IOException {

        }
    }

    private static final BufferAllocatorFactory BUFFER_ALLOCATOR = CachedBufferAllocator.getBufferAllocatorFactory();

    private static class BlockOutputFormat implements OutputFormat<Block>, Serializable {
        public static final byte[] NULL = Ints.toByteArray(0x00);
        private static LZ4Factory factory = LZ4Factory.fastestInstance();
        private Path root;

        @Override
        public void configure(Configuration parameters) {

            root = Paths.get("testFL/snappycompBlockB96");
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void open(int taskNumber, int numTasks) throws IOException {

        }

        @Override
        public void writeRecord(Block record) throws IOException {
            String file = Strings.padStart(Integer.toHexString(record.getId()), 2, '0');
            Path child = root.resolve(file);
            Files.createDirectories(child.getParent());

            OutputStream outputStream = Files.newOutputStream(child, StandardOpenOption.CREATE);
            outputStream.write(record.getHash());

            if (true) {
//                final LZ4Compressor lz4Compressor = factory.fastCompressor();
//                outputStream = new LZ4BlockOutputStream(outputStream, 64 * 1024, lz4Compressor);
                //outputStream = new GZIPOutputStream(outputStream, 64 * 1024);
                outputStream = new SnappyOutputStream(outputStream, 32 * 1024);
            }
            for (BucketOfHashAlert bb : record.getPackets()) {
                if (bb == null) {
                    outputStream.write(NULL);
                } else {
                    outputStream.write(Ints.toByteArray(bb.getCount()));
                    outputStream.write(bb.getPacket());
                }

            }
            outputStream.close();
        }

        @Override
        public void close() throws IOException {

        }
    }

    @Data
    private static class BlockOutputFormat2 implements OutputFormat<Block>, Serializable {
        private final String rootDir;

        @Override
        public void configure(Configuration parameters) {
            try {
                Files.createDirectories(Paths.get(rootDir));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void open(int taskNumber, int numTasks) throws IOException {

        }

        @Override
        public void writeRecord(Block record) throws IOException {
            String file = Strings.padStart(Integer.toHexString(record.getId()), 2, '0');


            Path child = Paths.get(rootDir).resolve(file);
            Files.createDirectories(child.getParent());

            OutputStream outputStream = Files.newOutputStream(child, StandardOpenOption.CREATE);
            outputStream.write(record.getHash());
            outputStream.close();

            final com.linkedin.paldb.api.Configuration config = new com.linkedin.paldb.api.Configuration();
            config.set(com.linkedin.paldb.api.Configuration.COMPRESSION_ENABLED, "true");
            config.registerSerializer(new Serializer<BucketOfHashAlert>() {

                @Override
                public void write(DataOutput dataOutput, BucketOfHashAlert input) throws IOException {
                    dataOutput.writeInt(input.getCount());
                    dataOutput.write(input.getHash());
                    dataOutput.write(input.getPacket());
                }

                @Override
                public BucketOfHashAlert read(DataInput dataInput) throws IOException {
                    BucketOfHashAlert al = new BucketOfHashAlert();
                    final int count = dataInput.readInt();
                    al.setCount(count);
                    byte[] hash = new byte[20]; dataInput.readFully(hash);
                    byte[] packet = new byte[count*30]; dataInput.readFully(packet);
                    al.setHash(hash);
                    al.setPacket(packet);
                    return al;
                }

                @Override
                public int getWeight(BucketOfHashAlert instance) {
                    return 4 + 20 + 1500 * 30;
                }
            });
            StoreWriter storeWriter = PalDB.createWriter(Paths.get(rootDir).resolve(file + ".db").toFile(), config);


            BucketOfHashAlert[] packets = record.getPackets();
            for (int i = 0, packetsLength = packets.length; i < packetsLength; i++) {
                BucketOfHashAlert bb = packets[i];
                if (bb != null) {
                    storeWriter.put(i, bb);
                }

            }
            storeWriter.close();
        }

        @Override
        public void close() throws IOException {

        }
    }
}