# Device Capability Discovery Specification (`capability-report.md`)

Nexus Boost performs a **100% read-only dynamic capability scan** on startup and on user request. No paths are assumed writable until tested.

| Capability ID | Subsystem | Detection Path / API | Privilege Tier | Fallback When Unsupported |
| :--- | :--- | :--- | :--- | :--- |
| `adpf_performance_hint` | Android ADPF | `PerformanceHintManager` (API 31+) | `NORMAL` | Standard thread priority hints |
| `thermal_telemetry` | Thermal Subsystem | `PowerManager.getCurrentThermalStatus()` + `/sys/class/thermal/thermal_zone*` | `NORMAL` | `BatteryManager.EXTRA_TEMPERATURE` |
| `display_refresh_modes` | Display Subsystem | `Display.getSupportedModes()` | `NORMAL` | Native default display mode |
| `background_memory_trim` | ActivityManager | `ActivityManager.killBackgroundProcesses()` | `NORMAL` | Manual app closure guidance |
| `android_game_mode` | GameManagerService | `cmd game mode <pkg>` | `REQUIRES_SHIZUKU` | ADPF + Memory Trim |
| `power_fixed_performance` | PowerManagerService | `cmd power set-fixed-performance-mode-enabled` | `REQUIRES_SHIZUKU` | ADPF HintSession |
| `cpufreq_policy_control` | Kernel CPUFreq | `/sys/devices/system/cpu/cpufreq/policy*` | `REQUIRES_ROOT` | Read-only frequency monitoring |
| `gpu_devfreq_control` | Kernel Devfreq | `/sys/class/devfreq/*` | `REQUIRES_ROOT` | Read-only or `UNSUPPORTED` badge |
