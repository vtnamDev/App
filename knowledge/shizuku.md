# Knowledge Base: Shizuku Privileged Engine (`knowledge/shizuku.md`)

## 1. Binder IPC & Privilege Identity
- **Mechanism**: Proxies Binder transactions and process execution through the `shizuku_server` daemon started via Wireless Debugging (`UID 2000` / `SHIZUKU_SHELL`) or Root (`UID 0` / `ROOT`).
- **Owner**: `rikka.shizuku.Shizuku` & Android `adbd` / `su`.
- **Detection**:
  - Package installed: `PackageManager.getPackageInfo("moe.shizuku.privileged.api", 0)`
  - Binder alive: `Shizuku.pingBinder()`
  - Permission granted: `Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED`
  - Identity: `Shizuku.getUid()` (`0` = ROOT, `2000` = SHELL)
- **Failure Modes & Recovery**: If `OnBinderDeadListener` fires during an active profile, immediately abort pending commands, mark Shizuku state `BINDER_DEAD`, and trigger local API rollback where possible.
