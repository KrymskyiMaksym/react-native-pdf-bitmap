# @krymskyimaksym/react-native-pdf-bitmap

Render PDF pages to monochrome 1-bit bitmaps for TSPL thermal printers (Xprinter, TSC, etc).

Uses **iOS PDFKit** and **Android PdfRenderer** natively — no Ghostscript, no poppler, no external dependencies.

## Installation

```bash
yarn add @krymskyimaksym/react-native-pdf-bitmap
npx expo run:ios # or npx expo run:android
```

## Usage

```typescript
import { renderPdfToBitmap } from '@krymskyimaksym/react-native-pdf-bitmap';

const result = await renderPdfToBitmap(base64PdfString, 203);
// result.bitmap     — base64 packed 1-bit bitmap (TSPL format)
// result.widthBytes — width in bytes (8 pixels per byte)
// result.widthDots  — width in pixels
// result.heightDots — height in pixels
```

## TSPL Bitmap Format

The output bitmap is packed as:
- 8 pixels per byte, MSB first
- 0 = black (print), 1 = white (no print)
- Ready for TSPL `BITMAP x,y,widthBytes,heightDots,0,<data>` command

## Requirements

- Expo SDK 52+
- iOS 15.1+
- Android API 24+