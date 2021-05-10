import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    private static void sleepBeforeEachRun() {
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runTest(String testType, int numInitialElements, int range, int[] numThreads, int[][] ratios,
            int numRuns, int[] initialList, Integer unrolled_K) {

        if(unrolled_K != null) {
            System.out.println("\nTesting " + testType + " (K=" + unrolled_K + ") with N = " + numInitialElements + ", range = " + range
            + ", numRuns = " + numRuns);
        } else {
            System.out.println("\nTesting " + testType + " with N = " + numInitialElements + ", range = " + range
                + ", numRuns = " + numRuns);
        }
        System.out.println("---");

        for (int ratioSet = 0; ratioSet < ratios.length; ratioSet++) {
            if (ratioSet > 0)
                System.out.println();
            System.out.println(ratios[ratioSet][0] + "% Adds, " + ratios[ratioSet][1] + "% Removes, "
                    + ratios[ratioSet][2] + "% Contains");
            for (int numThread : numThreads) {
                ArrayList<TestResults> testResults = new ArrayList<TestResults>(numRuns);
                for (int i = 0; i < numRuns; i++) {
                    sleepBeforeEachRun();
                    TestRunner test = new TestRunner(testType, numThread, initialList, numInitialElements, range, unrolled_K);
                    testResults.add(test.runTest(ratios[ratioSet][0], ratios[ratioSet][1], ratios[ratioSet][2]));
                }
                long totalTime = 0;
                long totalInitialSize = 0;
                long totalFinalSize = 0;
                long totalCompletedOps = 0;
                for (int i = 0; i < testResults.size(); i++) {
                    totalTime += testResults.get(i).time;
                    totalInitialSize += testResults.get(i).initialSize;
                    totalFinalSize += testResults.get(i).finalSize;
                    totalCompletedOps += testResults.get(i).numCompletedOps;
                }
                long avgTime = totalTime / numRuns;
                long avgThroughput = (long) (totalCompletedOps * Math.pow(10, 6) / totalTime); // ops/ms
                System.out.println("[" + numThread + " threads]: avgTime= " + avgTime + ", avgThroughput="
                        + avgThroughput + ", averageFinalSize=" + totalFinalSize / testResults.size());
            }
        }

        System.out.println("---");
        System.out.println("Done Testing " + testType);
    }

    private static void testOnServer() {
        final int N = (int) (5 * Math.pow(10, 3));
        final int range = (int) Math.pow(10, 4);
        final int numRuns = 5;
        final int[] initialList;
        final int[] numThreads = new int[] { 2, 8, 16, 28 };
        final int[][] ratios = new int[][] { { 50, 50, 0 }, { 25, 25, 50 }, { 5, 5, 90 } };

        System.out.println("Available CPU Cores: " + Runtime.getRuntime().availableProcessors());

        initialList = ThreadLocalRandom.current().ints(-range / 2, range / 2).distinct().limit(N).toArray();

        runTest("UnrolledList", N, range, numThreads, ratios, numRuns, initialList, 8);
        runTest("VersionedList", N, range, numThreads, ratios, numRuns, initialList, null);
        runTest("LockFreeList", N, range, numThreads, ratios, numRuns, initialList, null);
    }

    private static void testLocally() {
        final int numInitialElements = (int) (5 * Math.pow(10, 3));
        final int range = (int) Math.pow(10, 4);
        final int numRuns = 5;
        final int[] initialList;
        final int[] numThreads = new int[] { 1, 2, 4, 8 };
        final int[][] ratios = new int[][] { { 50, 50, 0 }, { 25, 25, 50 }, { 5, 5, 90 } };

        System.out.println("Available CPU Cores: " + Runtime.getRuntime().availableProcessors());

        initialList = ThreadLocalRandom.current().ints(-range / 2, range / 2).distinct().limit(numInitialElements)
                .toArray();

        runTest("UnrolledList", numInitialElements, range, numThreads, ratios, numRuns, initialList, 8);
        runTest("VersionedList", numInitialElements, range, numThreads, ratios, numRuns, initialList, null);
        runTest("LockFreeList", numInitialElements, range, numThreads, ratios, numRuns, initialList, null);
    }

    public static void main(String[] args) {
        testLocally();
        // testOnServer();
    }
}
