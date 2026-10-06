# Security & Privilege Boundary Audit (`security-audit.md`)

## 1. Command Injection Prevention
- **Vector**: Malicious or malformed package name passed to `cmd game mode` or `am`.
- **Mitigation**: `CommandValidator.isValidPackageName()` enforces `^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+$` and max length 128. `CommandValidator.isAllowlistedCommand()` enforces a strict prefix and token whitelist (`cmd game`, `cmd power set-fixed-performance-mode-enabled`, `dumpsys gfxinfo`, `dumpsys SurfaceFlinger --latency`). Arbitrary user shell strings are impossible to execute.

## 2. Privilege Escalation & Boundary Honesty
- **Vector**: Confusing Shizuku ADB/Shell (`UID 2000`) with Root (`UID 0`).
- **Mitigation**: `ShizukuEngine` explicitly inspects `Shizuku.getUid()`. When `uid == 2000`, kernel `/sys/` writes that require `root` remain locked in `REQUIRES_ROOT` state and are never blindly executed.

## 3. Secret & API Key Protection
- **Vector**: Hardcoding `GEMINI_API_KEY` or logging credentials in Room audit logs.
- **Mitigation**: `GEMINI_API_KEY` is injected exclusively via `BuildConfig.GEMINI_API_KEY` from `.env` / AI Studio Secrets panel. Retrofit clients do not log query parameters containing `key`, and a prominent Prototype Security Warning banner is displayed in the AI Advisor & Settings UI.
