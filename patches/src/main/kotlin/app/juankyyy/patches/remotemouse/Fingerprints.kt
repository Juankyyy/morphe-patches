package app.juankyyy.patches.remotemouse

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

/**
 * Fingerprint exacto para Remote Mouse 5.315 (confirmado en jadx).
 *
 * Clase: com.hungrybolo.remotemouseandroid.purchase.paywall.PaywallScreenState
 * Método: public static PaywallScreenState derive(boolean x7)
 *   if (z2 || z3 || z4) return FULL_ACCESS;
 *   if (z5) return SUBSCRIBED;
 *   if (z6) return PREMIUM_LEGACY;
 *   ...
 *
 * Parchear este método a FULL_ACCESS desbloquea todo el paywall de golpe.
 */
object PaywallDeriveFingerprint : Fingerprint(
    definingClass = "Lcom/hungrybolo/remotemouseandroid/purchase/paywall/PaywallScreenState;",
    name = "derive",
    returnType = "Lcom/hungrybolo/remotemouseandroid/purchase/paywall/PaywallScreenState;",
    parameters = listOf("Z", "Z", "Z", "Z", "Z", "Z", "Z"),
    filters = listOf(
        opcode(Opcode.RETURN_OBJECT)
    )
)

/**
 * Segundo gate: Entitlements.recompute() es el que setea los flags reales
 * GlobalVars.isBuyMedia / isBuyWeb / isRemoveAds / etc. leyendo prefs.
 * El parche de derive() solo cambia el UI a FULL_ACCESS, pero las funciones
 * chequean GlobalVars. Forzar recompute() a true desbloquea el uso real.
 */
object EntitlementsRecomputeFingerprint : Fingerprint(
    definingClass = "Lcom/hungrybolo/remotemouseandroid/purchase/Entitlements;",
    name = "recompute",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.SPUT_BOOLEAN)
    )
)
