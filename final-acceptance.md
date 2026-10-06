# Final Acceptance Checklist (`final-acceptance.md`)

## BUILD
- [x] Clean compilation via `compile_applet`
- [x] Zero compilation errors
- [x] Unit & Robolectric test suite passing (`gradle :app:testDebugUnitTest`)

## SHIZUKU
- [x] Installation detected (`moe.shizuku.privileged.api`)
- [x] Service binder status detected (`Shizuku.pingBinder()`)
- [x] Runtime permission request handled
- [x] UID identity detected (`UID 2000 SHELL` vs `UID 0 ROOT`)
- [x] Binder death listener & recovery implemented

## CAPABILITY
- [x] CPU policies & frequencies scanned dynamically
- [x] GPU devfreq nodes scanned dynamically
- [x] Thermal zones & PowerManager thermal APIs scanned
- [x] Android Game Mode & ADPF PerformanceHintManager checked
- [x] Unsupported states clearly badged with evidence and fallbacks

## PERFORMANCE & SAFETY
- [x] Real controls only with read-back verification
- [x] Transactional Room rollback snapshots before every apply
- [x] Thermal hysteresis guard preserves hardware safety
- [x] Strict package regex & command allowlist enforcement
