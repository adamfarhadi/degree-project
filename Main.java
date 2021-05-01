import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    private static void getMeanAndStdDev(int[] list) {
        double listMean = Arrays.stream(list).sum() / list.length;
        double listVariance = 0;

        for (int i = 0; i < list.length; i++) {
            listVariance += Math.pow(list[i] - listMean, 2);
        }

        listVariance /= (list.length - 1);

        System.out.println("Mean: " + listMean);
        System.out.println("Std Dev: " + Math.sqrt(listVariance));
    }

    private static int getUniform(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    private static void sleepBeforeEachRun() {
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runTest(String testType, int N, int range, int numOps, int[] numThreads, double[][] ratios,
            int numRuns, int[] list1) {
        System.out.println("\nTesting " + testType + " with N = " + N + ", range = " + range + ", numOps = " + numOps
                + ", numRuns = " + numRuns);
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
                    testResults
                            .add(test.runTest(numOps, ratios[ratioSet][0], ratios[ratioSet][1], ratios[ratioSet][2]));
                }
                long totalTime = 0;
                long totalInitialSize = 0;
                long totalFinalSize = 0;
                for (int i = 0; i < testResults.size(); i++) {
                    totalTime += testResults.get(i).time;
                    totalInitialSize += testResults.get(i).initialSize;
                    totalFinalSize += testResults.get(i).finalSize;
                }
                long avgTime = totalTime / testResults.size();
                long avgThroughput = (long) (numOps * Math.pow(10, 6) / avgTime); // ops/ms
                System.out.println("[" + numThread + " threads]: avgTime= " + avgTime + ", avgThroughput="
                        + avgThroughput + ", averageInitialSize=" + totalInitialSize / testResults.size()
                        + ", averageFinalSize=" + totalFinalSize / testResults.size());
            }
        }

        System.out.println("---");
        System.out.println("Done Testing " + testType);
    }

    private static void testOnServer() {
        final int N = (int) (5 * Math.pow(10, 2));
        final int range = (int) Math.pow(10, 3);
        final int numOps = (int) Math.pow(10, 6);
        final int numRuns = 5;
        final int[] list1 = new int[N];
        final int[] numThreads = new int[] { 2, 8, 16, 28 };
        final double[][] ratios = new double[][] { { 0.5, 0.5, 0.0 }, { 0.25, 0.25, 0.5 }, { 0.05, 0.05, 0.9 } };

        System.out.println("Available CPU Cores: " + Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < N; i++) {
            list1[i] = getUniform(-range / 2, range / 2 + 1);
        }

        System.out.println("\nList 1: Data sampled uniformly at random:");
        getMeanAndStdDev(list1);

        runTest("UnrolledList", N, range, numOps, numThreads, ratios, numRuns, list1);
        runTest("VersionedList", N, range, numOps, numThreads, ratios, numRuns, list1);
        runTest("LockFreeList", N, range, numOps, numThreads, ratios, numRuns, list1);
    }

    private static void testLocally() {
        final int N = (int) (5 * Math.pow(10, 2));
        final int range = (int) Math.pow(10, 3);
        final int numOps = (int) Math.pow(10, 6);
        final int numRuns = 5;
        final int[] list1 = new int[N];
        final int[] numThreads = new int[] { 2, 6 };
        final double[][] ratios = new double[][] { { 0.5, 0.5, 0.0 }, { 0.25, 0.25, 0.5 }, { 0.05, 0.05, 0.9 } };

        System.out.println("Available CPU Cores: " + Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < N; i++) {
            list1[i] = getUniform(-range / 2, range / 2 + 1);
        }

        System.out.println("\nList 1: Data sampled uniformly at random:");
        getMeanAndStdDev(list1);

        runTest("UnrolledList", N, range, numOps, numThreads, ratios, numRuns, list1);
        runTest("VersionedList", N, range, numOps, numThreads, ratios, numRuns, list1);
        runTest("LockFreeList", N, range, numOps, numThreads, ratios, numRuns, list1);
    }

    public static void main(String[] args) {
        // testLocally();
        testOnServer();
    }
}
