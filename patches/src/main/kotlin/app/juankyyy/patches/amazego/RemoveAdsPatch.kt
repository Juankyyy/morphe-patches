package app.juankyyy.patches.amazego

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

/**
 * Smali inyectado al principio de MaxFullscreenAdImpl.showAd(...).
 *
 * - Si no hay anuncio cargado (this.f == null) se deja el flujo original, que
 *   termina en onAdDisplayFailed y no muestra nada.
 * - Si hay anuncio pero no hay listener, se retorna sin mostrar nada.
 * - Si el wrapper es MaxRewardedAd, se disparan onAdDisplayed, onUserRewarded
 *   (MaxReward de importe 1) y onAdHidden; el juego recibe la recompensa.
 * - Para interstitial/app open se disparan onAdDisplayed y onAdHidden.
 *
 * El campo adListener está en la superclase a (mismo paquete) y el campo f es
 * privado de MaxFullscreenAdImpl; ambos son accesibles desde el propio método.
 * p1/p2 se usan como temporales solo en las ramas que terminan en return.
 */
private val FAKE_FULLSCREEN_SMALI = """
    iget-object v0, p0, Lcom/applovin/impl/mediation/ads/a;->adListener:Lcom/applovin/mediation/MaxAdListener;
    iget-object v1, p0, Lcom/applovin/impl/mediation/ads/MaxFullscreenAdImpl;->f:Lcom/applovin/impl/t2;
    if-eqz v1, :amazego_original
    if-eqz v0, :amazego_return
    iget-object p1, p0, Lcom/applovin/impl/mediation/ads/MaxFullscreenAdImpl;->a:Lcom/applovin/impl/mediation/ads/MaxFullscreenAdImpl${'$'}a;
    instance-of p1, p1, Lcom/applovin/mediation/ads/MaxRewardedAd;
    if-eqz p1, :amazego_not_rewarded
    instance-of p1, v0, Lcom/applovin/mediation/MaxRewardedAdListener;
    if-eqz p1, :amazego_not_rewarded
    check-cast v0, Lcom/applovin/mediation/MaxRewardedAdListener;
    const/4 p1, 0x1
    const-string p2, ""
    invoke-static {p1, p2}, Lcom/applovin/impl/mediation/MaxRewardImpl;->create(ILjava/lang/String;)Lcom/applovin/mediation/MaxReward;
    move-result-object p1
    invoke-interface {v0, v1}, Lcom/applovin/mediation/MaxAdListener;->onAdDisplayed(Lcom/applovin/mediation/MaxAd;)V
    invoke-interface {v0, v1, p1}, Lcom/applovin/mediation/MaxRewardedAdListener;->onUserRewarded(Lcom/applovin/mediation/MaxAd;Lcom/applovin/mediation/MaxReward;)V
    invoke-interface {v0, v1}, Lcom/applovin/mediation/MaxAdListener;->onAdHidden(Lcom/applovin/mediation/MaxAd;)V
    return-void
    :amazego_not_rewarded
    invoke-interface {v0, v1}, Lcom/applovin/mediation/MaxAdListener;->onAdDisplayed(Lcom/applovin/mediation/MaxAd;)V
    invoke-interface {v0, v1}, Lcom/applovin/mediation/MaxAdListener;->onAdHidden(Lcom/applovin/mediation/MaxAd;)V
    return-void
    :amazego_return
    return-void
    :amazego_original
""".trimIndent()

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Blocks AppLovin MAX interstitials, app-open ads and banners. " +
        "Rewarded ads grant their reward without playing the video.",
    default = true
) {
    compatibleWith(COMPATIBILITY_AMAZE_GO)

    execute {
        // Fullscreen: interstitials, app open y rewarded.
        MaxFullscreenShowAdFingerprint.method.addInstructions(0, FAKE_FULLSCREEN_SMALI)
        MaxFullscreenShowAdWithViewFingerprint.method.addInstructions(0, FAKE_FULLSCREEN_SMALI)

        // Banners: no cargar ni refrescar.
        MaxAdViewLoadAdFingerprint.method.addInstructions(0, "return-void")
        MaxAdViewStartAutoRefreshFingerprint.method.addInstructions(0, "return-void")
    }
}
