/**
 * Benchmark of Reed-Solomon encoding.
 *
 * Copyright 2015, Backblaze, Inc.  All rights reserved.
 */

package com.backblaze.erasure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.lang.Math;

/**
 * Benchmark of Reed-Solomon encoding.
 *
 * Counts the number of bytes of input data that can be processed per
 * second.
 */
public class ReedSolomonBenchmarkLRC {

    /* Data/Parity Shard Counts. */
    private static int GLOBAL_DATA_COUNT = 6;
    private static int LOCAL_GROUPS = 2;
    private static int GLOBAL_PARITY_COUNT = 2;
    private static int LOCAL_PARITY_COUNT = 2;

    /* Chunk Size and Stripe Sizes. */
    private static int CHUNK_SIZE = 128 * 1024;
    private static int LOCAL_DATA_COUNT = (int)Math.ceil(GLOBAL_DATA_COUNT / LOCAL_GROUPS);
    private static int LOCAL_STRIPE_SIZE = CHUNK_SIZE * LOCAL_DATA_COUNT;
    private static int GLOBAL_STRIPE_SIZE = CHUNK_SIZE * GLOBAL_DATA_COUNT;

    /* Data File Size. */
    private static final int FILE_SIZE = 100 * 1024 * 1024;

    /* Encoding Buffer Sizes. */
    private static int GLOBAL_ENCODING_BUFFER_SIZE = FILE_SIZE / GLOBAL_STRIPE_SIZE;
    private static int LOCAL_ENCODING_BUFFER_SIZE = GLOBAL_ENCODING_BUFFER_SIZE * LOCAL_GROUPS;
    
    private static final long MEASUREMENT_DURATION = 2 * 1000;

    private static final Random random = new Random();

    private int nextBuffer = 0;

    public static void main(String [] args) {

        /* Parse args and set global variables */
        if (args.length != 0) {
            GLOBAL_DATA_COUNT = Integer.parseInt(args[0]);
            LOCAL_GROUPS = Integer.parseInt(args[1]);
            GLOBAL_PARITY_COUNT = Integer.parseInt(args[2]);
            LOCAL_PARITY_COUNT = Integer.parseInt(args[3]);
            CHUNK_SIZE = Integer.parseInt(args[4]);
            LOCAL_STRIPE_SIZE = CHUNK_SIZE * LOCAL_DATA_COUNT;
            GLOBAL_STRIPE_SIZE = LOCAL_STRIPE_SIZE * LOCAL_GROUPS;
            GLOBAL_ENCODING_BUFFER_SIZE = FILE_SIZE / GLOBAL_STRIPE_SIZE;
            LOCAL_ENCODING_BUFFER_SIZE = GLOBAL_ENCODING_BUFFER_SIZE * LOCAL_GROUPS;
        }
        (new ReedSolomonBenchmarkLRC()).run();
    }

