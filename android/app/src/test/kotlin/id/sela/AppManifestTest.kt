package id.sela
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
class AppManifestTest {
    private fun manifest() = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
    private val android = "http://schemas.android.com/apk/res/android"
    @Test fun launcherBrandIsOnlySela() {
        val app = manifest().getElementsByTagName("application").item(0) as org.w3c.dom.Element
        assertEquals("SELA", app.getAttributeNS(android, "label"))
        assertEquals(".SelaApplication", app.getAttributeNS(android, "name"))
    }
    @Test fun manifestContainsLauncherButNoBackgroundLocation() {
        val doc = manifest()
        val permissions = doc.getElementsByTagName("uses-permission")
        val names = (0 until permissions.length).map { (permissions.item(it) as org.w3c.dom.Element).getAttributeNS(android, "name") }
        assertTrue("android.permission.INTERNET" in names)
        assertFalse("android.permission.ACCESS_BACKGROUND_LOCATION" in names)
        val activity = doc.getElementsByTagName("activity").item(0) as org.w3c.dom.Element
        assertEquals("true", activity.getAttributeNS(android, "exported"))
        val category = doc.getElementsByTagName("category").item(0) as org.w3c.dom.Element
        assertEquals("android.intent.category.LAUNCHER", category.getAttributeNS(android, "name"))
    }
}
