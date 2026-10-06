# Frame Pacing & SurfaceFlinger (`knowledge/frame-pacing.md`)
- Uses `Choreographer.FrameCallback` for live in-app UI frame timing (P50, P90, P95, P99 in ms) and `dumpsys gfxinfo <package> framestats` when `SHIZUKU_SHELL` is active.
- Display refresh rate capabilities are queried from `Display.getSupportedModes()`.
