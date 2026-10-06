# Knowledge Base: GPU Devfreq Control (`knowledge/devfreq-gpu.md`)

## 1. Dynamic GPU Devfreq Discovery
- **Mechanism**: Queries `/sys/class/devfreq/*` and `/sys/class/kgsl/kgsl-3d0/devfreq` for `cur_freq`, `min_freq`, `max_freq`, `available_frequencies`, and `governor`.
- **Owner**: Linux Kernel `devfreq` driver (`drivers/devfreq/`).
- **Privilege Required**: Read: `NORMAL` / `SHIZUKU_SHELL` (SoC-dependent); Write: `ROOT`.
- **Safety**: Never hardcode `/sys/class/kgsl/kgsl-3d0` without verifying existence. Never write unlisted frequencies.
