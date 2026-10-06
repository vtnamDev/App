# Transactional Rollback Matrix (`rollback-matrix.md`)

| Tuning ID | Pre-Apply Snapshot Source | Apply Action | Verification Read-Back | Rollback Action | Crash / Reboot Recovery |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `adpf_hint_session` | `Session == null` | `createHintSession(tids, targetNs)` | `session != null` | `session.close()` | Ephemeral per-process; auto-cleaned by OS on process death |
| `background_memory_trim` | `MemoryInfo.availMem` | `killBackgroundProcesses(pkg)` | `availMem >= baseline` | N/A (OS re-spawns cached processes on demand) | No persistent modification |
| `android_game_mode_perf` | `cmd game mode <pkg>` | `cmd game mode performance <pkg>` | `cmd game mode <pkg>` contains `Performance` / `2` | `cmd game mode <original_mode> <pkg>` | Persisted in `rollback_snapshots` table; restored on next app launch if un-restored |
| `power_fixed_performance_mode` | `false` | `cmd power set-fixed-performance-mode-enabled true` | Exit code `0` | `cmd power set-fixed-performance-mode-enabled false` | Persisted in `rollback_snapshots`; restored on next launch; reset by OS on reboot |
| `cpufreq_policy_floor` | Read `/sys/.../policyX/scaling_min_freq` | Write validated OPP freq to `scaling_min_freq` | Read back `scaling_min_freq == target` | Write `originalValue` to `scaling_min_freq` | Restored on startup if same boot session; kernel resets sysfs on device reboot |
| `gpu_devfreq_min_floor` | Read `/sys/class/devfreq/*/min_freq` | Write validated OPP freq to `min_freq` | Read back `min_freq == target` | Write `originalValue` to `min_freq` | Restored on startup if same boot session; kernel resets sysfs on device reboot |
