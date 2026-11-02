package app.juankyyy.patches.amazego

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

val COMPATIBILITY_AMAZE_GO = Compatibility(
    name = "Amaze GO!",
    packageName = "com.oakever.arrows",
    // APKMirror distribuye esta release como BUNDLE: base.apk + arm64-v8a + armeabi-v7a.
    apkFileType = ApkFileType.APKM,
    appIconColor = 0xFE6C0E,
    targets = listOf(
        AppTarget(
            version = "1.37.0"
        )
    )
)
