public class TestResults {
    public long time;
    public int initialSize; 
    public int finalSize;
    public long numCompletedOps;

    public TestResults(long time, int initialSize, int finalSize, long numCompletedOps) {
        this.time = time;
        this.initialSize = initialSize;
        this.finalSize = finalSize;
        this.numCompletedOps = numCompletedOps;
    }
}
