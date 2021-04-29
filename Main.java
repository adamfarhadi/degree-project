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

    private static int getGaussian(int min, int max, int stdDev) {
        while (true) {
            int value = (int) Math.round(ThreadLocalRandom.current().nextGaussian() * stdDev);
            if (value >= min && value <= max) {
                return value;
            }
        }
    }

    private static void runUnrolledListTest(int N, int range, int numOps, int[] numThreads, double[][] ratios,
            int numRuns, int[] list1, int[] list2) {
        System.out.println("\nTesting UnrolledList with N = " + N + ", range = " + range + ", numOps = " + numOps
                + ", numRuns = " + numRuns);
        System.out.println("---");

        for (int ratioSet = 0; ratioSet < ratios.length; ratioSet++) {
            if (ratioSet > 0)
                System.out.println();
            System.out.println(ratios[ratioSet][0] + " Adds, " + ratios[ratioSet][1] + " Removes, "
                    + ratios[ratioSet][2] + " Contains");
            for (int numThread : numThreads) {
                long[] result = new long[numRuns];
                for (int i = 0; i < numRuns; i++) {
                    UnrolledListTestRunner test = new UnrolledListTestRunner(numThread, list1, N, range);
                    result[i] = test.runTest(numOps, ratios[ratioSet][0], ratios[ratioSet][1], ratios[ratioSet][2]);
                }
                System.out.println("Average time for " + numThread + " threads, uniform = "
                        + Arrays.stream(result).sum() / result.length);

                result = new long[numRuns];
                for (int i = 0; i < numRuns; i++) {
                    UnrolledListTestRunner test = new UnrolledListTestRunner(numThread, list2, N, range);
                    result[i] = test.runTest(numOps, ratios[ratioSet][0], ratios[ratioSet][1], ratios[ratioSet][2]);
                }
                System.out.println("Average time for " + numThread + " threads, gaussian = "
                        + Arrays.stream(result).sum() / result.length);
            }
        }

        System.out.println("---");
        System.out.println("Done Testing UnrolledList");
    }

    private static void runConcurrentSkipListTest(int N, int range, int numOps, int[] numThreads, double[][] ratios,
            int numRuns, int[] list1, int[] list2) {
        System.out.println("\nTesting ConcurrentSkipListSet with N = " + N + ", range = " + range + ", numOps = " + numOps
                + ", numRuns = " + numRuns);
        System.out.println("---");

        for (int ratioSet = 0; ratioSet < ratios.length; ratioSet++) {
            if (ratioSet > 0)
                System.out.println();
            System.out.println(ratios[ratioSet][0] + " Adds, " + ratios[ratioSet][1] + " Removes, "
                    + ratios[ratioSet][2] + " Contains");
            for (int numThread : numThreads) {
                long[] result = new long[numRuns];
                for (int i = 0; i < numRuns; i++) {
                    ConcurrentSkipListTestRunner test = new ConcurrentSkipListTestRunner(numThread, list1, N, range);
                    result[i] = test.runTest(numOps, ratios[ratioSet][0], ratios[ratioSet][1], ratios[ratioSet][2]);
                }
                System.out.println("Average time for " + numThread + " threads, uniform = "
                        + Arrays.stream(result).sum() / result.length);

                result = new long[numRuns];
                for (int i = 0; i < numRuns; i++) {
                    ConcurrentSkipListTestRunner test = new ConcurrentSkipListTestRunner(numThread, list2, N, range);
                    result[i] = test.runTest(numOps, ratios[ratioSet][0], ratios[ratioSet][1], ratios[ratioSet][2]);
                }
                System.out.println("Average time for " + numThread + " threads, gaussian = "
                        + Arrays.stream(result).sum() / result.length);
            }
        }

        System.out.println("---");
        System.out.println("Done Testing ConcurrentSkipListSet");
    }

    private static void test() {
        final int N = (int) Math.pow(10, 4);
        final int range = (int) Math.pow(10, 5);
        final int numOps = (int) Math.pow(10, 4);
        final int numRuns = 10;
        final int[] list1 = new int[N];
        final int[] list2 = new int[N];
        final int[] numThreads = new int[] { 4 };
        final double[][] ratios = new double[][] { { 0.1, 0.1, 0.8 } };

        System.out.println("Available CPU Cores: " + Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < N; i++) {
            list1[i] = getUniform(-range / 2, range / 2 + 1);
            list2[i] = getGaussian(-range / 2, range / 2 + 1, range / 8);
        }

        System.out.println("\nList 1: Data sampled uniformly at random:");
        getMeanAndStdDev(list1);

        System.out.println("\nList 2: Data sampled using normal distribution:");
        getMeanAndStdDev(list2);

        runUnrolledListTest(N, range, numOps, numThreads, ratios, numRuns, list1, list2);
        runConcurrentSkipListTest(N, range, numOps, numThreads, ratios, numRuns, list1, list2);
    }

    public static void main(String[] args) {
        test();
    }
}
