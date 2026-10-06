# OEM Variants (`knowledge/oem-variants.md`)
- Isolates vendor-specific sysfs and service signatures for Qualcomm Snapdragon (`kgsl-3d0`), Google Tensor (`exynos-drm` / `mali`), MediaTek Dimensity (`ged` / `mali`), and Samsung Exynos.
- Marks OEM features as `UNSUPPORTED` when their specific node or service is absent on the current runtime device.
