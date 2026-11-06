package cz.fpsengine.performance.monitor;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

/** Samples JVM heap and GC time via standard MXBeans. Meant to be called every few seconds, not per frame. */
public final class JvmMonitor {
    private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private long lastGcMs = -1;
    private long lastNs;

    public JvmSample sample() {
        long now = System.nanoTime();
        long gcMs = 0;
        for (GarbageCollectorMXBean b : gcBeans) {
            long t = b.getCollectionTime();
            if (t > 0) gcMs += t;
        }
        double fraction = 0;
        if (lastGcMs >= 0 && now > lastNs) {
            fraction = Math.min(1.0, (gcMs - lastGcMs) / ((now - lastNs) / 1e6));
        }
        lastGcMs = gcMs;
        lastNs = now;
        var heap = memory.getHeapMemoryUsage();
        return new JvmSample(heap.getUsed(), heap.getMax() > 0 ? heap.getMax() : heap.getCommitted(), fraction);
    }
}
