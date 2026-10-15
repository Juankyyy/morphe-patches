package app.juankyyy.patches.waterminder

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

val COMPATIBILITY_WATERMINDER = Compatibility(
    name = "WaterMinder",
    packageName = "com.funnmedia.waterminder",
    // APKPure/Uptodown distribuyen esta app como XAPK (base.apk + splits config.*).
    // Morphe lo soporta de forma nativa: ApkFileType.XAPK / XAPK_REQUIRED.
    apkFileType = ApkFileType.XAPK,
    targets = listOf(
        AppTarget(
            version = "5.10.10"
        )
    )
)

/** Compatibilidad experimental para parches en pruebas (desactivados por defecto). */
val COMPATIBILITY_WATERMINDER_EXPERIMENTAL = Compatibility(
    name = "WaterMinder",
    packageName = "com.funnmedia.waterminder",
    apkFileType = ApkFileType.XAPK,
    targets = listOf(
        AppTarget(
            version = "5.10.10",
            isExperimental = true
        )
    )
)
