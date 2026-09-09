package expo.modules.pdfbitmap

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.io.File

class PdfBitmapModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("PdfBitmap")

    AsyncFunction("renderPdfToBitmap") { base64Pdf: String, dpi: Int ->
      withRenderer(base64Pdf) { renderer ->
        if (renderer.pageCount == 0) throw Exception("PDF has no pages")
        renderPage(renderer, 0, dpi)
      }
    }

    AsyncFunction("renderPdfToBitmaps") { base64Pdf: String, dpi: Int ->
      withRenderer(base64Pdf) { renderer ->
        if (renderer.pageCount == 0) throw Exception("PDF has no pages")
        // Сторінки рендеряться по черзі: PdfRenderer не дозволяє тримати
        // відкритими дві сторінки одночасно, та й тримати в пам'яті всі
        // ARGB-бітмапи багатомісної ТТН немає потреби.
        (0 until renderer.pageCount).map { index -> renderPage(renderer, index, dpi) }
      }
    }
  }

  /** Розпаковує base64 PDF у тимчасовий файл і віддає відкритий PdfRenderer. */
  private fun <T> withRenderer(base64Pdf: String, block: (PdfRenderer) -> T): T {
    val pdfBytes = Base64.decode(base64Pdf, Base64.DEFAULT)

    val cacheDir = appContext.reactContext?.cacheDir ?: throw Exception("No react context")
    val tempFile = File.createTempFile("pdf_render_", ".pdf", cacheDir)
    try {
      tempFile.writeBytes(pdfBytes)

      val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
      val renderer = PdfRenderer(fd)

      try {
        return block(renderer)
      } finally {
        renderer.close()
        fd.close()
      }
    } finally {
      tempFile.delete()
    }
  }

  /** Рендерить одну сторінку в упакований 1-бітний bitmap для TSPL. */
  private fun renderPage(renderer: PdfRenderer, index: Int, dpi: Int): Map<String, Any> {
    val page = renderer.openPage(index)

    val scale = dpi.toFloat() / 72f
    val width = (page.width * scale).toInt()
    val height = (page.height * scale).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.WHITE)

    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
    page.close()

    // Pack pixels into 1-bit TSPL bitmap (MSB first, 0=black, 1=white)
    val widthBytes = (width + 7) / 8
    val bitmapData = ByteArray(widthBytes * height)
    val row = IntArray(width)

    for (y in 0 until height) {
      bitmap.getPixels(row, 0, width, 0, y, width, 1)
      for (col in 0 until widthBytes) {
        var byte = 0
        for (bit in 0 until 8) {
          val px = col * 8 + bit
          if (px < width) {
            val pixel = row[px]
            // Convert to grayscale
            val gray = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114).toInt()
            // gray > 127 = white → bit 1
            if (gray > 127) {
              byte = byte or (1 shl (7 - bit))
            }
          } else {
            byte = byte or (1 shl (7 - bit)) // padding = white
          }
        }
        bitmapData[y * widthBytes + col] = byte.toByte()
      }
    }

    bitmap.recycle()

    return mapOf(
      "bitmap" to Base64.encodeToString(bitmapData, Base64.NO_WRAP),
      "widthBytes" to widthBytes,
      "widthDots" to width,
      "heightDots" to height,
      "pageIndex" to index,
    )
  }
}
