import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Assertions.assertTrue
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
            patchNames = setOf("Unlock Premium", "Remove license check", "Update check off by default"),
            allPatches = loadAllPatches(bundle!!),
        )
        val byType = classes.associateBy { it.type }

        // 0. PairIP license check -> skipped.
        val licenseClient = byType["Lcom/pairip/licensecheck/LicenseClient;"]
            ?: error("LicenseClient not found")
        assertStartsWithReturnVoid(
            licenseClient.method("checkLicense", listOf("Landroid/content/Context;")),
            "LicenseClient.checkLicense"
        )

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

        // 4. Purchase state -> purchased.
        val purchaseState = byType["Lnu7;"] ?: error("Lnu7 not found")
        assertStartsWithConstTrue(
            purchaseState.method("a", listOf("Lyv7;")),
            "Lnu7.a (purchase state)"
        )

        // 5. Update check default -> off, toggle still functional.
        assertUpdateCheckDefaultOff(byType)
    }
}

private const val UPDATE_CHECK_KEY = "check_inAppUpdate_enabled"

/** Index of `const-string key` in the method, or null. */
private fun ClassDef.keyIndex(methodName: String, params: List<String>, key: String): Int? {
    val m = try {
        method(methodName, params)
    } catch (e: IllegalStateException) {
        return null
    }
    val insns = m.instructions()
    for (i in insns.indices) {
        val ref = (insns[i] as? ReferenceInstruction)?.reference as? StringReference ?: continue
        if (ref.string == key) return i
    }
    return null
}

private fun const4LiteralAt(cls: ClassDef, methodName: String, params: List<String>, idx: Int): Int? {
    val insn = cls.method(methodName, params).instructions().getOrNull(idx) ?: return null
    if (insn.opcode != Opcode.CONST_4) return null
    return (insn as? NarrowLiteralInstruction)?.narrowLiteral
}

/** Verifies the update-check default flip in the 3 patched methods. */
private fun assertUpdateCheckDefaultOff(byType: Map<String, ClassDef>) {
    // 1. Toggle init: default right after the key is now 0.
    val toggle = byType["Lrk;"] ?: error("Lrk not found")
    val toggleKey = toggle.keyIndex("<init>", emptyList(), UPDATE_CHECK_KEY)
        ?: error("update-check key not found in Lrk.<init>")
    assertTrue(
        const4LiteralAt(toggle, "<init>", emptyList(), toggleKey + 1) == 0,
        "Lrk.<init> toggle default must be const/4 0x0"
    )

    // 2-3. MainActivity readers: zeroed default before Lfy1.C, restore only in onCreate.
    val main = byType["Lcom/funnmedia/waterminder/screen/activity/MainActivity;"]
        ?: error("MainActivity not found")
    for ((name, params, restore) in listOf(
        Triple("onCreate", listOf("Landroid/os/Bundle;"), true),
        Triple("onResume", emptyList(), false)
    )) {
        val insns = main.method(name, params).instructions()
        val callIdx = insns.indices.singleOrNull { i ->
            val ref = (insns[i] as? ReferenceInstruction)?.reference as? MethodReference ?: return@singleOrNull false
            if (ref.definingClass != "Lfy1;" || ref.name != "C") return@singleOrNull false
            ((i - 2)..(i - 1)).any { j ->
                j >= 0 && (((insns[j] as? ReferenceInstruction)?.reference as? StringReference)?.string == UPDATE_CHECK_KEY)
            }
        } ?: error("Lfy1.C update-check call not found in MainActivity.$name")
        assertTrue(
            const4LiteralAt(main, name, params, callIdx - 1) == 0,
            "MainActivity.$name must zero the update-check default before Lfy1.C"
        )
        if (restore) {
            // [const0, INVOKE, MOVE_RESULT, const1, ...].
            assertTrue(
                insns.getOrNull(callIdx + 1)?.opcode == Opcode.MOVE_RESULT,
                "MainActivity.$name MOVE_RESULT must directly follow Lfy1.C"
            )
            assertTrue(
                const4LiteralAt(main, name, params, callIdx + 2) == 1,
                "MainActivity.$name must restore the shared register after MOVE_RESULT"
            )
        }
    }
}
