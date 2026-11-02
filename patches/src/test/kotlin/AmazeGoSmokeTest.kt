import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

private const val PKG = "com.oakever.arrows"
private const val FULLSCREEN = "Lcom/applovin/impl/mediation/ads/MaxFullscreenAdImpl;"
private const val AD_VIEW = "Lcom/applovin/mediation/ads/MaxAdView;"

class AmazeGoSmokeTest {

    @TempDir
    lateinit var workDir: File

    @Test
    fun `bundle contains Remove ads for Amaze GO`() {
        val root = repoRoot()
        val bundle = newestPatchBundle(root)
        assumeTrue(bundle != null, "skipping: no .mpp bundle yet, run ./gradlew :patches:buildAndroid first")

        val patches = loadAllPatches(bundle!!)
        val patch = patches.singleOrNull {
            it.name == "Remove ads" &&
                it.compatibility.orEmpty().any { compat -> compat.packageName == PKG }
        } ?: error("Remove ads for $PKG not found in bundle, found ${patches.map { it.name }}")

        val compat = patch.compatibility.orEmpty().singleOrNull { it.packageName == PKG }
            ?: error("Remove ads is not compatible with $PKG")
        assertEquals("Amaze GO!", compat.name)
        assertTrue(compat.targets.any { it.version == "1.37.0" }, "target 1.37.0 missing")
    }

    @Test
    fun `Remove ads fakes MAX fullscreen callbacks and disables banners`() {
        val root = repoRoot()
        val bundle = newestPatchBundle(root)
        assumeTrue(bundle != null, "skipping: no .mpp bundle yet, run ./gradlew :patches:buildAndroid first")

        val apk = File(root, "apks/amazego/base.apk")
        assumeTrue(apk.exists(), "skipping: base.apk not present at ${apk.path}")

        val classes = applyPatches(
            apk = apk,
            workDir = workDir,
            pkg = PKG,
            patchNames = setOf("Remove ads"),
            allPatches = loadAllPatches(bundle!!),
        )
        val byType = classes.associateBy { it.type }

        val fullscreen = byType[FULLSCREEN]
            ?: error("$FULLSCREEN not found after patching")

        assertFakeFullscreen(
            fullscreen.method(
                "showAd",
                listOf("Ljava/lang/String;", "Ljava/lang/String;", "Landroid/app/Activity;"),
            ),
            "showAd(String,String,Activity)",
        )
        assertFakeFullscreen(
            fullscreen.method(
                "showAd",
                listOf(
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Landroid/view/ViewGroup;",
                    "Landroidx/lifecycle/Lifecycle;",
                    "Landroid/app/Activity;",
                ),
            ),
            "showAd(String,String,ViewGroup,Lifecycle,Activity)",
        )

        val adView = byType[AD_VIEW]
            ?: error("$AD_VIEW not found after patching")
        assertEquals(
            Opcode.RETURN_VOID,
            adView.method("loadAd").instructions().firstOrNull()?.opcode,
            "MaxAdView.loadAd must be a no-op",
        )
        assertEquals(
            Opcode.RETURN_VOID,
            adView.method("startAutoRefresh").instructions().firstOrNull()?.opcode,
            "MaxAdView.startAutoRefresh must be a no-op",
        )
    }

    private fun assertFakeFullscreen(method: Method, label: String) {
        val first = method.instructions().firstOrNull() as? ReferenceInstruction
        val field = first?.reference as? FieldReference
        assertTrue(
            first?.opcode == Opcode.IGET_OBJECT &&
                field?.definingClass == "Lcom/applovin/impl/mediation/ads/a;" &&
                field.name == "adListener",
            "$label must start by reading adListener; got ${first?.opcode} $field",
        )

        assertTrue(
            method.instructions().any { insn ->
                insn is ReferenceInstruction &&
                    insn.opcode == Opcode.INVOKE_INTERFACE &&
                    (insn.reference as? MethodReference)?.name == "onUserRewarded"
            },
            "$label must call onUserRewarded",
        )
        assertTrue(
            method.instructions().any { insn ->
                insn is ReferenceInstruction &&
                    insn.opcode == Opcode.INVOKE_INTERFACE &&
                    (insn.reference as? MethodReference)?.name == "onAdHidden"
            },
            "$label must call onAdHidden",
        )
    }
}
