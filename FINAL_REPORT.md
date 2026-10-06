# NEXUS BOOST — FINAL ENGINEERING REPORT (`FINAL_REPORT.md`)

## 1. Executive Summary
**Nexus Boost** is a production-grade Android performance-control, capability discovery, hardware telemetry, and AI diagnostics platform built with Kotlin, Jetpack Compose (Material 3), Room, DataStore, Shizuku API, Android ADPF/Game Mode APIs, and multi-model Gemini intelligence.

## 2. What Works (Verified Functionality)
- **Dynamic Hardware & OS Capability Engine**: Performs read-only probing of `/sys/devices/system/cpu/cpufreq/policy*`, `/sys/class/devfreq/*`, `/sys/class/thermal/thermal_zone*`, `PerformanceHintManager`, `PowerManager` thermal status/headroom, `GameManager`, and `Display` refresh modes.
- **Shizuku Privileged Engine**: Detects Shizuku installation, binder liveness, runtime permissions, and execution UID (`SHIZUKU_SHELL` UID 2000 vs `ROOT` UID 0). Executes strictly allowlisted commands (`cmd game`, `cmd power`, `dumpsys gfxinfo`) with timeout, cancellation, and audit logging.
- **Transactional Performance Engine & State Machine**: Implements `DISCOVERED → SUPPORTED → READY → APPLYING → VERIFYING → ACTIVE → ROLLING_BACK → RESTORED`. Captures pre-modification `RollbackSnapshotEntity` records in Room, verifies every change via read-back, and automatically rolls back on failure, manual stop, thermal limit, or startup crash recovery.
- **Real Telemetry & Percentile Benchmark Engine**: Captures real `Choreographer` frame times (P50, P90, P95, P99 in ms), frame stability %, CPU cluster frequencies, memory availability, battery temperature, and thermal status with explicit confidence badges (`HIGH`, `MEDIUM`, `UNAVAILABLE`). Supports JSON, CSV, and Markdown report generation.
- **Gemini AI Performance Advisor**:
  - **Deep Hardware & Kernel Reasoning**: Uses `gemini-3.1-pro-preview` with `thinkingLevel = "HIGH"` to analyze live device capability reports, bottleneck percentiles, and thermal envelopes.
  - **Live SoC & Game Search Grounding**: Uses `gemini-3.5-flash` with `googleSearch` tool to retrieve up-to-date OEM thermal behavior, driver notes, and game frame-pacing patches.
  - **Fast Telemetry Triage**: Uses `gemini-3.1-flash-lite-preview` for instant low-latency snapshot summaries.

## 3. Device Dependencies & Known Limitations
- Direct kernel sysfs writes (`scaling_min_freq`, `devfreq/min_freq`) require `ROOT` (`UID 0`). On non-root devices, Nexus Boost clearly marks those controls `REQUIRES_ROOT` and automatically uses `SHIZUKU_SHELL` (`cmd game`, `cmd power`) or `NORMAL` (`PerformanceHintManager`, `ActivityManager` memory trim) controls.
- Cross-app frame timing requires `SHIZUKU_SHELL` (`dumpsys gfxinfo <pkg> framestats`); otherwise local window Choreographer timing is reported with a clear `MEDIUM (App Window)` confidence badge.
