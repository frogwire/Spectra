package app.frogwire.spectra.downloads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** File names come from the website, so they are cleaned before anything is saved. */
class SafeFileNameTest {

    private fun safe(name: String) = DownloadController.safeFileName(name)

    @Test
    fun `ordinary names are kept`() {
        assertEquals("report.pdf", safe("report.pdf"))
        assertEquals("My Photo (1).jpg", safe("My Photo (1).jpg"))
    }

    @Test
    fun `folder paths are stripped`() {
        assertEquals("passwd", safe("../../etc/passwd"))
        assertEquals("evil.exe", safe("C:\\Windows\\evil.exe"))
    }

    @Test
    fun `hidden-file dots and reserved characters are removed`() {
        assertEquals("bashrc", safe(".bashrc"))
        assertEquals("ab.txt", safe("a<>:\"|?*b.txt"))
    }

    @Test
    fun `control characters are removed`() {
        assertEquals("ab.txt", safe("a\u0000\u0007b.txt"))
    }

    @Test
    fun `empty names get a default`() {
        assertEquals("download", safe(""))
        assertEquals("download", safe("..."))
        assertEquals("download", safe("/"))
    }

    @Test
    fun `long names are shortened but keep their extension`() {
        val name = safe("x".repeat(300) + ".pdf")
        assertTrue(name.length <= 120)
        assertTrue(name, name.endsWith(".pdf"))
    }
}
