package app.juankyyy.patches.remotemouse

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Disables banner and interstitial AdMob ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_REMOTE_MOUSE)

    execute {
        // No-op a la carga y muestra de ads en GoogleAd y AdController.
        GoogleAdLoadFullAdFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
        GoogleAdDisplayInterstitialFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
        GoogleAdOnCreateAddFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
        AdControllerLoadFullAdFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
        AdControllerOnCreateAddFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
    }
}
