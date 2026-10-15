package app.juankyyy.patches.waterminder

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val UPDATE_CHECK_KEY = "check_inAppUpdate_enabled"

/** Index of `const-string <KEY>` in the method. */
private fun MutableMethod.findKeyIndex(key: String): Int {
    val insns = implementation!!.instructions
    for (i in insns.indices) {
        val ref = (insns[i] as? ReferenceInstruction)?.reference as? StringReference ?: continue
        if (ref.string == key) return i
    }
    error("key $key not found")
}

/**
 * Zeroes the default (Z) argument of the `Lfy1.C(key, default)` call that
 * follows the key string. The Z register is shared with later code, so when
 * [restore] is true the original `0x1` is restored right after MOVE_RESULT
 * (which overwrites the result register, never the Z register).
 */
private fun MutableMethod.zeroLfy1Default(restore: Boolean) {
    val insns = implementation!!.instructions
    for (i in insns.indices) {
        val ref = (insns[i] as? ReferenceInstruction)?.reference as? MethodReference ?: continue
        if (ref.definingClass != "Lfy1;" || ref.name != "C") continue
        val keyBefore = ((i - 2)..(i - 1)).any { j ->
            j >= 0 && (((insns[j] as? ReferenceInstruction)?.reference as? StringReference)?.string == UPDATE_CHECK_KEY)
        }
        if (!keyBefore) continue
        val zReg = (insns[i] as FiveRegisterInstruction).registerE
        // Insert zero before the call: [const0, INVOKE, MOVE_RESULT, ...].
        addInstructions(i, "const/4 v$zReg, 0x0")
        if (restore) {
            // Restore after MOVE_RESULT: [..., INVOKE, MOVE_RESULT, const1, ...].
            addInstructions(i + 3, "const/4 v$zReg, 0x1")
        }
        return
    }
    error("Lfy1.C $UPDATE_CHECK_KEY call not found")
}

@Suppress("unused")
val updateCheckOffByDefaultPatch = bytecodePatch(
    name = "Update check off by default",
    description = "Disables automatic update checks by default. Can still be enabled in Settings.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WATERMINDER)

    execute {
        // 1. Toggle initial state: getBoolean default true -> false.
        UpdateCheckToggleFingerprint.method.apply {
            val keyIdx = findKeyIndex(UPDATE_CHECK_KEY)
            val def = implementation!!.instructions[keyIdx + 1]
            check(def is NarrowLiteralInstruction && def.opcode == Opcode.CONST_4 && def.narrowLiteral == 1) {
                "unexpected update-check toggle default"
            }
            val reg = (def as OneRegisterInstruction).registerA
            replaceInstruction(keyIdx + 1, "const/4 v$reg, 0x0")
        }

        // 2-3. MainActivity readers: default true -> false, restoring the shared register.
        // onResume needs no restore: its Z register is overwritten by MOVE_RESULT.
        UpdateCheckOnCreateFingerprint.method.zeroLfy1Default(restore = true)
        UpdateCheckOnResumeFingerprint.method.zeroLfy1Default(restore = false)
    }
}
