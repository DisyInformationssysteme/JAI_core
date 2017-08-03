/*
 * $RCSfile: BMPImageDecoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.4 $
 * $Date: 2006-08-22 00:12:03 $
 * $State: Exp $
 */
package com.sun.media.jai.codecimpl;

import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.media.jai.RasterFactory;
import javax.media.jai.util.ImagingException;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecodeParam;
import com.sun.media.jai.codec.ImageDecoderImpl;

/**
 * @since EA2
 */
public class BMPImageDecoder extends ImageDecoderImpl {

  public BMPImageDecoder(final InputStream input, final ImageDecodeParam param) {
    super(input, param);
  }

  @Override
  public RenderedImage decodeAsRenderedImage(final int page) throws IOException {
    if (page != 0) {
      throw new IOException(JaiI18N.getString("BMPImageDecoder8"));
    }
    try {
      return new BMPImage(this.input);
    } catch (final Exception e) {
      throw CodecUtils.toIOException(e);
    }
  }
}

class BMPImage extends SimpleRenderedImage {

  // BMP variables
  private BufferedInputStream inputStream;
  private long bitmapFileSize;
  private long bitmapOffset;
  private long compression;
  private long imageSize;
  private byte palette[];
  private int imageType;
  private int numBands;
  private boolean isBottomUp;
  private int bitsPerPixel;
  private int redMask, greenMask, blueMask, alphaMask;

  // BMP Image types
  private static final int VERSION_2_1_BIT = 0;
  private static final int VERSION_2_4_BIT = 1;
  private static final int VERSION_2_8_BIT = 2;
  private static final int VERSION_2_24_BIT = 3;

  private static final int VERSION_3_1_BIT = 4;
  private static final int VERSION_3_4_BIT = 5;
  private static final int VERSION_3_8_BIT = 6;
  private static final int VERSION_3_24_BIT = 7;

  private static final int VERSION_3_NT_16_BIT = 8;
  private static final int VERSION_3_NT_32_BIT = 9;

  private static final int VERSION_4_1_BIT = 10;
  private static final int VERSION_4_4_BIT = 11;
  private static final int VERSION_4_8_BIT = 12;
  private static final int VERSION_4_16_BIT = 13;
  private static final int VERSION_4_24_BIT = 14;
  private static final int VERSION_4_32_BIT = 15;

  // Color space types
  private static final int LCS_CALIBRATED_RGB = 0;
  private static final int LCS_sRGB = 1;
  private static final int LCS_CMYK = 2;

  // Compression Types
  private static final int BI_RGB = 0;
  private static final int BI_RLE8 = 1;
  private static final int BI_RLE4 = 2;
  private static final int BI_BITFIELDS = 3;

  private WritableRaster theTile = null;

