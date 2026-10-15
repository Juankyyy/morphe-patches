package app.juankyyy.patches.waterminder

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.util.Base64

internal const val SIGNATURE_SPOOF_EXTENSION_CLASS = "Lapp/template/extension/signature/SignatureSpoof;"
private const val PACKAGE_MANAGER = "Landroid/content/pm/PackageManager;"
private const val EXTENSION_PACKAGE_PREFIX = "Lapp/template/extension/"

/** Original FunnMedia signing certificate, DER encoded in base64 (from the 5.10.10 base.apk). */
internal const val WATERMINDER_CERTIFICATE_BASE64 =
    "MIIDUDCCAjigAwIBAgIDfw+ZMA0GCSqGSIb3DQEBCwUAMFkxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJJTDERMA8GA1UEBxMIT2FrIExhd24xEzARBgNVBAoTCkZ1bm4gTWVkaWExFTATBgNVBAMTDEtyaXNzIFNtb2xrYTAeFw0xNTAzMTYyMDU1MTJaFw00MDAzMDkyMDU1MTJaMFkxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJJTDERMA8GA1UEBxMIT2FrIExhd24xEzARBgNVBAoTCkZ1bm4gTWVkaWExFTATBgNVBAMTDEtyaXNzIFNtb2xrYTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALWkDOrU1qH0eh6NQWhIhD16Ms0apdoDWAUNpI77Vc6WsZqFGEuAWd1puKWopSd9Z43Q6RMKGr7geyZ8k9CNJ+oKTFHvcvTAG6MolZINjm0dqa6neA0hU6UgT9qvHTUykQdSVKsACIg1iOCw/5E7JuWui+iZaWevzbHX/du8DqSvCSL6yEZz/PNw3x3Zlivf2nMytdthdtE1sfDPpQwr2icsgteoZvgVmseSN6a71rSQfa2eNnLKyGCJZWFkTJRt2zfer2unQapTeSmRXXUwbUEy5fdZrHdutw04/TkVfYfOn3Dnvoy6ERDWz0pT1MnmmQz5wXmFV0KGVa1SfWOfZbsCAwEAAaMhMB8wHQYDVR0OBBYEFBt8d6dqJOTqJepK+kF9QU6MW6AkMA0GCSqGSIb3DQEBCwUAA4IBAQAu3W2PCrV5JQm7Gxlpxaid/ph6ePrDdN1Wrf8ioWwYY3Crw1PUIPlZ3P/JpFT9ZKkzuE3r8fu7sY/wG08rfqbHf9RWWVzOyLBEacUNJW8aFa9b3UQJWouIhNYnrjhsg4cQ3fTEcC4UIsZ+gtpQCirWAHWMVv/5sRAm3vETKcIFZK242PlqyB5cT4Cg98lqbpsgEIx67yWPIpglcvAqEjOoesuCOk+mA50otbFKxOUyyfQhHKVqyC8/etuyWhXt9YJe476PlVko1v8d66PXOAQez4H8Kt3qxnu3lZgRDLpdSm6CUhU/zrNicfR7rp/RHWNJ7psgqjuSpYhx5e1r3L34"

/** Replaces the method body with an immediate `return "<value>"`. */
private fun MutableMethod.returnConstString(value: String) {
    check(!value.contains('"')) { "value cannot contain quotes" }
    val count = implementation!!.instructions.size
    removeInstructions(0, count)
    addInstructions(0, "const-string v0, \"$value\"\nreturn-object v0")
}

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = "Reports the original signing certificate to Google/Firebase SDK checks inside the app. Does not fix Google sign-in: that verification happens on Google's servers, which see the re-signed certificate.",
    default = false
) {
    compatibleWith(COMPATIBILITY_WATERMINDER_EXPERIMENTAL)

    extendWith("extensions/signature.mpe")

    execute {
        // Fail at bundle load, not on the user's device, when the certificate is not a certificate.
        runCatching {
            CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(Base64.getDecoder().decode(WATERMINDER_CERTIFICATE_BASE64)))
        }.getOrElse { throw IllegalArgumentException("Invalid certificate for WaterMinder", it) }

        mutableClassDefBy(SIGNATURE_SPOOF_EXTENSION_CLASS).methods.apply {
            single { it.name == "getSpoofedPackageName" }
                .returnConstString("com.funnmedia.waterminder")
            single { it.name == "getSpoofedCertificate" }
                .returnConstString(WATERMINDER_CERTIFICATE_BASE64)
        }

        // Collect first: mutating classes while iterating them is not allowed.
        val targets = mutableListOf<ClassDef>()
        classDefForEach { classDef ->
            if (classDef.type.startsWith(EXTENSION_PACKAGE_PREFIX)) return@classDefForEach
            if (classDef.methods.any { m -> m.implementation?.instructions?.any { it.isGetPackageInfo() } == true }) {
                targets += classDef
            }
        }
        if (targets.isEmpty()) throw PatchException("No PackageManager.getPackageInfo call found")

        for (classDef in targets) {
            for (method in mutableClassDefBy(classDef).methods) {
                val instructions = method.implementation?.instructions?.toList() ?: continue
                instructions.forEachIndexed { index, instruction ->
                    if (!instruction.isGetPackageInfo()) return@forEachIndexed
                    val call = (instruction as ReferenceInstruction).reference as MethodReference
                    val descriptor = "$SIGNATURE_SPOOF_EXTENSION_CLASS->getPackageInfo(" +
                        PACKAGE_MANAGER + call.parameterTypes.joinToString("") + ")" + call.returnType
                    val replacement = when (instruction) {
                        is RegisterRangeInstruction -> {
                            val last = instruction.startRegister + instruction.registerCount - 1
                            "invoke-static/range { v${instruction.startRegister} .. v$last }, $descriptor"
                        }
                        is FiveRegisterInstruction -> {
                            val registers = listOf(
                                instruction.registerC, instruction.registerD, instruction.registerE,
                                instruction.registerF, instruction.registerG,
                            ).take(instruction.registerCount)
                            "invoke-static { ${registers.joinToString { "v$it" }} }, $descriptor"
                        }
                        else -> throw PatchException("Unexpected invoke format ${instruction.opcode}")
                    }
                    method.replaceInstruction(index, replacement)
                }
            }
        }
    }
}

/** `invoke-virtual PackageManager.getPackageInfo(String, int | PackageInfoFlags)`. */
private fun Instruction.isGetPackageInfo(): Boolean {
    if (opcode != Opcode.INVOKE_VIRTUAL && opcode != Opcode.INVOKE_VIRTUAL_RANGE) return false
    val call = (this as ReferenceInstruction).reference as? MethodReference ?: return false
    return call.definingClass == PACKAGE_MANAGER && call.name == "getPackageInfo" &&
        call.returnType == "Landroid/content/pm/PackageInfo;" &&
        call.parameterTypes.firstOrNull()?.toString() == "Ljava/lang/String;"
}
