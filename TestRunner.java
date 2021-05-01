import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import abstraction.ConcurrentSet;
import lockfree.LockFreeList;
import unrolled.UnrolledList;
import versioned.VersionedList;

public class TestRunner {

    ConcurrentSet list;
    int N;
    int range;
    int numThreads;
    int initialSize;

    public TestRunner(String testType, int numThreads, int[] inputList, int N, int range) {
        if (testType == "UnrolledList") {
            list = new UnrolledList();
        } else if (testType == "VersionedList") {
            list = new VersionedList();
        } else if (testType == "LockFreeList") {
            list = new LockFreeList();
        } else {
            return;
        }

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

    public TestResults runTest(double ratioAdds, double ratioRemoves, double ratioContains) {

        Thread[] threads = new Thread[numThreads];
        ThreadRunner[] threadRunners = new ThreadRunner[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threadRunners[i] = new ThreadRunner(list, range, (int) (ratioAdds * 100), (int) (ratioRemoves),
                    (int) (ratioContains));
            threads[i] = new Thread(threadRunners[i]);
        }

        long time = System.nanoTime();

        for (Thread thread : threads) {
            thread.start();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            for (ThreadRunner x : threadRunners) {
                x.stopThread();
            }
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long total_time = System.nanoTime() - time;

        long numCompletedOps = 0;

        for (int i = 0; i < numThreads; i++) {
            numCompletedOps += threadRunners[i].numCompletedOps;
        }

        return new TestResults(total_time, initialSize, list.size(), numCompletedOps);

        // Thread[] threads = new Thread[numThreads];
        // int numAddsPerThread = (int) (numOpsPerThread * ratioAdds);
        // int numRemovesPerThread = (int) (numOpsPerThread * ratioRemoves);
        // int numContainsPerThread = (int) (numOpsPerThread * ratioContains);
        // Integer[][] operationsList = new Integer[numThreads][];

        // for (int i = 0; i < numThreads; i++) {
        // operationsList[i] = generateOperations(numAddsPerThread + numRemovesPerThread
        // + numContainsPerThread,
        // numAddsPerThread, numRemovesPerThread, numContainsPerThread);
        // }

        // long time = System.nanoTime();

        // for (int i = 0; i < numThreads; i++) {
        // threads[i] = new Thread(new RunOperations(operationsList[i]));
        // threads[i].start();
        // }

        // for (int i = 0; i < threads.length; i++) {
        // try {
        // threads[i].join();
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        // }

        // long total_time = System.nanoTime() - time;

        // return new TestResults(total_time, initialSize, list.size());
    }
}
