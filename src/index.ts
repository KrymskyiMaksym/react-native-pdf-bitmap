import { requireNativeModule } from 'expo-modules-core';

type BitmapResult = {
  bitmap: string;
  widthBytes: number;
  widthDots: number;
  heightDots: number;
  /** Zero-based index of the source page inside the PDF. */
  pageIndex: number;
};

const PdfBitmap = requireNativeModule<{
  renderPdfToBitmap: (
    base64Pdf: string,
    dpi: number,
  ) => Promise<BitmapResult>;
  renderPdfToBitmaps: (
    base64Pdf: string,
    dpi: number,
  ) => Promise<BitmapResult[]>;
}>('PdfBitmap');

/**
 * Renders the first page of a PDF (base64) to a monochrome 1-bit bitmap
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

/**
 * Renders every page of a PDF (base64) to monochrome 1-bit bitmaps,
 * in page order. Use this for multi-page documents such as a multi-seat
 * waybill, where each page is a separate label.
 *
 * @param base64Pdf - PDF file content as base64 string
 * @param dpi - Resolution (default 203 for thermal printers)
 * @returns One packed bitmap per page
 */
export async function renderPdfToBitmaps(
  base64Pdf: string,
  dpi = 203,
): Promise<BitmapResult[]> {
  return PdfBitmap.renderPdfToBitmaps(base64Pdf, dpi);
}

export type { BitmapResult };
