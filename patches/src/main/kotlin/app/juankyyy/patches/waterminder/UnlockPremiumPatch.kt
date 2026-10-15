package app.juankyyy.patches.waterminder

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks all Premium features.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WATERMINDER)

    execute {
        // 1. RevenueCat: todos los entitlements activos.
        RevenueCatEntitlementActiveFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )

        // 2. Superwall: suscripción siempre activa.
        SuperwallSubscriptionActiveFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )

        // 3. Gates de la app: nada es premium (sin candado, sin paywall).
        ReminderTypePremiumFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """
        )
        CupTypePremiumFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """
        )
        SettingsMenuPremiumFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """
        )

        // 4. Estado de compra: siempre comprado (puerta central usada en 35+ sitios).
        PurchaseStateFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
    }
}
