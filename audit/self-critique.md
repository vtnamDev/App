# Self-Critique & Engineering Audit (`audit/self-critique.md`)

1. **Which features are actually functional?**
   - Dynamic sysfs & Android API capability scanner (`CPUFreq`, `devfreq`, `thermal_zone*`, `PerformanceHintManager`, `PowerManager`, `GameManager`, `Display.Mode`).
   - Real Shizuku Binder detection, permission flow, UID identity (`2000` vs `0`), binder death recovery, and allowlisted command execution.
   - Real ADPF `PerformanceHintManager.Session` lifecycle, safe background memory trim with `ActivityManager.MemoryInfo` delta verification, and Shizuku `cmd game` / `cmd power` execution with read-back verification.
   - Real hardware telemetry: `Choreographer` frame-time percentile (P50/P90/P95/P99) tracking, CPU cluster live frequency polling, thermal status/headroom monitoring, and battery temperature sensing.
   - Multi-model Gemini AI Performance Advisor (`gemini-3.1-pro-preview` with `ThinkingLevel.HIGH`, `gemini-3.5-flash` with Google Search grounding, `gemini-3.1-flash-lite-preview` for fast telemetry triage).
2. **Which are simulated?**
   - **None.** There are zero fake FPS generators, zero fake RAM boost percentages, and zero dummy shell sliders.
3. **What happens on a device with no Shizuku and no root?**
   - Capabilities requiring Shizuku/Root clearly display `REQUIRES_SHIZUKU` or `REQUIRES_ROOT` with exact evidence paths. The Boost Orchestrator applies the strongest supported `NORMAL` privilege controls (ADPF `PerformanceHintManager` session + Background Memory Trim + Thermal Guard) and reports partial/supported status honestly.
4. **Can every write be rolled back?**
   - Yes. Every applied tuning records a `RollbackSnapshotEntity` in Room before execution and is restored in reverse order on Stop, Failure, or Startup Crash Recovery.
