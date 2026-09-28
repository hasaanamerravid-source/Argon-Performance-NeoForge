package argon.debug;

import java.util.concurrent.atomic.AtomicLong;

public final class ArgonProfiler {
    public static final AtomicLong PIECES_TESTED = new AtomicLong(0);
    public static final AtomicLong OCTREE_CHECKS_SAVED = new AtomicLong(0);
    public static final AtomicLong TOTAL_LAYOUT_TIME_NS = new AtomicLong(0);

    private ArgonProfiler() {}

    public static void reset() {
        PIECES_TESTED.set(0);
        OCTREE_CHECKS_SAVED.set(0);
        TOTAL_LAYOUT_TIME_NS.set(0);
    }
}