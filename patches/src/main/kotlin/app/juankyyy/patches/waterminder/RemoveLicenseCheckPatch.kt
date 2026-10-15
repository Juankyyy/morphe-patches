package app.juankyyy.patches.waterminder

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val removeLicenseCheckPatch = bytecodePatch(
    name = "Remove license check",
    description = "Skips the Play license check so the app starts on installs outside Google Play.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WATERMINDER)

    execute {
        PairipLicenseCheckFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
    }
}
