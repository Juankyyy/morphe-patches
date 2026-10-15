package app.juankyyy.patches.waterminder

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

/**
 * RevenueCat: EntitlementInfo.isActive() lee el campo isActive del servidor.
 * Forzarlo a true hace que todos los entitlements parezcan activos.
 */
object RevenueCatEntitlementActiveFingerprint : Fingerprint(
    definingClass = "Lcom/revenuecat/purchases/EntitlementInfo;",
    name = "isActive",
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.IGET_BOOLEAN),
        opcode(Opcode.RETURN)
    )
)

/**
 * Superwall: SubscriptionStatus.isActive() es `this is Active`.
 * Forzarlo a true hace que la app crea que la suscripción está activa,
 * que es lo que observa WMApplication (SuperwallDelegate).
 */
object SuperwallSubscriptionActiveFingerprint : Fingerprint(
    definingClass = "Lcom/superwall/sdk/models/entitlements/SubscriptionStatus;",
    name = "isActive",
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.INSTANCE_OF),
        opcode(Opcode.RETURN)
    )
)

/**
 * Gates de la app (nombres ofuscados por R8, válidos para 5.10.10):
 * cada enum de features implementa Lk99.isPremiumFeature() con un switch
 * sobre ordinal() que marca algunas entradas como premium.
 * - Lbk8: tipos de recordatorio (INTERVAL, STATIC, WATER_LEVEL).
 * - Lle2: tipos de taza (MULTI_INGREDIENTS, NORMAL).
 * - Lq56: items del menú de ajustes (Export, HealthConnect, Reminders...).
 * Forzarlos a false trata todo como no-premium: sin candado y sin paywall.
 */
object ReminderTypePremiumFingerprint : Fingerprint(
    definingClass = "Lbk8;",
    name = "isPremiumFeature",
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.AGET),
        opcode(Opcode.RETURN)
    )
)

object CupTypePremiumFingerprint : Fingerprint(
    definingClass = "Lle2;",
    name = "isPremiumFeature",
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.AGET),
        opcode(Opcode.RETURN)
    )
)

object SettingsMenuPremiumFingerprint : Fingerprint(
    definingClass = "Lq56;",
    name = "isPremiumFeature",
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.AGET),
        opcode(Opcode.RETURN)
    )
)

/**
 * PairIP: LicenseClient.checkLicense() es el punto único de entrada del
 * check de licencia de Play. Lo llama Application.attachBaseContext y
 * LicenseContentProvider.onCreate al arrancar. Anularlo (return-void)
 * evita el check local de instalador, la conexión al servicio de
 * licencias de Play, los reintentos y la pantalla de "Descarga esta app".
 */
object PairipLicenseCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "checkLicense",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        opcode(Opcode.CONST_STRING),
        opcode(Opcode.RETURN_VOID)
    )
)
