import ExpoModulesCore
import PDFKit

public class PdfBitmapModule: Module {
  public func definition() -> ModuleDefinition {
    Name("PdfBitmap")

    AsyncFunction("renderPdfToBitmap") { (base64Pdf: String, dpi: Int) -> [String: Any] in
      guard let pdfData = Data(base64Encoded: base64Pdf) else {
        throw Exception(name: "ERR_INVALID_PDF", description: "Invalid base64 PDF data")
      }

      guard let document = PDFDocument(data: pdfData) else {
        throw Exception(name: "ERR_PDF_PARSE", description: "Failed to parse PDF")
      }

      guard let page = document.page(at: 0) else {
        throw Exception(name: "ERR_NO_PAGE", description: "PDF has no pages")
      }

      let pageRect = page.bounds(for: .mediaBox)
      let scale = CGFloat(dpi) / 72.0
      let width = Int(pageRect.width * scale)
      let height = Int(pageRect.height * scale)

      // Render page to grayscale bitmap
      let colorSpace = CGColorSpaceCreateDeviceGray()
      guard let context = CGContext(
        data: nil,
        width: width,
        height: height,
        bitsPerComponent: 8,
        bytesPerRow: width,
        space: colorSpace,
        bitmapInfo: CGImageAlphaInfo.none.rawValue
      ) else {
        throw Exception(name: "ERR_CONTEXT", description: "Failed to create bitmap context")
      }

      // White background
      context.setFillColor(gray: 1.0, alpha: 1.0)
      context.fill(CGRect(x: 0, y: 0, width: width, height: height))

      // Render PDF page
      context.scaleBy(x: scale, y: scale)
      page.draw(with: .mediaBox, to: context)

      guard let data = context.data else {
        throw Exception(name: "ERR_RENDER", description: "Failed to render PDF")
      }

      let pixels = data.assumingMemoryBound(to: UInt8.self)

      // Pack 8 pixels into 1 byte (MSB first), TSPL: 0=black, 1=white
      let widthBytes = (width + 7) / 8
      var bitmapData = Data(capacity: widthBytes * height)

      for row in 0..<height {
        for col in 0..<widthBytes {
          var byte: UInt8 = 0
          for bit in 0..<8 {
            let px = col * 8 + bit
            if px < width {
              let idx = row * width + px
              // pixel > 127 = white → bit 1, pixel <= 127 = black → bit 0
              if pixels[idx] > 127 {
                byte |= (1 << (7 - bit))
              }
            } else {
              byte |= (1 << (7 - bit)) // padding = white
            }
          }
          bitmapData.append(byte)
        }
      }

      return [
        "bitmap": bitmapData.base64EncodedString(),
        "widthBytes": widthBytes,
        "widthDots": width,
        "heightDots": height,
      ]
    }
  }
}
