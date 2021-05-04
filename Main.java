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

    private static void runTest(String testType, int N, int range, int[] numThreads, double[][] ratios, int numRuns,
            int[] list1) {
        System.out
                .println("\nTesting " + testType + " with N = " + N + ", range = " + range + ", numRuns = " + numRuns);
        System.out.println("---");

        for (int ratioSet = 0; ratioSet < ratios.length; ratioSet++) {
            if (ratioSet > 0)
                System.out.println();
            System.out.println(ratios[ratioSet][0] + " Adds, " + ratios[ratioSet][1] + " Removes, "
                    + ratios[ratioSet][2] + " Contains");
            for (int numThread : numThreads) {
                ArrayList<TestResults> testResults = new ArrayList<TestResults>(numRuns);
                for (int i = 0; i < numRuns; i++) {
                    sleepBeforeEachRun();
                    TestRunner test = new TestRunner(testType, numThread, list1, N, range);
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
        final double[][] ratios = new double[][] { { 0.5, 0.5, 0.0 }, { 0.25, 0.25, 0.5 }, { 0.05, 0.05, 0.9 } };

        System.out.println("Available CPU Cores: " + Runtime.getRuntime().availableProcessors());

        initialList = ThreadLocalRandom.current().ints(-range/2, range/2).distinct().limit(N).toArray();

        runTest("UnrolledList", N, range, numThreads, ratios, numRuns, initialList);
        runTest("VersionedList", N, range, numThreads, ratios, numRuns, initialList);
        runTest("LockFreeList", N, range, numThreads, ratios, numRuns, initialList);
    }

    private static void testLocally() {
        final int N = (int) (5 * Math.pow(10, 3));
        final int range = (int) Math.pow(10, 4);
        final int numRuns = 5;
        final int[] initialList;
        final int[] numThreads = new int[] { 2, 6 };
        final double[][] ratios = new double[][] { { 0.5, 0.5, 0.0 }, { 0.25, 0.25, 0.5 }, { 0.05, 0.05, 0.9 } };

        System.out.println("Available CPU Cores: " + Runtime.getRuntime().availableProcessors());

        initialList = ThreadLocalRandom.current().ints(-range/2, range/2).distinct().limit(N).toArray();

        runTest("UnrolledList", N, range, numThreads, ratios, numRuns, initialList);
        runTest("VersionedList", N, range, numThreads, ratios, numRuns, initialList);
        runTest("LockFreeList", N, range, numThreads, ratios, numRuns, initialList);
    }

    public static void main(String[] args) {
        testLocally();
        // testOnServer();
    }
}
