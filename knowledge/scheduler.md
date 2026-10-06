# Knowledge Base: Scheduler, Game Mode, Frame Pacing, Profiling, OEM & UI (`knowledge/scheduler.md`)

## Scheduler & UCLAMP (`scheduler.md`)
- Uses ADPF `PerformanceHintManager` on non-root devices and inspects `/dev/cpuctl/top-app/cpu.uclamp.min` on root devices.
- Background processes are trimmed safely using `ActivityManager.killBackgroundProcesses(pkg)` and `cmd activity send-trim-memory` via Shizuku without touching persistent system processes.
