import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

private const val PKG = "com.hungrybolo.remotemouseandroid"
private const val PAYWALL_STATE = "Lcom/hungrybolo/remotemouseandroid/purchase/paywall/PaywallScreenState;"
private const val ENTITLEMENTS = "Lcom/hungrybolo/remotemouseandroid/purchase/Entitlements;"
private const val GLOBAL_VARS = "Lcom/hungrybolo/remotemouseandroid/utils/GlobalVars;"

class RemoteMouseSmokeTest {

    @TempDir
    lateinit var workDir: File

    @Test
    fun `bundle contains Unlock PRO for Remote Mouse`() {
        val root = repoRoot()
        val bundle = newestPatchBundle(root)
        assumeTrue(bundle != null, "skipping: no .mpp bundle yet, run ./gradlew :patches:buildAndroid first")

        val patches = loadAllPatches(bundle!!)
        val patch = patches.singleOrNull { it.name == "Unlock PRO" }
            ?: error("Unlock PRO not found in bundle, found ${patches.map { it.name }}")

        val compat = patch.compatibility.orEmpty().singleOrNull { it.packageName == PKG }
            ?: error("Unlock PRO is not compatible with $PKG")
        assertEquals("Remote Mouse", compat.name)
        assertTrue(compat.targets.any { it.version == "5.315" }, "target 5.315 missing")
    }

    @Test
    fun `Unlock PRO forces FULL_ACCESS and global entitlement flags`() {
        val root = repoRoot()
        val bundle = newestPatchBundle(root)
        assumeTrue(bundle != null, "skipping: no .mpp bundle yet, run ./gradlew :patches:buildAndroid first")
        // The APK lives in the gitignored apks/ directory (base.apk extracted from the APKM).
        // Skip rather than fail when absent, so the test suite stays green without copyrighted inputs.
        val apk = File(root, "apks/remotemouse/base.apk")
        assumeTrue(apk.exists(), "skipping: base.apk not present at ${apk.path}")

        val classes = applyPatches(
            apk = apk,
            workDir = workDir,
            pkg = PKG,
            patchNames = setOf("Unlock PRO"),
            allPatches = loadAllPatches(bundle!!),
        )
        val byType = classes.associateBy { it.type }

        // 1. Paywall UI gate -> FULL_ACCESS.
        val paywall = byType[PAYWALL_STATE] ?: error("$PAYWALL_STATE not found")
        val derive = paywall.method("derive", listOf("Z", "Z", "Z", "Z", "Z", "Z", "Z"))
        assertStartsWithSgetObject(derive, PAYWALL_STATE, "FULL_ACCESS", "PaywallScreenState.derive")

        // 2. Real feature flags -> all true.
        val entitlements = byType[ENTITLEMENTS] ?: error("$ENTITLEMENTS not found")
        val recompute = entitlements.method("recompute", emptyList())
        assertStartsWithConstTrue(recompute, "Entitlements.recompute")

        val sputBooleans = recompute.instructions()
            .filter { it.opcode == Opcode.SPUT_BOOLEAN }
            .map { ((it as ReferenceInstruction).reference as FieldReference).name }
            .toSet()
        assertTrue(
            "isRemoveAds" in sputBooleans && "isBuyMedia" in sputBooleans,
            "Entitlements.recompute must force GlobalVars flags; sput-boolean fields: $sputBooleans"
        )
        assertTrue(
            recompute.instructions().any { it.opcode == Opcode.RETURN_VOID },
            "Entitlements.recompute must contain return-void"
        )
        assertTrue(
            GLOBAL_VARS in recompute.instructions()
                .mapNotNull { ((it as? ReferenceInstruction)?.reference as? FieldReference)?.definingClass }
                .toSet(),
            "Entitlements.recompute must write to $GLOBAL_VARS"
        )
    }
}
