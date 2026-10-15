package app.juankyyy.patches.remotemouse

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock PRO",
    description = "Unlocks all PRO features.",
    default = true
) {
    compatibleWith(COMPATIBILITY_REMOTE_MOUSE)

    execute {
        // 1. Paywall UI -> siempre FULL_ACCESS.
        PaywallDeriveFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Lcom/hungrybolo/remotemouseandroid/purchase/paywall/PaywallScreenState;->FULL_ACCESS:Lcom/hungrybolo/remotemouseandroid/purchase/paywall/PaywallScreenState;
                return-object v0
            """
        )

        // 2. Feature flags reales -> todos true, se salta el salto a comprar.
        EntitlementsRecomputeFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                sput-boolean v0, Lcom/hungrybolo/remotemouseandroid/utils/GlobalVars;->isRemoveAds:Z
                sput-boolean v0, Lcom/hungrybolo/remotemouseandroid/utils/GlobalVars;->isBuyMedia:Z
                sput-boolean v0, Lcom/hungrybolo/remotemouseandroid/utils/GlobalVars;->isBuyClipboard:Z
                sput-boolean v0, Lcom/hungrybolo/remotemouseandroid/utils/GlobalVars;->isBuyWeb:Z
                sput-boolean v0, Lcom/hungrybolo/remotemouseandroid/utils/GlobalVars;->isBuyLandFunction:Z
                sput-boolean v0, Lcom/hungrybolo/remotemouseandroid/utils/GlobalVars;->isBuyKeyboardFunction:Z
                sput-boolean v0, Lcom/hungrybolo/remotemouseandroid/utils/GlobalVars;->isVolumeFunctionEnabled:Z
                sput-boolean v0, Lcom/hungrybolo/remotemouseandroid/utils/GlobalVars;->isShowWeb:Z
                return-void
            """
        )
    }
}
