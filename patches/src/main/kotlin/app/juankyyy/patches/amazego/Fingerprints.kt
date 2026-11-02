package app.juankyyy.patches.amazego

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

/**
 * Amaze GO! 1.37.0 usa AppLovin MAX como mediador (config de Meevii con
 * "mediation": "max"). Todos los anuncios fullscreen —interstitial, app open y
 * rewarded— pasan por este método, que es el punto común de la SDK:
 *
 *   MaxInterstitialAd.showAd(...) -> MaxFullscreenAdImpl.showAd(...)
 *   MaxAppOpenAd.showAd()          -> MaxFullscreenAdImpl.showAd(...)
 *   MaxRewardedAd.showAd(...)      -> MaxFullscreenAdImpl.showAd(...)
 *
 * El parche lo sustituye por callbacks inmediatos: para rewarded dispara
 * onUserRewarded(...) y para el resto solo onAdDisplayed/onAdHidden. El juego
 * (y Meevii) reciben el flujo normal sin que llegue a mostrarse el anuncio.
 *
 * Son dos overloads: el de 3 argumentos y el que recibe ViewGroup/Lifecycle.
 */
object MaxFullscreenShowAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/impl/mediation/ads/MaxFullscreenAdImpl;",
    name = "showAd",
    returnType = "V",
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Landroid/app/Activity;"
    ),
    filters = listOf(
        opcode(Opcode.RETURN_VOID)
    )
)

object MaxFullscreenShowAdWithViewFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/impl/mediation/ads/MaxFullscreenAdImpl;",
    name = "showAd",
    returnType = "V",
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Landroid/view/ViewGroup;",
        "Landroidx/lifecycle/Lifecycle;",
        "Landroid/app/Activity;"
    ),
    filters = listOf(
        opcode(Opcode.RETURN_VOID)
    )
)

/**
 * Banners de MAX. Dejar loadAd/startAutoRefresh en no-op evita que se cree y
 * pinte el banner; no hace falta tocar la vista ni el layout del juego.
 */
object MaxAdViewLoadAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxAdView;",
    name = "loadAd",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.RETURN_VOID)
    )
)

object MaxAdViewStartAutoRefreshFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxAdView;",
    name = "startAutoRefresh",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.RETURN_VOID)
    )
)
