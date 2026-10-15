package app.juankyyy.patches.remotemouse

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val disablePopupsPatch = bytecodePatch(
    name = "Disable popups",
    description = "Disables rate dialog, what's new dialog and auto update checks.",
    default = true
) {
    compatibleWith(COMPATIBILITY_REMOTE_MOUSE)

    execute {
        // WhatsNewDialog nunca se muestra.
        WhatsNewShouldShowFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """
        )
        // RateDialog nunca se muestra.
        RateDialogShowFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
        // No chequear updates al abrir la app.
        AutoUpdaterCheckUpdateFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
    }
}
