import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    private static void sleepBeforeEachRun(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runTest(String testType, int numInitialElements, int range, int[] numThreads,
            int timeToRunThreads, int timeToSleep, int[][] ratios, int numRuns, int[] initialList, Integer unrolled_K) {

        if (unrolled_K != null) {
            System.out.println("\nTesting " + testType + " (K=" + unrolled_K + ") with numInitialElements = "
                    + numInitialElements + ", range = " + range + ", numRuns = " + numRuns);
        } else {
            System.out.println("\nTesting " + testType + " with numInitialElements = " + numInitialElements
                    + ", range = " + range + ", numRuns = " + numRuns);
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
                    sleepBeforeEachRun(timeToSleep);
                    TestRunner test = new TestRunner(testType, numThread, timeToRunThreads, initialList, range,
                            unrolled_K);
                    testResults.add(test.runTest(ratios[ratioSet][0], ratios[ratioSet][1], ratios[ratioSet][2]));
                }
                long totalTime = 0;
                long totalFinalSize = 0;
                long totalCompletedOps = 0;
                for (int i = 0; i < testResults.size(); i++) {
                    totalTime += testResults.get(i).time;
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

    private static void runKTest(int numInitialElements, int range, int numThread, int timeToRunThreads,
            int timeToSleep, int[] ratios, int numRuns, int[] initialList, ArrayList<Integer> K_list) {

        System.out.println("Running K_Test for UnrolledList with numInitialElements = " + numInitialElements
                + ", range = " + range + ", numRuns = " + numRuns + ", ratios=[" + ratios[0] + "," + ratios[1] + ","
                + ratios[2] + "]");

        System.out.println("---");

        for (int K : K_list) {
            ArrayList<TestResults> testResults = new ArrayList<TestResults>(numRuns);
            for (int i = 0; i < numRuns; i++) {
                sleepBeforeEachRun(timeToSleep);
                TestRunner test = new TestRunner("UnrolledList", numThread, timeToRunThreads, initialList, range, K);
                testResults.add(test.runTest(ratios[0], ratios[1], ratios[2]));
            }
            long totalTime = 0;
            long totalFinalSize = 0;
            long totalCompletedOps = 0;
            for (int i = 0; i < testResults.size(); i++) {
                totalTime += testResults.get(i).time;
                totalFinalSize += testResults.get(i).finalSize;
                totalCompletedOps += testResults.get(i).numCompletedOps;
            }
            long avgTime = totalTime / numRuns;
            long avgThroughput = (long) (totalCompletedOps * Math.pow(10, 6) / totalTime); // ops/ms
            System.out.println("K= " + K + ", avgThroughput=" + avgThroughput + ", avgTime=" + avgTime
                    + ", averageFinalSize=" + totalFinalSize / testResults.size());
        }

        System.out.println("---");
        System.out.println("Done Running K_Test");
    }

    private static void runUnrolledTest() {
        final int range = 2*((int) Math.pow(10, 2));
        final int numInitialElements = range / 2;
        final int numRuns = 3;
        final int[] numThreads = new int[] { 2, 4 };
        final int[][] ratios = new int[][] { { 50, 50, 0 }, { 10, 10, 80 }, { 0, 0, 100 } };
        final int timeToRunThreads = 2000;
        final int timeToSleep = 100;
        final int K = 14;

        final int[] initialList = ThreadLocalRandom.current().ints(0, range + 1).distinct().limit(numInitialElements)
                .toArray();

        System.out.println("Available CPU Cores: " + Runtime.getRuntime().availableProcessors());

        runTest("UnrolledList", numInitialElements, range, numThreads, timeToRunThreads, timeToSleep, ratios, numRuns,
                initialList, K);
        runTest("VersionedList", numInitialElements, range, numThreads, timeToRunThreads, timeToSleep, ratios, numRuns,
                initialList, null);
        runTest("LazyList", numInitialElements, range, numThreads, timeToRunThreads, timeToSleep, ratios, numRuns,
                initialList, null);
        runTest("LockFreeList", numInitialElements, range, numThreads, timeToRunThreads, timeToSleep, ratios, numRuns,
                initialList, null);

        // runTest("Skiplist", numInitialElements, range, numThreads, timeToRunThreads,
        // timeToSleep, ratios, numRuns,
        // initialList, null);
        // runTest("Hashset", numInitialElements, range, numThreads, timeToRunThreads,
        // timeToSleep, ratios, numRuns,
        // initialList, null);
    }

    private static void K_Test() {
        final int range = 5*((int) Math.pow(10, 3));
        final int numInitialElements = range / 2;
        final int numRuns = 5;
        final int numThreads = 8;
        final int[] ratios = new int[] { 10, 10, 80 };
        final int timeToRunThreads = 1000;
        final int timeToSleep = 100;

        final int[] initialList = ThreadLocalRandom.current().ints(0, range + 1).distinct().limit(numInitialElements)
                .toArray();

        ArrayList<Integer> K_list = new ArrayList<Integer>();

        final int K_interval = 8;
        final int K_max = 32 * K_interval;

        for (int i = K_interval; i <= K_max; i += K_interval) {
            K_list.add(i);
        }

        System.out.println("Available CPU Cores: " + Runtime.getRuntime().availableProcessors());

        runKTest(numInitialElements, range, numThreads, timeToRunThreads, timeToSleep, ratios, numRuns, initialList,
                K_list);
    }

    public static void main(String[] args) {
        runUnrolledTest();
        // K_Test();
    }
}
