package com.nosferatu.launcher.parser

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class TocParserTest {

    private fun addEntry(zos: ZipOutputStream, name: String, content: String) {
        val entry = ZipEntry(name)
        zos.putNextEntry(entry)
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    @Test
    fun parseNavXhtml() {
      runBlocking {
        val tmp = File.createTempFile("test_nav", ".epub")
        ZipOutputStream(FileOutputStream(tmp)).use { zos ->
            val containerXml = """
                <?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OPS/package.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
            """.trimIndent()

            val packageOpf = """
                <?xml version="1.0" encoding="utf-8"?>
                <package version="3.0" xmlns="http://www.idpf.org/2007/opf">
                  <manifest>
                    <item id="nav" href="nav.xhtml" properties="nav" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                  </spine>
                </package>
            """.trimIndent()

            val navXhtml = """
                <?xml version="1.0" encoding="utf-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
                  <body>
                    <nav epub:type="toc">
                      <ol>
                        <li><a href="chapter1.xhtml">Chapter 1</a></li>
                        <li><a href="chapter2.xhtml">Chapter 2</a></li>
                      </ol>
                    </nav>
                  </body>
                </html>
            """.trimIndent()

            addEntry(zos, "META-INF/container.xml", containerXml)
            addEntry(zos, "OPS/package.opf", packageOpf)
            addEntry(zos, "OPS/nav.xhtml", navXhtml)
        }

          val parser = TocParser()
          val items = parser.parse(tmp)
          assertEquals(2, items.size)
          assertEquals("Chapter 1", items[0].title)
          assertEquals("chapter1.xhtml", items[0].href)
          assertEquals("Chapter 2", items[1].title)
          tmp.delete()
        }
      }

    @Test
    fun parseNcx() {
      runBlocking {
        val tmp = File.createTempFile("test_ncx", ".epub")
        ZipOutputStream(FileOutputStream(tmp)).use { zos ->
            val containerXml = """
                <?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OPS/package.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
            """.trimIndent()

            val packageOpf = """
                <?xml version="1.0" encoding="utf-8"?>
                <package version="2.0" xmlns="http://www.idpf.org/2007/opf">
                  <manifest>
                    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                  </manifest>
                </package>
            """.trimIndent()

            val ncx = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
                  <navMap>
                    <navPoint id="np1"><navLabel><text>Chapter A</text></navLabel><content src="a.xhtml"/></navPoint>
                    <navPoint id="np2"><navLabel><text>Chapter B</text></navLabel><content src="b.xhtml"/></navPoint>
                  </navMap>
                </ncx>
            """.trimIndent()

            addEntry(zos, "META-INF/container.xml", containerXml)
            addEntry(zos, "OPS/package.opf", packageOpf)
            addEntry(zos, "OPS/toc.ncx", ncx)
        }

          val parser = TocParser()
          val items = parser.parse(tmp)
          assertEquals(2, items.size)
          assertEquals("Chapter A", items[0].title)
          assertEquals("a.xhtml", items[0].href)
          assertEquals("Chapter B", items[1].title)
          tmp.delete()
        }
      }
}
