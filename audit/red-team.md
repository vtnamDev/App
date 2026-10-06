# Red Team Adversarial Review (`audit/red-team.md`)

| Attack / Failure Scenario | System Response | Verification |
| :--- | :--- | :--- |
| **Package Name Injection** (`com.game; rm -rf /`) | Rejected by `CommandValidator.isValidPackageName()` before command construction; logged as `VALIDATION_FAILED` in `error_events`. | Unit tested in `NexusBoostEngineTest` |
| **Disallowed Shell Command** (`stop thermal-engine`) | Rejected by `CommandValidator.isAllowlistedCommand()` and blocked by `SafetyPolicy`. | Unit tested in `NexusBoostEngineTest` |
| **Shizuku Binder Dies Mid-Boost** | `OnBinderDeadListener` immediately updates `PrivilegeState` to `BINDER_DEAD`, cancels pending shell commands, and rolls back local sessions. | Tested in state machine transitions |
| **Partial Optimization Failure** | Step 1 succeeds, Step 2 fails verification -> `PerformanceEngine` immediately triggers reverse rollback of Step 1 and transitions to `RESTORED` with error report. | Unit tested in `NexusBoostEngineTest` |
| **Thermal Spike (`>= 42.0°C` or `THERMAL_STATUS_MODERATE`)** | Thermal Hysteresis Controller transitions Boost state to `LIMITED`, steps down high-power overrides, and waits until temp `< 39.5°C` before allowing re-boost. | Unit tested in `NexusBoostEngineTest` |
| **Stale Reboot Snapshot** | `RollbackSnapshotEntity` stores `bootSessionId` (`SystemClock.elapsedRealtime()` epoch). Snapshots from a previous boot are marked `EXPIRED_BY_REBOOT` instead of writing stale values. | Verified in `PerformanceEngine` |
