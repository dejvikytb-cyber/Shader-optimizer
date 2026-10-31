# Testing and benchmarking checklist

Use one world seed, one fixed spawn position and camera angle, 60 s warm-up, 120 s capture, 3 runs per case.

Cases: vanilla; Sodium only; Sodium+Iris no pack; Sodium+Iris light pack; Sodium+Iris heavy pack;
render distance 8/16/32; entity/particle stress area; 60 min session (watch heap trend); `/fpsengine on|off` toggling; with Lithium, FerriteCore, ImmediatelyFast.

Record per run: `/fpsengine status` (avg FPS, 1% low, median/p99 ms, heap, GC%), enabled vs disabled.
Phase 1 does not change rendering, so enabled and disabled results should match within noise; a difference means overhead, which is a bug to report.
