package unrolled;

public final class Constants {
    private Constants(){}

    // only use even K since we aren't using ceil/floor
    public static final int K = 8;
    public static final int MINFULL = K / 4;
    public static final int MAXMERGE = 3 * K / 4;

    public static final int sentinalMin = Integer.MIN_VALUE;
    public static final int sentinalMax = Integer.MAX_VALUE;
    public static final int unusedSlot = Integer.MIN_VALUE + 1;
}
