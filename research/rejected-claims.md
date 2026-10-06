# Rejected Claims & Anti-Patterns

The following "game booster" claims frequently found in low-quality repositories and forums were audited against AOSP source code and Linux kernel documentation and **strictly banned** from Nexus Boost:

| Claim / Command | Source Tier | Why It Is Rejected |
| :--- | :--- | :--- |
| `setprop debug.performance.tuning 1` | E (Forum myth) | Does not exist anywhere in AOSP `frameworks/base` or `frameworks/native`. Zero effect. |
| `setprop debug.egl.hw 1` / `debug.sf.hw 1` | E (Obsolete) | Removed after Android 4.0 when hardware acceleration became mandatory in SurfaceFlinger. |
| `stop thermal-engine` / `stop vendor.thermal-engine` | D (Dangerous) | Disables userspace thermal mitigation, risking battery swelling, skin burn limits, and sudden PMIC emergency power-off. |
| Writing arbitrary MHz to `scaling_max_freq` | D (Invalid) | Linux `cpufreq` clamps or rejects values not defined in the SoC's DeviceTree OPP table (`scaling_available_frequencies`). |
| `echo 3 > /proc/sys/vm/drop_caches` in a loop | C (Counterproductive) | Evicts clean page cache (including game shader/asset pages), causing immediate disk I/O thrashing and severe frame stutters. |
| Fake "RAM Boosted by 42%" animation | E (Deceptive UI) | Violates Nexus Boost Non-Negotiable Rule #1 & #2. Memory metrics must reflect real `ActivityManager.MemoryInfo` before and after. |
