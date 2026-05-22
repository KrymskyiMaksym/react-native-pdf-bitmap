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
      val pdfBytes = Base64.decode(base64Pdf, Base64.DEFAULT)

      val tempFile = File.createTempFile("pdf_render_", ".pdf", context.cacheDir)
      try {
        tempFile.writeBytes(pdfBytes)

        val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)

        try {
          val page = renderer.openPage(0)

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

          for (row in 0 until height) {
            for (col in 0 until widthBytes) {
              var byte = 0
              for (bit in 0 until 8) {
                val px = col * 8 + bit
                if (px < width) {
                  val pixel = bitmap.getPixel(px, row)
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
              bitmapData[row * widthBytes + col] = byte.toByte()
            }
          }

          bitmap.recycle()

          mapOf(
            "bitmap" to Base64.encodeToString(bitmapData, Base64.NO_WRAP),
            "widthBytes" to widthBytes,
            "widthDots" to width,
            "heightDots" to height
          )
        } finally {
          renderer.close()
          fd.close()
        }
      } finally {
        tempFile.delete()
      }
    }
  }
}
