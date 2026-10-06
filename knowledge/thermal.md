# Knowledge Base: Thermal Systems & Safety (`knowledge/thermal.md`)

## 1. Thermal Monitoring & Hysteresis Protection
- **Mechanism**: Combines `PowerManager.getCurrentThermalStatus()`, `PowerManager.getThermalHeadroom(10)`, BatteryManager temperature (`EXTRA_TEMPERATURE / 10.0`), and `/sys/class/thermal/thermal_zone*/temp`.
- **Safety Boundary**: Nexus Boost **never** disables thermal zones or stops thermal daemons. When thermal status reaches `THERMAL_STATUS_MODERATE` (2) or higher, or battery temp exceeds `42.0°C`, the Dynamic Controller automatically steps down aggressive boosts until temperatures drop below `39.5°C` (2.5°C hysteresis band).
