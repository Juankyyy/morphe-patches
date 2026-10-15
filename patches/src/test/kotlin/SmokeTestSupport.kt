import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.loadPatchesFromJar
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.nio.ByteBuffer

/**
 * Minimal shared helpers for per-app smoke tests.
 *
 * Each test applies the app's patch to the real APK in the gitignored `apks/`
 * directory (e.g. `apks/remotemouse/base.apk`) and asserts on the emitted bytecode.
 * Tests skip (rather than fail) when the APK or the patch bundle is absent,
 * so CI stays green; locally, with the files present, the test really runs.
 *
 * Adapted to morphe-patcher 1.15.1: [PatcherConfig] only takes the APK file and
 * a temp dir (no pkg/version parameters).
 */
fun repoRoot(): File {
    var dir = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
    return dir ?: File(System.getProperty("user.dir")).absoluteFile
}

/** Newest built patch bundle (.mpp), or null when the project has not been built yet. */
fun newestPatchBundle(root: File): File? =
    File(root, "patches/build/libs").walkTopDown()
        .filter {
            it.isFile && it.extension == "mpp" &&
                !it.name.contains("sources") && !it.name.contains("javadoc")
        }
        .maxByOrNull { it.lastModified() }

fun loadAllPatches(bundle: File): Set<Patch<*>> = loadPatchesFromJar(setOf(bundle))

fun Method.instructions(): List<Instruction> = implementation?.instructions?.toList().orEmpty()

fun ClassDef.method(name: String, parameters: List<String>? = null): Method =
    methods.firstOrNull { method ->
        method.name == name &&
            (parameters == null ||
                method.parameterTypes.map(CharSequence::toString) == parameters)
    } ?: error("$type.$name(${parameters?.joinToString()}) not found")

/**
 * Applies [patchNames] (filtered to patches compatible with [pkg]) to [apk]
 * and returns every class from the emitted dex files.
 */
fun applyPatches(
    apk: File,
    workDir: File,
    pkg: String,
    patchNames: Set<String>,
    allPatches: Set<Patch<*>>,
): List<ClassDef> {
    val target = allPatches.filter { patch ->
        patch.name in patchNames &&
            patch.compatibility.orEmpty().any { it.packageName == pkg }
    }.toList()

    assertTrue(
        target.map { it.name }.toSet() == patchNames,
        "expected patches $patchNames in the bundle, found ${target.map { it.name }}"
    )

    val config = PatcherConfig(
        apkFile = apk,
        temporaryFilesPath = workDir.resolve("morphe-tmp"),
    )

    // Patcher.invoke() only executes the patches. Patcher.get() finalizes the bytecode
    // context and emits the patched dex files - without it you re-read the unpatched input.
    val result = Patcher(config).use { patcher ->
        patcher += target.toSet()
        val results = runBlocking { patcher.invoke().toList() }
        val ours = results.filter { it.patch.name in patchNames }
        assertTrue(
            ours.size == patchNames.size,
            "every patch must execute exactly once; got ${results.map { it.patch.name }}"
        )
        for (r in ours) {
            assertNull(r.exception, "patch '${r.patch.name}' failed: ${r.exception}")
        }
        patcher.get()
    }

    assertTrue(result.dexFiles.isNotEmpty(), "patcher produced no patched dex files")

    // Drain every stream up front; path-backed dex files stay locked on Windows otherwise.
    return result.dexFiles.flatMap { dexFile ->
        val bytes = dexFile.stream.use { it.readBytes() }
        DexBackedDexFile(Opcodes.forApi(36), ByteBuffer.wrap(bytes)).classes.toList()
    }
}

/** Asserts the method starts with `sget-object` of [fieldName] from [definingClass]. */
fun assertStartsWithSgetObject(
    method: Method,
    definingClass: String,
    fieldName: String,
    label: String,
) {
    val first = method.instructions().getOrNull(0) as? ReferenceInstruction
    val ref = first?.reference as? FieldReference
    assertTrue(
        first != null && first.opcode == Opcode.SGET_OBJECT &&
            ref != null && ref.definingClass == definingClass && ref.name == fieldName,
        "$label must start with sget-object $definingClass->$fieldName; " +
            "got ${first?.opcode} $ref"
    )
    assertTrue(
        method.instructions().getOrNull(1)?.opcode == Opcode.RETURN_OBJECT,
        "$label must return the object immediately; " +
            "second instruction is ${method.instructions().getOrNull(1)?.opcode}"
    )
}

/** Asserts the method starts with `const/4 vX, 1` (forced boolean true). */
fun assertStartsWithConstTrue(method: Method, label: String) =
    assertStartsWithConstBoolean(method, expected = true, label = label)

/** Asserts the method starts with `const/4 vX, 0` (forced boolean false). */
fun assertStartsWithConstFalse(method: Method, label: String) =
    assertStartsWithConstBoolean(method, expected = false, label = label)

/** Asserts the method starts with `const/4 vX, 0/1` (forced boolean). */
fun assertStartsWithConstBoolean(method: Method, expected: Boolean, label: String) {
    val first = method.instructions().getOrNull(0)
    assertTrue(
        first is NarrowLiteralInstruction &&
            first.opcode == Opcode.CONST_4 && first.narrowLiteral == if (expected) 1 else 0,
        "$label must start with const/4 ${if (expected) "0x1" else "0x0"}; got ${first?.opcode}"
    )
}
