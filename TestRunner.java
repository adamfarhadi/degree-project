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

    public TestResults runTest(int ratioAdds, int ratioRemoves, int ratioContains) {

        Thread[] threads = new Thread[numThreads];
        ThreadRunner[] threadRunners = new ThreadRunner[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threadRunners[i] = new ThreadRunner(list, range, ratioAdds, ratioRemoves, ratioContains);
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
    }
}
