# Hardware & System Safety Policy (`safety-policy.md`)

1. **Zero Thermal Bypass**: Nexus Boost strictly prohibits stopping thermal daemons (`thermal-engine`, `thermald`), altering `/sys/class/thermal/thermal_zone*/trip_point_*`, or masking thermal interrupts.
2. **Automatic Thermal Hysteresis Step-Down**: When `PowerManager.getCurrentThermalStatus()` reaches `THERMAL_STATUS_MODERATE` (or battery/SoC temperature exceeds `42.0°C`), the Dynamic Controller automatically throttles back active performance overrides and only clears the thermal limit once temperature drops below `39.5°C`.
3. **Zero Voltage Manipulation**: No sysfs or kernel interface related to PMIC/regulator voltages (`/sys/class/regulator`, undervolting, overvolting) is ever accessed or written.
4. **Validated OPP Ranges Only**: CPU and GPU frequency writes are strictly validated against the hardware's own `scaling_available_frequencies` / `available_frequencies` list.
5. **Allowlisted Privileged Execution**: The Shizuku command engine validates package names with strict regex `^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+$` and rejects shell metacharacters (`;`, `|`, `&`, `` ` ``, `$`, `\n`, `>`, `<`).
6. **Transactional Rollback**: Every state change records a `RollbackSnapshotEntity` in Room **before** execution and restores in reverse order on manual stop, game exit, failure, or app startup recovery.
