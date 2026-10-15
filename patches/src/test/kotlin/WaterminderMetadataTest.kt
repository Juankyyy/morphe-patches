import app.juankyyy.patches.waterminder.COMPATIBILITY_WATERMINDER
import app.morphe.patcher.patch.ApkFileType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Metadata-only test: needs no APK. Validates the WaterMinder scaffolding
 * until the real bytecode patch + smoke test land.
 */
class WaterminderMetadataTest {

    @Test
    fun `WaterMinder compatibility declares XAPK target`() {
        assertEquals("com.funnmedia.waterminder", COMPATIBILITY_WATERMINDER.packageName)
        assertEquals("WaterMinder", COMPATIBILITY_WATERMINDER.name)
        assertEquals(ApkFileType.XAPK, COMPATIBILITY_WATERMINDER.apkFileType)
        assertTrue(
            COMPATIBILITY_WATERMINDER.targets.any { it.version == "5.10.10" },
            "target 5.10.10 missing"
        )
    }
}
