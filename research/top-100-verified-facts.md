# Top 100 Verified Facts & Rejected Myths

## Verified Engineering Facts (AOSP & Linux Kernel)
1. **Android Game Mode (`cmd game`)**: Exposed on Android 12+ (`API 31+`) via `GameManagerService`. Shell (UID 2000 via Shizuku) can execute `cmd game mode [1|2|3] <package>` and `cmd game set --mode [2|3] --downscale [ratio] --fps [rate] <package>`.
2. **ADPF `PerformanceHintManager`**: Available on Android 12+ (`API 31+`) to normal apps; provides hints to PowerHAL (`uclamp.min` adjustments in the vendor HAL) for registered thread IDs.
3. **Thermal Headroom API**: `PowerManager.getThermalHeadroom(int forecastSeconds)` (Android 12+) returns a float where `1.0f` corresponds to `THERMAL_STATUS_SEVERE`.
4. **CPUFreq Policy Topology**: Modern big.LITTLE / DynamIQ SoCs group cores into `/sys/devices/system/cpu/cpufreq/policy0`, `policy4`, `policy7` etc. Reading `scaling_cur_freq`, `scaling_min_freq`, `scaling_max_freq`, and `scaling_available_frequencies` often works unprivileged or via shell, while writing requires UID 0 (`root` or `system`).
5. **GPU Devfreq Variability**: Qualcomm Adreno exposes `/sys/class/kgsl/kgsl-3d0/devfreq/`, while Mali/MediaTek/Tensor expose `/sys/class/devfreq/<addr>.mali` or platform-specific nodes. Paths must always be discovered by scanning `/sys/class/devfreq/`.
6. **SurfaceFlinger Frame Latency**: `dumpsys SurfaceFlinger --latency <layer>` and `dumpsys gfxinfo <package> framestats` are accessible to UID 2000 (`SHIZUKU_SHELL`) and provide true vsync/present timestamps.
7. **Background Memory Trimming**: `ActivityManager.killBackgroundProcesses(packageName)` works with `KILL_BACKGROUND_PROCESSES` normal permission, while `am send-trim-memory <process> RUNNING_CRITICAL` works via `SHIZUKU_SHELL`.

## Rejected Dangerous or Fake Claims (See `rejected-claims.md`)
- `setprop debug.sf.hw 1`, `setprop debug.performance.tuning 1`, `setprop video.accelerate.hw 1`: Dead/no-op build properties from Android 2.3 Gingerbread that do nothing in modern SurfaceFlinger/HWUI.
- Disabling `thermal-engine` or writing `0` to `/sys/class/thermal/thermal_zone*/mode`: Dangerous hardware safety violation that risks permanent PMIC/battery degradation or SoC shutdown.
- Arbitrary undervolting/overvolting or unlisted frequency writes: Rejected by kernel OPPs or causes immediate kernel panic.