  /**
   * Constructor for BMPImage
   *
   * @param stream
   */
  public BMPImage(final InputStream stream) {
    if (stream instanceof BufferedInputStream) {
      this.inputStream = (BufferedInputStream) stream;
    } else {
      this.inputStream = new BufferedInputStream(stream);
    }
    try {

      this.inputStream.mark(Integer.MAX_VALUE);

      // Start File Header
      if (!(readUnsignedByte(this.inputStream) == 'B' && readUnsignedByte(this.inputStream) == 'M')) {
        throw new RuntimeException(JaiI18N.getString("BMPImageDecoder0"));
      }

      // Read file size
      this.bitmapFileSize = readDWord(this.inputStream);

      // Read the two reserved fields
      readWord(this.inputStream);
      readWord(this.inputStream);

      // Offset to the bitmap from the beginning
      this.bitmapOffset = readDWord(this.inputStream);

      // End File Header

      // Start BitmapCoreHeader
      final long size = readDWord(this.inputStream);

      if (size == 12) {
        this.width = readWord(this.inputStream);
        this.height = readWord(this.inputStream);
      } else {
        this.width = readLong(this.inputStream);
        this.height = readLong(this.inputStream);
      }

      final int planes = readWord(this.inputStream);
      this.bitsPerPixel = readWord(this.inputStream);

      this.properties.put("color_planes", new Integer(planes));
      this.properties.put("bits_per_pixel", new Integer(this.bitsPerPixel));

      // As BMP always has 3 rgb bands, except for Version 5,
      // which is bgra
      this.numBands = 3;

      if (size == 12) {
        // Windows 2.x and OS/2 1.x
        this.properties.put("bmp_version", "BMP v. 2.x");

        // Classify the image type
        if (this.bitsPerPixel == 1) {
          this.imageType = VERSION_2_1_BIT;
        } else if (this.bitsPerPixel == 4) {
          this.imageType = VERSION_2_4_BIT;
        } else if (this.bitsPerPixel == 8) {
          this.imageType = VERSION_2_8_BIT;
        } else if (this.bitsPerPixel == 24) {
          this.imageType = VERSION_2_24_BIT;
        }

        // Read in the palette
        final int numberOfEntries = (int) ((this.bitmapOffset - 14 - size) / 3);
        final int sizeOfPalette = numberOfEntries * 3;
        this.palette = new byte[sizeOfPalette];
        this.inputStream.read(this.palette, 0, sizeOfPalette);
        this.properties.put("palette", this.palette);
      } else {

        this.compression = readDWord(this.inputStream);
        this.imageSize = readDWord(this.inputStream);
        final long xPelsPerMeter = readLong(this.inputStream);
        final long yPelsPerMeter = readLong(this.inputStream);
        final long colorsUsed = readDWord(this.inputStream);
        final long colorsImportant = readDWord(this.inputStream);

        switch ((int) this.compression) {
          case BI_RGB:
            this.properties.put("compression", "BI_RGB");
            break;

          case BI_RLE8:
            this.properties.put("compression", "BI_RLE8");
            break;

          case BI_RLE4:
            this.properties.put("compression", "BI_RLE4");
            break;

          case BI_BITFIELDS:
            this.properties.put("compression", "BI_BITFIELDS");
            break;
        }

        this.properties.put("x_pixels_per_meter", new Long(xPelsPerMeter));
        this.properties.put("y_pixels_per_meter", new Long(yPelsPerMeter));
        this.properties.put("colors_used", new Long(colorsUsed));
        this.properties.put("colors_important", new Long(colorsImportant));

        if (size == 40) {
          // Windows 3.x and Windows NT
          switch ((int) this.compression) {

            case BI_RGB: // No compression
            case BI_RLE8: // 8-bit RLE compression
            case BI_RLE4: // 4-bit RLE compression

              // Read in the palette
              final int numberOfEntries = (int) ((this.bitmapOffset - 14 - size) / 4);
              int sizeOfPalette = numberOfEntries * 4;
              this.palette = new byte[sizeOfPalette];
              this.inputStream.read(this.palette, 0, sizeOfPalette);
              this.properties.put("palette", this.palette);

              if (this.bitsPerPixel == 1) {
                this.imageType = VERSION_3_1_BIT;
              } else if (this.bitsPerPixel == 4) {
                this.imageType = VERSION_3_4_BIT;
              } else if (this.bitsPerPixel == 8) {
                this.imageType = VERSION_3_8_BIT;
              } else if (this.bitsPerPixel == 24) {
                this.imageType = VERSION_3_24_BIT;
              } else if (this.bitsPerPixel == 16) {
                this.imageType = VERSION_3_NT_16_BIT;
                this.redMask = 0x7C00;
                this.greenMask = 0x3E0;
                this.blueMask = 0x1F;
                this.properties.put("red_mask", new Integer(this.redMask));
                this.properties.put("green_mask", new Integer(this.greenMask));
                this.properties.put("blue_mask", new Integer(this.blueMask));
              } else if (this.bitsPerPixel == 32) {
                this.imageType = VERSION_3_NT_32_BIT;
                this.redMask = 0x00FF0000;
                this.greenMask = 0x0000FF00;
                this.blueMask = 0x000000FF;
                this.properties.put("red_mask", new Integer(this.redMask));
                this.properties.put("green_mask", new Integer(this.greenMask));
                this.properties.put("blue_mask", new Integer(this.blueMask));
              }

              this.properties.put("bmp_version", "BMP v. 3.x");
              break;

            case BI_BITFIELDS:

              if (this.bitsPerPixel == 16) {
                this.imageType = VERSION_3_NT_16_BIT;
              } else if (this.bitsPerPixel == 32) {
                this.imageType = VERSION_3_NT_32_BIT;
              }

              // BitsField encoding
              this.redMask = (int) readDWord(this.inputStream);
              this.greenMask = (int) readDWord(this.inputStream);
              this.blueMask = (int) readDWord(this.inputStream);

              this.properties.put("red_mask", new Integer(this.redMask));
              this.properties.put("green_mask", new Integer(this.greenMask));
              this.properties.put("blue_mask", new Integer(this.blueMask));

              if (colorsUsed != 0) {
                // there is a palette
                sizeOfPalette = (int) colorsUsed * 4;
                this.palette = new byte[sizeOfPalette];
                this.inputStream.read(this.palette, 0, sizeOfPalette);
                this.properties.put("palette", this.palette);
              }

              this.properties.put("bmp_version", "BMP v. 3.x NT");
              break;

            default:
              throw new RuntimeException(JaiI18N.getString("BMPImageDecoder1"));
          }
        } else if (size == 108) {
          // Windows 4.x BMP

          this.properties.put("bmp_version", "BMP v. 4.x");

          // rgb masks, valid only if comp is BI_BITFIELDS
          this.redMask = (int) readDWord(this.inputStream);
          this.greenMask = (int) readDWord(this.inputStream);
          this.blueMask = (int) readDWord(this.inputStream);
          // Only supported for 32bpp BI_RGB argb
          this.alphaMask = (int) readDWord(this.inputStream);
          final long csType = readDWord(this.inputStream);
          final int redX = readLong(this.inputStream);
          final int redY = readLong(this.inputStream);
          final int redZ = readLong(this.inputStream);
          final int greenX = readLong(this.inputStream);
          final int greenY = readLong(this.inputStream);
          final int greenZ = readLong(this.inputStream);
          final int blueX = readLong(this.inputStream);
          final int blueY = readLong(this.inputStream);
          final int blueZ = readLong(this.inputStream);
          final long gammaRed = readDWord(this.inputStream);
          final long gammaGreen = readDWord(this.inputStream);
          final long gammaBlue = readDWord(this.inputStream);

          // Read in the palette
          final int numberOfEntries = (int) ((this.bitmapOffset - 14 - size) / 4);
          final int sizeOfPalette = numberOfEntries * 4;
          this.palette = new byte[sizeOfPalette];
          this.inputStream.read(this.palette, 0, sizeOfPalette);

          if (this.palette != null || this.palette.length != 0) {
            this.properties.put("palette", this.palette);
          }

          switch ((int) csType) {
            case LCS_CALIBRATED_RGB:
              // All the new fields are valid only for this case
              this.properties.put("color_space", "LCS_CALIBRATED_RGB");
              this.properties.put("redX", new Integer(redX));
              this.properties.put("redY", new Integer(redY));
              this.properties.put("redZ", new Integer(redZ));
              this.properties.put("greenX", new Integer(greenX));
              this.properties.put("greenY", new Integer(greenY));
              this.properties.put("greenZ", new Integer(greenZ));
              this.properties.put("blueX", new Integer(blueX));
              this.properties.put("blueY", new Integer(blueY));
              this.properties.put("blueZ", new Integer(blueZ));
              this.properties.put("gamma_red", new Long(gammaRed));
              this.properties.put("gamma_green", new Long(gammaGreen));
              this.properties.put("gamma_blue", new Long(gammaBlue));

              // break;
              throw new RuntimeException(JaiI18N.getString("BMPImageDecoder2"));

            case LCS_sRGB:
              // Default Windows color space
              this.properties.put("color_space", "LCS_sRGB");
              break;

            case LCS_CMYK:
              this.properties.put("color_space", "LCS_CMYK");
              //		    break;
              throw new RuntimeException(JaiI18N.getString("BMPImageDecoder2"));
          }

          if (this.bitsPerPixel == 1) {
            this.imageType = VERSION_4_1_BIT;
          } else if (this.bitsPerPixel == 4) {
            this.imageType = VERSION_4_4_BIT;
          } else if (this.bitsPerPixel == 8) {
            this.imageType = VERSION_4_8_BIT;
          } else if (this.bitsPerPixel == 16) {
            this.imageType = VERSION_4_16_BIT;
            if ((int) this.compression == BI_RGB) {
              this.redMask = 0x7C00;
              this.greenMask = 0x3E0;
              this.blueMask = 0x1F;
            }
          } else if (this.bitsPerPixel == 24) {
            this.imageType = VERSION_4_24_BIT;
          } else if (this.bitsPerPixel == 32) {
            this.imageType = VERSION_4_32_BIT;
            if ((int) this.compression == BI_RGB) {
              this.redMask = 0x00FF0000;
              this.greenMask = 0x0000FF00;
              this.blueMask = 0x000000FF;
            }
          }

          this.properties.put("red_mask", new Integer(this.redMask));
          this.properties.put("green_mask", new Integer(this.greenMask));
          this.properties.put("blue_mask", new Integer(this.blueMask));
          this.properties.put("alpha_mask", new Integer(this.alphaMask));
        } else {
          this.properties.put("bmp_version", "BMP v. 5.x");
          throw new RuntimeException(JaiI18N.getString("BMPImageDecoder4"));
        }
      }
    } catch (final IOException ioe) {
      final String message = JaiI18N.getString("BMPImageDecoder5");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
      //	    throw new RuntimeException(JaiI18N.getString("BMPImageDecoder5"));
    }

    if (this.height > 0) {
      // bottom up image
      this.isBottomUp = true;
    } else {
      // top down image
      this.isBottomUp = false;
      this.height = Math.abs(this.height);
    }

    // Reset Image Layout so there's only one tile.
    this.tileWidth = this.width;
    this.tileHeight = this.height;

    // When number of bitsPerPixel is <= 8, we use IndexColorModel.
    if (this.bitsPerPixel == 1 || this.bitsPerPixel == 4 || this.bitsPerPixel == 8) {

      this.numBands = 1;

      if (this.bitsPerPixel == 8) {
        this.sampleModel = RasterFactory
            .createPixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, this.width, this.height, this.numBands);
      } else {
        // 1 and 4 bit pixels can be stored in a packed format.
        this.sampleModel = new MultiPixelPackedSampleModel(
            DataBuffer.TYPE_BYTE,
            this.width,
            this.height,
            this.bitsPerPixel);
      }

      // Create IndexColorModel from the palette.
      byte r[], g[], b[];
      int size;
      if (this.imageType == VERSION_2_1_BIT || this.imageType == VERSION_2_4_BIT || this.imageType == VERSION_2_8_BIT) {

        size = this.palette.length / 3;

        if (size > 256) {
          size = 256;
        }

        int off;
        r = new byte[size];
        g = new byte[size];
        b = new byte[size];
        for (int i = 0; i < size; i++) {
          off = 3 * i;
          b[i] = this.palette[off];
          g[i] = this.palette[off + 1];
          r[i] = this.palette[off + 2];
        }
      } else {
        size = this.palette.length / 4;

        if (size > 256) {
          size = 256;
        }

        int off;
        r = new byte[size];
        g = new byte[size];
        b = new byte[size];
        for (int i = 0; i < size; i++) {
          off = 4 * i;
          b[i] = this.palette[off];
          g[i] = this.palette[off + 1];
          r[i] = this.palette[off + 2];
        }
      }

      if (ImageCodec.isIndicesForGrayscale(r, g, b)) {
        this.colorModel = ImageCodec.createComponentColorModel(this.sampleModel);
      } else {
        this.colorModel = new IndexColorModel(this.bitsPerPixel, size, r, g, b);
      }
    } else if (this.bitsPerPixel == 16) {
      this.numBands = 3;
      this.sampleModel = new SinglePixelPackedSampleModel(
          DataBuffer.TYPE_USHORT,
          this.width,
          this.height,
          new int[]{ this.redMask, this.greenMask, this.blueMask });

      this.colorModel = new DirectColorModel(
          ColorSpace.getInstance(ColorSpace.CS_sRGB),
          16,
          this.redMask,
          this.greenMask,
          this.blueMask,
          0,
          false,
          DataBuffer.TYPE_USHORT);
    } else if (this.bitsPerPixel == 32) {
      this.numBands = this.alphaMask == 0 ? 3 : 4;

      // The number of bands in the SampleModel is determined by
      // the length of the mask array passed in.
      final int[] bitMasks = this.numBands == 3
          ? new int[]{ this.redMask, this.greenMask, this.blueMask }
          : new int[]{ this.redMask, this.greenMask, this.blueMask, this.alphaMask };

      this.sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, this.width, this.height, bitMasks);