    public void run() {
        System.out.println("preparing...");
        final BufferSet [] globalBufferSets = new BufferSet [GLOBAL_ENCODING_BUFFER_SIZE];
        for (int iBufferSet = 0; iBufferSet < GLOBAL_ENCODING_BUFFER_SIZE; iBufferSet++) {
            globalBufferSets[iBufferSet] = new BufferSet(0);
        }
        final BufferSet [] localBufferSets = new BufferSet [LOCAL_ENCODING_BUFFER_SIZE];
        for (int iBufferSet = 0; iBufferSet < LOCAL_ENCODING_BUFFER_SIZE; iBufferSet++) {
            localBufferSets[iBufferSet] = new BufferSet(1);
        }
        final byte [] tempBuffer = new byte [CHUNK_SIZE];

        List<String> summaryLines = new ArrayList<String>();
        StringBuilder csv = new StringBuilder();
        csv.append("Outer,Middle,Inner,Multiply,Encode,Check\n");

        CodingLoop[] DesiredLoop;
        /* Only benchmark with InputOutputByteTableCodingLoop */
        DesiredLoop = new CodingLoop[]{CodingLoop.ALL_CODING_LOOPS[7]};
        /* Loop through all coding loops */
        // DesiredLoop = CodingLoop.ALL_CODING_LOOPS;
        for (CodingLoop codingLoop : DesiredLoop) {
            Measurement encodeAverage = new Measurement();
            {
                final String testName = codingLoop.getClass().getSimpleName() + " encodeParity";
                System.out.println("\nTEST: " + testName);
                ReedSolomon globalCodec = new ReedSolomon(GLOBAL_DATA_COUNT, GLOBAL_PARITY_COUNT, codingLoop);
                ReedSolomon localCodec = new ReedSolomon(LOCAL_DATA_COUNT, LOCAL_PARITY_COUNT, codingLoop);
                System.out.println("    warm up...");
                doOneEncodeMeasurement(localCodec, globalCodec, localBufferSets, globalBufferSets);
                doOneEncodeMeasurement(localCodec, globalCodec, localBufferSets, globalBufferSets);
                System.out.println("    testing...");
                for (int iMeasurement = 0; iMeasurement < 5; iMeasurement++) {
                    encodeAverage.add(doOneEncodeMeasurement(localCodec, globalCodec, localBufferSets, globalBufferSets));
                }
                System.out.println(String.format("\nAVERAGE: %s", encodeAverage));
                summaryLines.add(String.format("    %-45s %s", testName, encodeAverage));
            }
            // The encoding test should have filled all of the buffers with
            // correct parity, so we can benchmark parity checking.
            // Measurement checkAverage = new Measurement();
            // {
            //     final String testName = codingLoop.getClass().getSimpleName() + " isParityCorrect";
            //     System.out.println("\nTEST: " + testName);
            //     ReedSolomon codec = new ReedSolomon(LOCAL_GLOBAL_DATA_COUNT, LOCAL_GROUPS, codingLoop);
            //     System.out.println("    warm up...");
            //     doOneEncodeMeasurement(codec, bufferSets);
            //     doOneEncodeMeasurement(codec, bufferSets);
            //     System.out.println("    testing...");
            //     for (int iMeasurement = 0; iMeasurement < 10; iMeasurement++) {
            //         checkAverage.add(doOneCheckMeasurement(codec, bufferSets, tempBuffer));
            //     }
            //     System.out.println(String.format("\nAVERAGE: %s", checkAverage));
            //     summaryLines.add(String.format("    %-45s %s", testName, checkAverage));
            // }
            csv.append(codingLoopNameToCsvPrefix(codingLoop.getClass().getSimpleName()));
            csv.append(encodeAverage.getRate());
            csv.append(",");
            // csv.append(checkAverage.getRate());
            csv.append("\n");
        }

        System.out.println("\n");
        System.out.println(csv.toString());

        System.out.println("\nSummary:\n");
        for (String line : summaryLines) {
            System.out.println(line);
        }
    }

    private Measurement doOneEncodeMeasurement(ReedSolomon localCodec, ReedSolomon globalCodec, BufferSet[] localBufferSets, BufferSet[] globalBufferSets) {

        /* Global */ 
        nextBuffer = 0;
        long passesCompleted = 0;
        long bytesEncoded = 0;
        long encodingTime = 0;

        while (encodingTime < MEASUREMENT_DURATION) {
            BufferSet bufferSet = globalBufferSets[nextBuffer];
            nextBuffer = (nextBuffer + 1) % globalBufferSets.length;
            byte[][] shards = bufferSet.buffers;
            long startTime = System.currentTimeMillis();
            globalCodec.encodeParity(shards, 0, LOCAL_STRIPE_SIZE);
            long endTime = System.currentTimeMillis();
            encodingTime += (endTime - startTime);
            bytesEncoded += CHUNK_SIZE * GLOBAL_DATA_COUNT;
            passesCompleted += 1;
        }

        /* Local */
        nextBuffer = 0;
        passesCompleted = 0;
        bytesEncoded = 0;
        encodingTime = 0;

        while (encodingTime < MEASUREMENT_DURATION) {
            BufferSet bufferSet = localBufferSets[nextBuffer];
            nextBuffer = (nextBuffer + 1) % localBufferSets.length;
            byte[][] shards = bufferSet.buffers;
            long startTime = System.currentTimeMillis();
            localCodec.encodeParity(shards, 0, CHUNK_SIZE);
            long endTime = System.currentTimeMillis();
            encodingTime += (endTime - startTime);
            bytesEncoded += CHUNK_SIZE * LOCAL_DATA_COUNT;
            passesCompleted += 1;
        }

        /* Throughput calculation */
        double seconds = ((double)encodingTime) / 1000.0;
        double megabytes = ((double)bytesEncoded) / 1000000.0;
        Measurement result = new Measurement(megabytes, seconds);
        System.out.println(String.format("        %s passes, %s", passesCompleted, result));
        return result;
    }

