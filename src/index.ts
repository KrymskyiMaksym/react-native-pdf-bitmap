import { requireNativeModule } from 'expo-modules-core';

type BitmapResult = {
  bitmap: string;
  widthBytes: number;
  widthDots: number;
  heightDots: number;
};

const PdfBitmap = requireNativeModule<{
  renderPdfToBitmap: (
    base64Pdf: string,
    dpi: number,
  ) => Promise<BitmapResult>;
}>('PdfBitmap');

/**
 * Renders first page of a PDF (base64) to a monochrome 1-bit bitmap
 * suitable for TSPL thermal printers.
 *
 * @param base64Pdf - PDF file content as base64 string
 * @param dpi - Resolution (default 203 for thermal printers)
 * @returns Packed bitmap data + dimensions
 */
export async function renderPdfToBitmap(
  base64Pdf: string,
  dpi = 203,
): Promise<BitmapResult> {
  return PdfBitmap.renderPdfToBitmap(base64Pdf, dpi);
}

export type { BitmapResult };
