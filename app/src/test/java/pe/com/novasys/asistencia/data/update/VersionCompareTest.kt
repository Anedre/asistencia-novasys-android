package pe.com.novasys.asistencia.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.com.novasys.asistencia.data.update.UpdateRepository.Companion.compareVersions

class VersionCompareTest {

    @Test
    fun `newer patch, minor and major win`() {
        assertTrue(compareVersions("0.1.1", "0.1.0") > 0)
        assertTrue(compareVersions("0.2.0", "0.1.9") > 0)
        assertTrue(compareVersions("1.0.0", "0.99.99") > 0)
    }

    @Test
    fun `components compare as numbers, not text`() {
        // As strings "0.10.0" < "0.9.0"; as versions it is newer.
        assertTrue(compareVersions("0.10.0", "0.9.0") > 0)
    }

    @Test
    fun `missing components count as zero`() {
        assertEquals(0, compareVersions("1.2", "1.2.0"))
        assertTrue(compareVersions("1.2.1", "1.2") > 0)
    }

    @Test
    fun `pre-release and build suffixes are ignored`() {
        assertEquals(0, compareVersions("0.2.0-debug", "0.2.0"))
        assertEquals(0, compareVersions("0.2.0+15", "0.2.0"))
    }

    @Test
    fun `an older release is never offered`() {
        assertTrue(compareVersions("0.1.0", "0.2.0") < 0)
    }
}
