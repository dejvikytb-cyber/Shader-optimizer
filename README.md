# FPS Engine (Fabric, Minecraft 1.21.11)

## Honest scope: phase 1
This build is **measurement and diagnostics only**. It does not increase FPS yet.
It contains:
- Frame monitor (avg FPS, 1% low, median/p99 frametime, stutter ratio), allocation-free ring buffer.
- JVM heap and GC-time sampling every 5 s via MXBeans.
- Bottleneck classifier (memory pressure, frame pacing) with hysteresis, confirmation streak and cooldown. CPU vs GPU is not guessed.
- Read-only detection of Sodium, Iris and common performance mods.
- `/fpsengine on|off|status`; state saved in `config/fpsengine.json`. Self-deactivates on any internal error.
- GPU load and VRAM are reported as unavailable.

Sodium/Iris-specific optimizations (chunk scheduling, pipeline state caching, etc.) are phase 2 and need verified class and method listings for the exact Sodium and Iris versions.

## Build from GitHub (no local setup)
1. Create a new GitHub repository and upload everything in this folder (keep `.github/`).
2. Open the **Actions** tab, run **Build FPS Engine** (it also runs on every push).
3. Download the artifact **FPS-Engine-1.21.11**, which contains `FPS-Engine-1.21.11.jar`.
4. To publish a release: push a tag like `v1.0.0`; the JAR is attached to the release automatically.

If the build fails with "could not resolve", edit versions in `gradle.properties` using https://fabricmc.net/develop.

## Build locally
Requires JDK 21 and Gradle 9.2.1 (once, to create the wrapper):
```
gradle wrapper --gradle-version 9.2.1
```
Windows: `gradlew.bat clean build`  Linux: `./gradlew clean build`
Output: `build/libs/FPS-Engine-1.21.11.jar`

## Install
Fabric Loader 0.18+, Fabric API for 1.21.11, drop the JAR in `mods/`. Sodium and Iris are optional.

See `docs/TESTING.md` for the benchmarking checklist.
