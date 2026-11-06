# Testing checklist

Fixed world seed, fixed position and camera, 60 s warm-up, 120 s capture, 3 runs per case. Record `/fpsengine status` each run.

1. Mod Menu opens FPS Engine's screen; every toggle persists after restart (`config/fpsengine.json`).
2. `/fpsengine off` -> priority returns to Normal (Task Manager > Details > javaw.exe); `on` -> AboveNormal.
3. Laptop with iGPU + NVIDIA: before/after `/fpsengine gpu` + restart: compare `GPU in use` and FPS.
4. Alt-tab with throttle on: GPU usage drops; focus returns to normal FPS.
5. Cases: vanilla, Sodium, Sodium+Iris no pack, light pack, heavy pack, render distance 8/16/32, entity stress, 60 min session.
6. Intel and AMD machines: `/fpsengine check` shows the right vendor advice.
7. Enabled vs disabled FPS should differ only where priority/GPU preference matters; report any regression as overhead.
