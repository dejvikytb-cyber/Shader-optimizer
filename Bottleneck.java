package cz.fpsengine.performance.bottleneck;

public enum Bottleneck {
    /** Not enough data or no clear signal. Conservative defaults apply. */
    UNDETERMINED("undetermined"),
    MEMORY_PRESSURE("memory pressure (heap/GC)"),
    FRAME_PACING("frame pacing (stutter)");

    public final String label;

    Bottleneck(String label) {
        this.label = label;
    }
}
