# Knowledge Base: CPUFreq Policy Control (`knowledge/cpufreq.md`)

## 1. CPUFreq Policy Clamping & Governor Selection
- **Mechanism**: Modifies `/sys/devices/system/cpu/cpufreq/policyX/scaling_governor` and `scaling_min_freq` / `scaling_max_freq`.
- **Owner**: Linux Kernel `cpufreq` subsystem (`drivers/cpufreq/`).
- **Privilege Required**: Read is often `NORMAL` or `SHIZUKU_SHELL`; Write requires `ROOT` (`UID 0`).
- **Detection**: Dynamic directory scan of `/sys/devices/system/cpu/cpufreq/policy*`. Read `scaling_available_governors` and `scaling_available_frequencies`.
- **Validation & Rollback**: Only values present in `scaling_available_frequencies` or `scaling_available_governors` are permitted. Original values are snapshotted prior to write and verified via read-back.