      this.colorModel = new DirectColorModel(
          ColorSpace.getInstance(ColorSpace.CS_sRGB),
          32,
          this.redMask,
          this.greenMask,
          this.blueMask,
          this.alphaMask,
          false,
          DataBuffer.TYPE_INT);
    } else {
      this.numBands = 3;
      // Create SampleModel
      this.sampleModel = RasterFactory
          .createPixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, this.width, this.height, this.numBands);

      this.colorModel = ImageCodec.createComponentColorModel(this.sampleModel);
    }

    try {
      this.inputStream.reset();
      this.inputStream.skip(this.bitmapOffset);
    } catch (final IOException ioe) {
      final String message = JaiI18N.getString("BMPImageDecoder9");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
    }
  }

  // Deal with 1 Bit images using IndexColorModels
  private void read1Bit(final byte[] bdata, final int paletteEntries) {

    int padding = 0;
    final int bytesPerScanline = (int) Math.ceil(this.width / 8.0);

    final int remainder = bytesPerScanline % 4;
    if (remainder != 0) {
      padding = 4 - remainder;
    }

    final int imSize = (bytesPerScanline + padding) * this.height;

    // Read till we have the whole image
    final byte values[] = new byte[imSize];
    try {
      int bytesRead = 0;
      while (bytesRead < imSize) {
        bytesRead += this.inputStream.read(values, bytesRead, imSize - bytesRead);
      }
    } catch (final IOException ioe) {
      final String message = JaiI18N.getString("BMPImageDecoder6");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
      //	    throw new
      //		RuntimeException(JaiI18N.getString("BMPImageDecoder6"));
    }

    if (this.isBottomUp) {

      // Convert the bottom up image to a top down format by copying
      // one scanline from the bottom to the top at a time.

      for (int i = 0; i < this.height; i++) {
        System.arraycopy(
            values,
            imSize - (i + 1) * (bytesPerScanline + padding),
            bdata,
            i * bytesPerScanline,
            bytesPerScanline);
      }
    } else {

      for (int i = 0; i < this.height; i++) {
        System.arraycopy(values, i * (bytesPerScanline + padding), bdata, i * bytesPerScanline, bytesPerScanline);
      }
    }
  }

  // Method to read a 4 bit BMP image data
  private void read4Bit(final byte[] bdata, final int paletteEntries) {

    // Padding bytes at the end of each scanline
    int padding = 0;

    final int bytesPerScanline = (int) Math.ceil(this.width / 2.0);
    final int remainder = bytesPerScanline % 4;
    if (remainder != 0) {
      padding = 4 - remainder;
    }

    final int imSize = (bytesPerScanline + padding) * this.height;

    // Read till we have the whole image
    final byte values[] = new byte[imSize];
    try {
      int bytesRead = 0;
      while (bytesRead < imSize) {
        bytesRead += this.inputStream.read(values, bytesRead, imSize - bytesRead);
      }
    } catch (final IOException ioe) {
      final String message = JaiI18N.getString("BMPImageDecoder6");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
      //	    throw new
      //		RuntimeException(JaiI18N.getString("BMPImageDecoder6"));
    }

    if (this.isBottomUp) {

      // Convert the bottom up image to a top down format by copying
      // one scanline from the bottom to the top at a time.
      for (int i = 0; i < this.height; i++) {
        System.arraycopy(
            values,
            imSize - (i + 1) * (bytesPerScanline + padding),
            bdata,
            i * bytesPerScanline,
            bytesPerScanline);
      }
    } else {
      for (int i = 0; i < this.height; i++) {
        System.arraycopy(values, i * (bytesPerScanline + padding), bdata, i * bytesPerScanline, bytesPerScanline);
      }
    }
  }

  // Method to read 8 bit BMP image data
  private void read8Bit(final byte[] bdata, final int paletteEntries) {

    // Padding bytes at the end of each scanline
    int padding = 0;

    // width * bitsPerPixel should be divisible by 32
    final int bitsPerScanline = this.width * 8;
    if (bitsPerScanline % 32 != 0) {
      padding = (bitsPerScanline / 32 + 1) * 32 - bitsPerScanline;
      padding = (int) Math.ceil(padding / 8.0);
    }

    final int imSize = (this.width + padding) * this.height;

    // Read till we have the whole image
    final byte values[] = new byte[imSize];
    try {
      int bytesRead = 0;
      while (bytesRead < imSize) {
        bytesRead += this.inputStream.read(values, bytesRead, imSize - bytesRead);
      }
    } catch (final IOException ioe) {
      final String message = JaiI18N.getString("BMPImageDecoder6");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
      //	    throw new
      //		RuntimeException(JaiI18N.getString("BMPImageDecoder6"));
    }

    if (this.isBottomUp) {

      // Convert the bottom up image to a top down format by copying
      // one scanline from the bottom to the top at a time.
      for (int i = 0; i < this.height; i++) {
        System.arraycopy(values, imSize - (i + 1) * (this.width + padding), bdata, i * this.width, this.width);
      }
    } else {
      for (int i = 0; i < this.height; i++) {
        System.arraycopy(values, i * (this.width + padding), bdata, i * this.width, this.width);
      }
    }
  }

  // Method to read 24 bit BMP image data
  private void read24Bit(final byte[] bdata) {
    // Padding bytes at the end of each scanline
    int padding = 0;

    // width * bitsPerPixel should be divisible by 32
    final int bitsPerScanline = this.width * 24;
    if (bitsPerScanline % 32 != 0) {
      padding = (bitsPerScanline / 32 + 1) * 32 - bitsPerScanline;
      padding = (int) Math.ceil(padding / 8.0);
    }

    int imSize = (int) this.imageSize;
    if (imSize == 0) {
      imSize = (int) (this.bitmapFileSize - this.bitmapOffset);
    }

    // Read till we have the whole image
    final byte values[] = new byte[imSize];
    try {
      int bytesRead = 0;
      while (bytesRead < imSize) {
        bytesRead += this.inputStream.read(values, bytesRead, imSize - bytesRead);
      }
    } catch (final IOException ioe) {
      // throw new RuntimeException(JaiI18N.getString("BMPImageDecoder6"));
      final String message = JaiI18N.getString("BMPImageDecoder4");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
      //	    throw new RuntimeException(ioe.getMessage());
    }

    int l = 0, count;

    if (this.isBottomUp) {
      final int max = this.width * this.height * 3 - 1;

      count = -padding;
      for (int i = 0; i < this.height; i++) {
        l = max - (i + 1) * this.width * 3 + 1;
        count += padding;
        for (int j = 0; j < this.width; j++) {
          bdata[l++] = values[count++];
          bdata[l++] = values[count++];
          bdata[l++] = values[count++];
        }
      }
    } else {
      count = -padding;
      for (int i = 0; i < this.height; i++) {
        count += padding;
        for (int j = 0; j < this.width; j++) {
          bdata[l++] = values[count++];
          bdata[l++] = values[count++];
          bdata[l++] = values[count++];
        }
      }
    }
  }

  private void read16Bit(final short sdata[]) {
    // Padding bytes at the end of each scanline
    int padding = 0;

    // width * bitsPerPixel should be divisible by 32
    final int bitsPerScanline = this.width * 16;
    if (bitsPerScanline % 32 != 0) {
      padding = (bitsPerScanline / 32 + 1) * 32 - bitsPerScanline;
      padding = (int) Math.ceil(padding / 8.0);
    }

    int imSize = (int) this.imageSize;
    if (imSize == 0) {
      imSize = (int) (this.bitmapFileSize - this.bitmapOffset);
    }

    int l = 0;

    try {
      if (this.isBottomUp) {
        final int max = this.width * this.height - 1;

        for (int i = 0; i < this.height; i++) {
          l = max - (i + 1) * this.width + 1;
          for (int j = 0; j < this.width; j++) {
            sdata[l++] = (short) (readWord(this.inputStream) & 0xffff);
          }
          for (int m = 0; m < padding; m++) {
            this.inputStream.read();
          }
        }
      } else {
        for (int i = 0; i < this.height; i++) {
          for (int j = 0; j < this.width; j++) {
            sdata[l++] = (short) (readWord(this.inputStream) & 0xffff);
          }
          for (int m = 0; m < padding; m++) {
            this.inputStream.read();
          }
        }
      }
    } catch (final IOException ioe) {
      final String message = JaiI18N.getString("BMPImageDecoder6");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
      //	    throw new RuntimeException(JaiI18N.getString("BMPImageDecoder6"));
    }
  }

  private void read32Bit(final int idata[]) {
    int imSize = (int) this.imageSize;
    if (imSize == 0) {
      imSize = (int) (this.bitmapFileSize - this.bitmapOffset);
    }

    int l = 0;

    try {
      if (this.isBottomUp) {
        final int max = this.width * this.height - 1;

        for (int i = 0; i < this.height; i++) {
          l = max - (i + 1) * this.width + 1;
          for (int j = 0; j < this.width; j++) {
            idata[l++] = (int) readDWord(this.inputStream);
          }
        }
      } else {
        for (int i = 0; i < this.height; i++) {
          for (int j = 0; j < this.width; j++) {
            idata[l++] = (int) readDWord(this.inputStream);
          }
        }
      }
    } catch (final IOException ioe) {
      final String message = JaiI18N.getString("BMPImageDecoder6");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
      //	    throw new RuntimeException(JaiI18N.getString("BMPImageDecoder6"));
    }
  }

  private void readRLE8(byte bdata[]) {

    // If imageSize field is not provided, calculate it.
    int imSize = (int) this.imageSize;
    if (imSize == 0) {
      imSize = (int) (this.bitmapFileSize - this.bitmapOffset);
    }

    int padding = 0;
    // If width is not 32 bit aligned, then while uncompressing each
    // scanline will have padding bytes, calculate the amount of padding
    final int remainder = this.width % 4;
    if (remainder != 0) {
      padding = 4 - remainder;
    }

    // Read till we have the whole image
    final byte values[] = new byte[imSize];
    try {
      int bytesRead = 0;
      while (bytesRead < imSize) {
        bytesRead += this.inputStream.read(values, bytesRead, imSize - bytesRead);
      }
    } catch (final IOException ioe) {
      final String message = JaiI18N.getString("BMPImageDecoder6");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
      //	    throw new RuntimeException(JaiI18N.getString("BMPImageDecoder6"));
    }

    // Since data is compressed, decompress it
    final byte val[] = decodeRLE8(imSize, padding, values);

    // Uncompressed data does not have any padding
    imSize = this.width * this.height;

    if (this.isBottomUp) {

      // Convert the bottom up image to a top down format by copying
      // one scanline from the bottom to the top at a time.
      // int bytesPerScanline = (int)Math.ceil((double)width/8.0);
      final int bytesPerScanline = this.width;
      for (int i = 0; i < this.height; i++) {
        System.arraycopy(val, imSize - (i + 1) * (bytesPerScanline), bdata, i * bytesPerScanline, bytesPerScanline);
      }

    } else {

      bdata = val;
    }
  }

  private byte[] decodeRLE8(final int imSize, final int padding, final byte values[]) {

    final byte val[] = new byte[this.width * this.height];
    int count = 0, l = 0;
    int value;
    boolean flag = false;

    while (count != imSize) {

      value = values[count++] & 0xff;

      if (value == 0) {
        switch (values[count++] & 0xff) {

          case 0:
            // End-of-scanline marker
            break;

          case 1:
            // End-of-RLE marker
            flag = true;
            break;

          case 2:
            // delta or vector marker
            final int xoff = values[count++] & 0xff;
            final int yoff = values[count] & 0xff;
            // Move to the position xoff, yoff down
            l += xoff + yoff * this.width;
            break;

          default:
            final int end = values[count - 1] & 0xff;
            for (int i = 0; i < end; i++) {
              val[l++] = (byte) (values[count++] & 0xff);
            }

            // Whenever end pixels can fit into odd number of bytes,
            // an extra padding byte will be present, so skip that.
            if (!isEven(end)) {
              count++;
            }
        }
      } else {
        for (int i = 0; i < value; i++) {
          val[l++] = (byte) (values[count] & 0xff);
        }
        count++;
      }

      // If End-of-RLE data, then exit the while loop
      if (flag) {
        break;
      }
    }

    return val;
  }

  private int[] readRLE4() {

    // If imageSize field is not specified, calculate it.
    int imSize = (int) this.imageSize;
    if (imSize == 0) {
      imSize = (int) (this.bitmapFileSize - this.bitmapOffset);
    }

    int padding = 0;
    // If width is not 32 byte aligned, then while uncompressing each
    // scanline will have padding bytes, calculate the amount of padding
    final int remainder = this.width % 4;
    if (remainder != 0) {
      padding = 4 - remainder;
    }

    // Read till we have the whole image
    final int values[] = new int[imSize];
    try {
      for (int i = 0; i < imSize; i++) {
        values[i] = this.inputStream.read();
      }
    } catch (final IOException ioe) {
      final String message = JaiI18N.getString("BMPImageDecoder6");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
      //	    throw new RuntimeException(JaiI18N.getString("BMPImageDecoder6"));
    }

    // Decompress the RLE4 compressed data.
    int val[] = decodeRLE4(imSize, padding, values);

    // Invert it as it is bottom up format.
    if (this.isBottomUp) {

      final int inverted[] = val;
      val = new int[this.width * this.height];
      int l = 0, index, lineEnd;

      for (int i = this.height - 1; i >= 0; i--) {
        index = i * this.width;
        lineEnd = l + this.width;
        while (l != lineEnd) {
          val[l++] = inverted[index++];
        }
      }
    }

    // This array will be used to call setPixels as the decompression
    // had unpacked the 4bit pixels each into an int.
    return val;
  }

  private int[] decodeRLE4(final int imSize, final int padding, final int values[]) {

    final int val[] = new int[this.width * this.height];
    int count = 0, l = 0;
    int value;
    boolean flag = false;

    while (count != imSize) {

      value = values[count++];

      if (value == 0) {

        // Absolute mode
        switch (values[count++]) {

          case 0:
            // End-of-scanline marker
            break;

          case 1:
            // End-of-RLE marker
            flag = true;
            break;

          case 2:
            // delta or vector marker
            final int xoff = values[count++];
            final int yoff = values[count];
            // Move to the position xoff, yoff down
            l += xoff + yoff * this.width;
            break;

          default:
            final int end = values[count - 1];
            for (int i = 0; i < end; i++) {
              val[l++] = isEven(i) ? (values[count] & 0xf0) >> 4 : (values[count++] & 0x0f);
            }

            // When end is odd, the above for loop does not
            // increment count, so do it now.
            if (!isEven(end)) {
              count++;
            }

            // Whenever end pixels can fit into odd number of bytes,
            // an extra padding byte will be present, so skip that.
            if (!isEven((int) Math.ceil(end / 2))) {
              count++;
            }
            break;
        }
      } else {
        // Encoded mode
        final int alternate[] = { (values[count] & 0xf0) >> 4, values[count] & 0x0f };
        for (int i = 0; i < value; i++) {
          val[l++] = alternate[i % 2];
        }

        count++;
      }

      // If End-of-RLE data, then exit the while loop
      if (flag) {
        break;
      }

    }

    return val;
  }

  private boolean isEven(final int number) {
    return (number % 2 == 0 ? true : false);
  }

  // Windows defined data type reading methods - everything is little endian

  // Unsigned 8 bits
  private int readUnsignedByte(final InputStream stream) throws IOException {
    return (stream.read() & 0xff);
  }

  // Unsigned 2 bytes
  private int readUnsignedShort(final InputStream stream) throws IOException {
    final int b1 = readUnsignedByte(stream);
    final int b2 = readUnsignedByte(stream);
    return ((b2 << 8) | b1) & 0xffff;
  }

  // Signed 16 bits
  private int readShort(final InputStream stream) throws IOException {
    final int b1 = readUnsignedByte(stream);
    final int b2 = readUnsignedByte(stream);
    return (b2 << 8) | b1;
  }

  // Unsigned 16 bits
  private int readWord(final InputStream stream) throws IOException {
    return readUnsignedShort(stream);
  }

  // Unsigned 4 bytes
  private long readUnsignedInt(final InputStream stream) throws IOException {
    final int b1 = readUnsignedByte(stream);
    final int b2 = readUnsignedByte(stream);
    final int b3 = readUnsignedByte(stream);
    final int b4 = readUnsignedByte(stream);
    final long l = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    return l & 0xffffffff;
  }

  // Signed 4 bytes
  private int readInt(final InputStream stream) throws IOException {
    final int b1 = readUnsignedByte(stream);
    final int b2 = readUnsignedByte(stream);
    final int b3 = readUnsignedByte(stream);
    final int b4 = readUnsignedByte(stream);
    return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
  }

  // Unsigned 4 bytes
  private long readDWord(final InputStream stream) throws IOException {
    return readUnsignedInt(stream);
  }

  // 32 bit signed value
  private int readLong(final InputStream stream) throws IOException {
    return readInt(stream);
  }

  private synchronized Raster computeTile(final int tileX, final int tileY) {
    if (this.theTile != null) {
      return this.theTile;
    }

    // Create a new tile
    final Point org = new Point(tileXToX(tileX), tileYToY(tileY));
    final WritableRaster tile = RasterFactory.createWritableRaster(this.sampleModel, org);
    byte bdata[] = null; // buffer for byte data
    short sdata[] = null; // buffer for short data
    int idata[] = null; // buffer for int data

    if (this.sampleModel.getDataType() == DataBuffer.TYPE_BYTE) {
      bdata = ((DataBufferByte) tile.getDataBuffer()).getData();
    } else if (this.sampleModel.getDataType() == DataBuffer.TYPE_USHORT) {
      sdata = ((DataBufferUShort) tile.getDataBuffer()).getData();
    } else if (this.sampleModel.getDataType() == DataBuffer.TYPE_INT) {
      idata = ((DataBufferInt) tile.getDataBuffer()).getData();
    }

    // There should only be one tile.
    switch (this.imageType) {

      case VERSION_2_1_BIT:
        // no compression
        read1Bit(bdata, 3);
        break;

      case VERSION_2_4_BIT:
        // no compression
        read4Bit(bdata, 3);
        break;

      case VERSION_2_8_BIT:
        // no compression
        read8Bit(bdata, 3);
        break;

      case VERSION_2_24_BIT:
        // no compression
        read24Bit(bdata);
        break;

      case VERSION_3_1_BIT:
        // 1-bit images cannot be compressed.
        read1Bit(bdata, 4);
        break;

      case VERSION_3_4_BIT:
        switch ((int) this.compression) {
          case BI_RGB:
            read4Bit(bdata, 4);
            break;

          case BI_RLE4:
            final int pixels[] = readRLE4();
            tile.setPixels(0, 0, this.width, this.height, pixels);
            break;

          default:
            throw new RuntimeException(JaiI18N.getString("BMPImageDecoder3"));
        }
        break;

      case VERSION_3_8_BIT:
        switch ((int) this.compression) {
          case BI_RGB:
            read8Bit(bdata, 4);
            break;

          case BI_RLE8:
            readRLE8(bdata);
            break;

          default:
            throw new RuntimeException(JaiI18N.getString("BMPImageDecoder3"));
        }

        break;

      case VERSION_3_24_BIT:
        // 24-bit images are not compressed
        read24Bit(bdata);
        break;

      case VERSION_3_NT_16_BIT:
        read16Bit(sdata);
        break;

      case VERSION_3_NT_32_BIT:
        read32Bit(idata);
        break;

      case VERSION_4_1_BIT:
        read1Bit(bdata, 4);
        break;

      case VERSION_4_4_BIT:
        switch ((int) this.compression) {

          case BI_RGB:
            read4Bit(bdata, 4);
            break;

          case BI_RLE4:
            final int pixels[] = readRLE4();
            tile.setPixels(0, 0, this.width, this.height, pixels);
            break;

          default:
            throw new RuntimeException(JaiI18N.getString("BMPImageDecoder3"));
        }

      case VERSION_4_8_BIT:
        switch ((int) this.compression) {

          case BI_RGB:
            read8Bit(bdata, 4);
            break;

          case BI_RLE8:
            readRLE8(bdata);
            break;

          default:
            throw new RuntimeException(JaiI18N.getString("BMPImageDecoder3"));
        }
        break;

      case VERSION_4_16_BIT:
        read16Bit(sdata);
        break;

      case VERSION_4_24_BIT:
        read24Bit(bdata);
        break;

      case VERSION_4_32_BIT:
        read32Bit(idata);
        break;
    }

    this.theTile = tile;

    return tile;
  }

  @Override
  public synchronized Raster getTile(final int tileX, final int tileY) {
    if ((tileX != 0) || (tileY != 0)) {
      throw new IllegalArgumentException(JaiI18N.getString("BMPImageDecoder7"));
    }
    return computeTile(tileX, tileY);
  }

  public void dispose() {
    this.theTile = null;
  }
}
