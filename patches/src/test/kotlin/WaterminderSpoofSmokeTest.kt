import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.security.MessageDigest
import java.util.Base64

private const val PKG = "com.funnmedia.waterminder"
private const val EXTENSION = "Lapp/template/extension/signature/SignatureSpoof;"
private const val PACKAGE_MANAGER = "Landroid/content/pm/PackageManager;"

// SHA-1 of FunnMedia's original signing certificate (matches the APKPure fingerprint).
private const val ORIGINAL_CERT_SHA1 = "b9145f8b2b6b8639d2006835fc135994e5610f75"

class WaterminderSpoofSmokeTest {

    @TempDir
    lateinit var workDir: File

    @Test
    fun `Spoof signature routes getPackageInfo through the extension`() {
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
            patchNames = setOf("Spoof signature"),
            allPatches = loadAllPatches(bundle!!),
        )
        val byType = classes.associateBy { it.type }

        // 1. The extension is merged with our package name and certificate injected.
        val extension = byType[EXTENSION] ?: error("$EXTENSION not merged into the emitted dexes")
        assertStartsWithConstString(
            extension.method("getSpoofedPackageName", emptyList()), PKG, "getSpoofedPackageName"
        )
        val certMethod = extension.method("getSpoofedCertificate", emptyList())
        val certB64 = ((certMethod.instructions().getOrNull(0) as? ReferenceInstruction)
            ?.reference as? com.android.tools.smali.dexlib2.iface.reference.StringReference)?.string
            ?: error("getSpoofedCertificate does not start with const-string")
        val sha1 = MessageDigest.getInstance("SHA-1").digest(Base64.getDecoder().decode(certB64))
            .joinToString("") { "%02x".format(it) }
        assertTrue(sha1 == ORIGINAL_CERT_SHA1, "spoofed certificate SHA-1 is $sha1, expected $ORIGINAL_CERT_SHA1")

        // 2. No direct PackageManager.getPackageInfo(String, ...) call is left outside the extension.
        val direct = mutableListOf<String>()
        var routed = 0
        for (cls in classes) {
            if (cls.type.startsWith("Lapp/template/extension/")) continue
            for (m in cls.methods) for (insn in m.instructions()) {
                val call = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (call.definingClass == PACKAGE_MANAGER && call.name == "getPackageInfo" &&
                    call.parameterTypes.firstOrNull()?.toString() == "Ljava/lang/String;"
                ) {
                    direct += "${cls.type}.${m.name}"
                }
                if (call.definingClass == EXTENSION && call.name == "getPackageInfo") routed++
            }
        }
        assertTrue(direct.isEmpty(), "direct getPackageInfo calls left:\n${direct.joinToString("\n")}")
        assertTrue(routed > 0, "no getPackageInfo calls routed through the extension")
    }
}
