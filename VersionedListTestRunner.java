import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class VersionedListTestRunner {

    VersionedList list;
    int N;
    int range;
    int numThreads;
    int initialSize;

    public VersionedListTestRunner(int numThreads, int[] inputList, int N, int range) {
        list = new VersionedList();
        for (int i = 0; i < inputList.length; i++) {
            list.add(inputList[i]);
        }
        this.initialSize = list.size();
        this.numThreads = numThreads;
        this.N = N;
        this.range = range;
    }

    class RunOperations implements Runnable {
        private Integer[] operations;

        public RunOperations(Integer[] operations) {
            this.operations = operations;
        }

        @Override
        public void run() {
            testThread(operations);
        }
    }

    public void testThread(Integer[] operations) {
        int l = operations.length;
        for (int i = 0; i < l; i++) {
            if (operations[i] == 0) {
                list.add(ThreadLocalRandom.current().nextInt(-range / 2, range / 2 + 1));
            } else if (operations[i] == 1) {
                list.remove(ThreadLocalRandom.current().nextInt(-range / 2, range / 2 + 1));
            } else if (operations[i] == 2) {
                list.contains(ThreadLocalRandom.current().nextInt(-range / 2, range / 2 + 1));
            }
        }
    }

    public Integer[] generateOperations(int numOps, int numAdds, int numRemoves, int numContains) {
        Integer[] operationsList = new Integer[numOps];
        for (int i = 0; i < numOps; i++) {
            if (i < numAdds)
                operationsList[i] = 0;
            else if (i < numAdds + numRemoves)
                operationsList[i] = 1;
            else
                operationsList[i] = 2;
        }

        List<Integer> list = Arrays.asList(operationsList);
        Collections.shuffle(list);
        operationsList = list.toArray(operationsList);
        return operationsList;
    }

    public TestResults runTest(int numOperations, double ratioAdds, double ratioRemoves, double ratioContains) {

        Thread[] threads = new Thread[numThreads];
        int numAddsPerThread = (int) (numOperations * ratioAdds / numThreads);
        int numRemovesPerThread = (int) (numOperations * ratioRemoves / numThreads);
        int numContainsPerThread = (int) (numOperations * ratioContains / numThreads);
        Integer[][] operationsList = new Integer[numThreads][];

        for (int i = 0; i < numThreads; i++) {
            operationsList[i] = generateOperations(numAddsPerThread + numRemovesPerThread + numContainsPerThread,
                    numAddsPerThread, numRemovesPerThread, numContainsPerThread);
        }

        // System.out.println("Initial List Size: " + list.size());

        long time = System.nanoTime();

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(new RunOperations(operationsList[i]));
            threads[i].start();
        }

        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        long total_time = System.nanoTime() - time;

        // System.out.println("Final List Size: " + list.size());

        return new TestResults(total_time, initialSize, list.size());
    }
}
