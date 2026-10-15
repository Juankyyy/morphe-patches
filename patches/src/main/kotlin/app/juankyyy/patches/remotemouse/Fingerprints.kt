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

/**
 * Ads de Remote Mouse 5.315 (confirmado en el APK).
 *
 * Paquete functions/ads/: AdController delega en GoogleAd (AdMob).
 * Hacer no-op los métodos de carga/muestra elimina banners e
 * intersticiales aunque cambie la lógica de Entitlements.recompute().
 */
object GoogleAdLoadFullAdFingerprint : Fingerprint(
    definingClass = "Lcom/hungrybolo/remotemouseandroid/functions/ads/GoogleAd;",
    name = "loadFullAd",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.RETURN_VOID)
    )
)

object GoogleAdDisplayInterstitialFingerprint : Fingerprint(
    definingClass = "Lcom/hungrybolo/remotemouseandroid/functions/ads/GoogleAd;",
    name = "displayInterstitial",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.RETURN_VOID)
    )
)

object GoogleAdOnCreateAddFingerprint : Fingerprint(
    definingClass = "Lcom/hungrybolo/remotemouseandroid/functions/ads/GoogleAd;",
    name = "onCreateAdd",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroid/view/ViewGroup;"),
    filters = listOf(
        opcode(Opcode.RETURN_VOID)
    )
)

object AdControllerLoadFullAdFingerprint : Fingerprint(
    definingClass = "Lcom/hungrybolo/remotemouseandroid/functions/ads/AdController;",
    name = "loadFullAd",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.RETURN_VOID)
    )
)

object AdControllerOnCreateAddFingerprint : Fingerprint(
    definingClass = "Lcom/hungrybolo/remotemouseandroid/functions/ads/AdController;",
    name = "onCreateAdd",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroid/view/ViewGroup;"),
    filters = listOf(
        opcode(Opcode.RETURN_VOID)
    )
)
