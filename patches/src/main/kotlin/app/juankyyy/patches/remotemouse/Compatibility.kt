package app.juankyyy.patches.remotemouse

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

val COMPATIBILITY_REMOTE_MOUSE = Compatibility(
    name = "Remote Mouse",
    packageName = "com.hungrybolo.remotemouseandroid",
    // APKMirror marca esta release 5.315 como BUNDLE -> en Morphe se declara como APKM.
    apkFileType = ApkFileType.APKM,
    appIconColor = 0x2196F3,
    targets = listOf(
        AppTarget(
            version = "5.315"
        )
    )
)
