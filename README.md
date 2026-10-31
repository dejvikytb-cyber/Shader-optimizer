# FPS Engine (Fabric, Minecraft 1.21.11) - Windows 10/11, NVIDIA focus, Intel + AMD CPUs

## What it really does
Honest summary: a mod cannot make a shader pack cheaper for the GPU. FPS Engine fixes the things that commonly cost real FPS on Windows + NVIDIA + Iris, and tells you the rest.

**Active changes (all toggleable in Mod Menu, all reversible):**
- **High-performance GPU preference** (`/fpsengine gpu` or button, opt-in): sets Windows' per-app GPU preference for the game's Java executable (HKCU only, restart required). On laptops with Intel/AMD iGPU + NVIDIA this is often the single biggest FPS gain, because Minecraft may otherwise render on the iGPU.
- **Process priority boost** (default on, Windows): AboveNormal for the game process only; reset to Normal when you turn the engine off. Helps when background apps compete for CPU. Small effect if nothing else is running.
- **Unfocused throttle** (default on, 20 FPS): sleeps the render loop while the window is in the background. Frees GPU/CPU for other apps. Does not raise in-game FPS.

**Diagnostics (`/fpsengine status`, `/fpsengine check`, Mod Menu):**
- Measured: avg FPS, 1% low, median/p99 frametime, stutter %, heap, GC time, the GL renderer actually used.
- Detected: CPU vendor/name (Intel/AMD), GPUs, Windows power plan, Game Mode, whether Iris has a shader pack active (via Iris' public API, no internals).
- Recommendations: wrong GPU in use, power saver plan, Game Mode off, too little/too much heap, non-G1 collector, NVIDIA Control Panel values, missing Sodium, optional helper mods.
- GPU load and VRAM are reported as unavailable (no supported interface).

**Not implemented:** Sodium/Iris internal hooks (chunk scheduling, shader state caching). I cannot verify those class signatures without the exact jars, and unverified hooks would crash or do nothing. The mod never changes shader settings, resolution, shadows or any visual option.

## Build from GitHub
1. Create a GitHub repo, upload this folder (keep `.github/`).
2. Actions tab > **Build FPS Engine** (also runs on every push).
3. Download artifact `FPS-Engine-1.21.11` -> `FPS-Engine-1.21.11.jar`.
Tag `v1.0.0` to attach the JAR to a GitHub release.
If a dependency cannot be resolved, adjust `gradle.properties` using https://fabricmc.net/develop.

## Build locally
JDK 21 + Gradle 9.2.1 once: `gradle wrapper --gradle-version 9.2.1`, then `gradlew.bat clean build` (Windows) or `./gradlew clean build` (Linux).

## Install
Fabric Loader 0.18+, Fabric API for 1.21.11, optional Mod Menu 17.0.0, optional Sodium + Iris. Put the JAR in `mods/`. Settings: Mod Menu > FPS Engine > configure.

## Commands
`/fpsengine on | off | status | check | gpu`

## JVM arguments (optional, benchmark yourself)
Set heap in your launcher (e.g. `-Xms4G -Xmx6G` with shaders; never above ~60% of RAM). Java 21 default G1 is fine; `-XX:+UseG1GC -XX:MaxGCPauseMillis=50` is a common safe starting point.
