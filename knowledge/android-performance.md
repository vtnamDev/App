# Knowledge Base: Android Performance & ADPF (`knowledge/android-performance.md`)

## 1. Android Dynamic Performance Framework (ADPF)
- **Mechanism**: Bridges application workload threads with the device's PowerHAL and kernel scheduler (`schedutil` / `uclamp`).
- **Owner**: Android Framework (`android.os.PerformanceHintManager`, `android.os.PowerManager`).
- **Privilege Required**: `NORMAL` (API 31+ / Android 12+).
- **Detection**: `context.getSystemService(PerformanceHintManager::class.java)?.preferredUpdateRateNanos != null` and `Build.VERSION.SDK_INT >= 31`.
- **Measurable Benefit**: Reduces frame-time P99 spikes by signaling thread work duration targets (`createHintSession`) before CPU frequency drops.
- **Rollback**: Close `PerformanceHintManager.Session` via `session.close()`.