    private Measurement doOneCheckMeasurement(ReedSolomon codec, BufferSet[] bufferSets, byte [] tempBuffer) {
        long passesCompleted = 0;
        long bytesChecked = 0;
        long checkingTime = 0;
        while (checkingTime < MEASUREMENT_DURATION) {
            BufferSet bufferSet = bufferSets[nextBuffer];
            nextBuffer = (nextBuffer + 1) % bufferSets.length;
            byte[][] shards = bufferSet.buffers;
            long startTime = System.currentTimeMillis();
            if (!codec.isParityCorrect(shards, 0, CHUNK_SIZE, tempBuffer)) {
                // if the parity is not correct, it will throw off the
                // benchmarking because it may return early.
                throw new RuntimeException("parity not correct");
            }
            long endTime = System.currentTimeMillis();
            checkingTime += (endTime - startTime);
            bytesChecked += CHUNK_SIZE * LOCAL_DATA_COUNT;
            passesCompleted += 1;
        }
        double seconds = ((double)checkingTime) / 1000.0;
        double megabytes = ((double)bytesChecked) / 1000000.0;
        Measurement result = new Measurement(megabytes, seconds);
        System.out.println(String.format("        %s passes, %s", passesCompleted, result));
        return result;
    }

    /**
     * Converts a name like "OutputByteInputTableCodingLoop" to
     * "output,byte,input,table,".
     */
    private static String codingLoopNameToCsvPrefix(String className) {
        List<String> names = splitCamelCase(className);
        return
                names.get(0) + "," +
                names.get(1) + "," +
                names.get(2) + "," +
                names.get(3) + ",";
    }

    /**
     * Converts a name like "OutputByteInputTableCodingLoop" to a List of
     * words: { "output", "byte", "input", "table", "coding", "loop" }
     */
    private static List<String> splitCamelCase(String className) {
        String remaining = className;
        List<String> result = new ArrayList<String>();
        while (!remaining.isEmpty()) {
            boolean found = false;
            for (int i = 1; i < remaining.length(); i++) {
                if (Character.isUpperCase(remaining.charAt(i))) {
                    result.add(remaining.substring(0, i));
                    remaining = remaining.substring(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                result.add(remaining);
                remaining = "";
            }
        }
        return result;
    }

    private static class BufferSet {

        public byte [] [] buffers;

        // public byte [] bigBuffer;

        public BufferSet(int layer) {
            int totalCount = 0;
            int byteCount = 0;

            /* Global */ 
            if (layer == 0) {
                totalCount = GLOBAL_DATA_COUNT + GLOBAL_PARITY_COUNT;
                byteCount = LOCAL_STRIPE_SIZE;
            }

            /* Local */
            else if (layer == 1) {
                totalCount = LOCAL_DATA_COUNT + LOCAL_PARITY_COUNT;
                byteCount = CHUNK_SIZE;
            }
            buffers = new byte [totalCount] [byteCount];
            for (int iBuffer = 0; iBuffer < totalCount; iBuffer++) {
                byte [] buffer = buffers[iBuffer];
                for (int iByte = 0; iByte < byteCount; iByte++) {
                    buffer[iByte] = (byte) random.nextInt(256);
                }
            }

            // bigBuffer = new byte [totalCount * byteCount];
            // for (int i = 0; i < totalCount * byteCount; i++) {
            //     bigBuffer[i] = (byte) random.nextInt(256);
            // }
        }
    }

    private static class Measurement {
        private double megabytes;
        private double seconds;

        public Measurement() {
            this.megabytes = 0.0;
            this.seconds = 0.0;
        }

        public Measurement(double megabytes, double seconds) {
            this.megabytes = megabytes;
            this.seconds = seconds;
        }

        public void add(Measurement other) {
            megabytes += other.megabytes;
            seconds += other.seconds;
        }

        public double getRate() {
            return megabytes / seconds;
        }

        @Override
        public String toString() {
            return String.format("%5.1f MB/s", getRate());
        }
    }
}
