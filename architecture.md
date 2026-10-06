# NEXUS BOOST — PRODUCTION ARCHITECTURE (`architecture.md`)

## 1. Layered Clean Architecture
```
UI Layer (Jetpack Compose + Material 3 Adaptive Navigation)
  ├── DashboardScreen (Live Telemetry, Boost State Machine, Thermal Guard)
  ├── CapabilitiesScreen (Dynamic Sysfs/API Probe Evidence & OEM Adapter Report)
  ├── TuningScreen (Verified Tuning Catalog, Read-Back Verification & Rollback)
  ├── GamesScreen (Installed App Scanner, Per-Game Profiles & Presets)
  ├── BenchmarksScreen (P50/P90/P95/P99 Frame-Time Sessions, JSON/CSV/MD Export)
  ├── AiAdvisorScreen (Gemini 3.1 Pro High-Thinking, 3.5 Flash Search Grounding, 3.1 Flash-Lite Quick Triage)
  └── AuditLogsScreen (Transactional Rollback Matrix, Command Audit Trail, Crash Recovery)
        ↓
ViewModel Layer (`NexusBoostViewModel`)
        ↓
Domain / Engine Controllers
  ├── `ShizukuEngine`: Binder lifecycle, UID 2000 vs UID 0 detection, strict allowlisted command execution with timeout
  ├── `CapabilityScanner`: Dynamic read-only discovery of CPU policies, GPU devfreq, Thermal zones, ADPF, GameManager, Display modes
  ├── `PerformanceEngine`: State machine (`DISCOVERED -> SUPPORTED -> READY -> APPLYING -> VERIFYING -> ACTIVE -> ROLLING_BACK -> RESTORED`)
  ├── `TelemetryEngine`: Choreographer frame-time percentiles, `/proc/stat`, `/sys/devices/system/cpu`, Battery & PowerManager thermal headroom
  └── `GeminiAdvisorService`: Multi-model AI reasoning (`gemini-3.1-pro-preview` with `ThinkingLevel.HIGH`, `gemini-3.5-flash` with `googleSearch`, `gemini-3.1-flash-lite-preview`)
        ↓
Persistence Layer (Room Database `NexusDatabase` + Preferences DataStore)
  ├── Entities: `DeviceSnapshotEntity`, `GameProfileEntity`, `TuningActionEntity`, `BenchmarkSessionEntity`, `TelemetrySampleEntity`, `PrivilegeStateEntity`, `RollbackSnapshotEntity`, `ErrorEventEntity`
```

## 2. Command Abstraction & Transactional Safety
No UI component can invoke shell commands directly. Every operation flows through:
`CommandRequest` → `Package/Regex Validation` → `PrivilegeCheck` → `CapabilityCheck` → `Snapshot Original Value to Room` → `Execute` → `Read-Back Verify` → `Audit Log` → `Automatic Rollback on Failure or Exit`.
