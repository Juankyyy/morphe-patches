import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

private const val PKG = "com.funnmedia.waterminder"

class WaterminderSmokeTest {

    @TempDir
    lateinit var workDir: File

    @Test
    fun `Unlock Premium forces subscription active and clears feature gates`() {
        val root = repoRoot()
        val bundle = newestPatchBundle(root)
        assumeTrue(bundle != null, "skipping: no .mpp bundle yet, run ./gradlew :patches:buildAndroid first")
        // The APK lives in the gitignored apks/ directory (base.apk extracted from the XAPK).
        // Skip rather than fail when absent, so the test suite stays green without copyrighted inputs.
        val apk = File(root, "apks/waterminder/base.apk")
        assumeTrue(apk.exists(), "skipping: base.apk not present at ${apk.path}")

        val classes = applyPatches(
            apk = apk,
            workDir = workDir,
            pkg = PKG,
            patchNames = setOf("Unlock Premium"),
            allPatches = loadAllPatches(bundle!!),
        )
        val byType = classes.associateBy { it.type }

        // 1. RevenueCat entitlement -> active.
        val entitlementInfo = byType["Lcom/revenuecat/purchases/EntitlementInfo;"]
            ?: error("EntitlementInfo not found")
        assertStartsWithConstTrue(
            entitlementInfo.method("isActive", emptyList()),
            "EntitlementInfo.isActive"
        )

        // 2. Superwall subscription -> active.
        val subscriptionStatus = byType["Lcom/superwall/sdk/models/entitlements/SubscriptionStatus;"]
            ?: error("SubscriptionStatus not found")
        assertStartsWithConstTrue(
            subscriptionStatus.method("isActive", emptyList()),
            "SubscriptionStatus.isActive"
        )

        // 3. App feature gates -> not premium.
        for ((type, label) in listOf(
            "Lbk8;" to "reminder types",
            "Lle2;" to "cup types",
            "Lq56;" to "settings menu"
        )) {
            val cls = byType[type] ?: error("$type not found")
            assertStartsWithConstFalse(
                cls.method("isPremiumFeature", emptyList()),
                "$type.isPremiumFeature ($label)"
            )
        }
    }
}
