/*
 * $RCSfile: PNMImageDecoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.3 $
 * $Date: 2006-08-22 00:12:04 $
 * $State: Exp $
 */
package com.sun.media.jai.codecimpl;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;

import javax.media.jai.RasterFactory;
import javax.media.jai.util.ImagingException;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecodeParam;
import com.sun.media.jai.codec.ImageDecoderImpl;
import com.sun.media.jai.codec.SeekableStream;

/**
 * @since EA2
 */
public class PNMImageDecoder extends ImageDecoderImpl {

  public PNMImageDecoder(final SeekableStream input, final ImageDecodeParam param) {
    super(input, param);
  }

  @Override
  public RenderedImage decodeAsRenderedImage(final int page) throws IOException {
    if (page != 0) {
      throw new IOException(JaiI18N.getString("PNMImageDecoder5"));
    }
    try {
      return new PNMImage(this.input);
    } catch (final Exception e) {
      throw CodecUtils.toIOException(e);
    }
  }
}

class PNMImage extends SimpleRenderedImage {

  private static final int PBM_ASCII = '1';
  private static final int PGM_ASCII = '2';
  private static final int PPM_ASCII = '3';
  private static final int PBM_RAW = '4';
  private static final int PGM_RAW = '5';
  private static final int PPM_RAW = '6';

  private static final int LINE_FEED = 0x0A;

  private final SeekableStream input;

  private final byte[] lineSeparator;

  /** File variant: PBM/PGM/PPM, ASCII/RAW. */
  private int variant;

  /** Maximum pixel value. */
  private int maxValue;

  /** Raster that is the entire image. */
  private Raster theTile;

  private int numBands;

  private int dataType;

  /**
   * Construct a PNMImage.
   *
   * @param input The SeekableStream for the PNM file.
   */
  public PNMImage(final SeekableStream input) {
    this.theTile = null;

    this.input = input;

    final String ls = java.security.AccessController
        .doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));
    this.lineSeparator = ls.getBytes();

    // Read file header.
    try {
      if (this.input.read() != 'P') { // magic number
        throw new RuntimeException(JaiI18N.getString("PNMImageDecoder0"));
      }

      this.variant = this.input.read(); // file variant
      if ((this.variant < PBM_ASCII) || (this.variant > PPM_RAW)) {
        throw new RuntimeException(JaiI18N.getString("PNMImageDecoder1"));
      }

      this.width = readInteger(this.input); // width
      this.height = readInteger(this.input); // height

      if (this.variant == PBM_ASCII || this.variant == PBM_RAW) {
        this.maxValue = 1;
      } else {
        this.maxValue = readInteger(this.input); // maximum value
      }
    } catch (final IOException e) {
      final String message = JaiI18N.getString("PNMImageDecoder6");
      sendExceptionToListener(message, e);
      //            e.printStackTrace();
      //            throw new RuntimeException(JaiI18N.getString("PNMImageDecoder2"));
    }

    // The RAWBITS format can only support byte image data, which means
    // maxValue should be less than 0x100. In case there's a conflict,
    // base the maxValue on variant.
    if (isRaw(this.variant) && this.maxValue >= 0x100) {
      this.maxValue = 0xFF;
    }

    // Reset image layout so there's only one tile.
    this.tileWidth = this.width;
    this.tileHeight = this.height;

    // Determine number of bands: pixmap (PPM) is 3 bands,
    // bitmap (PBM) and greymap (PGM) are 1 band.
    if (this.variant == PPM_ASCII || this.variant == PPM_RAW) {
      this.numBands = 3;
    } else {
      this.numBands = 1;
    }

    // Determine data type based on maxValue.
    if (this.maxValue < 0x100) {
      this.dataType = DataBuffer.TYPE_BYTE;
    } else if (this.maxValue < 0x10000) {
      this.dataType = DataBuffer.TYPE_USHORT;
    } else {
      this.dataType = DataBuffer.TYPE_INT;
    }

    // Choose an appropriate SampleModel.
    if ((this.variant == PBM_ASCII) || (this.variant == PBM_RAW)) {
      // Each pixel takes 1 bit, pack 8 pixels into a byte.
      this.sampleModel = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, this.width, this.height, 1);
      this.colorModel = ImageCodec.createGrayIndexColorModel(this.sampleModel, false);
    } else {
      final int[] bandOffsets = this.numBands == 1 ? new int[]{ 0 } : new int[]{ 0, 1, 2 };
      this.sampleModel = RasterFactory.createPixelInterleavedSampleModel(
          this.dataType,
          this.tileWidth,
          this.tileHeight,
          this.numBands,
          this.tileWidth * this.numBands,
          bandOffsets);

      this.colorModel = ImageCodec.createComponentColorModel(this.sampleModel);
    }
  }

  /** Returns true if file variant is raw format, false if ASCII. */
  private boolean isRaw(final int v) {
    return (v >= PBM_RAW);
  }

  /** Reads the next integer. */
  private int readInteger(final SeekableStream in) throws IOException {
    int ret = 0;
    boolean foundDigit = false;

    int b;
    while ((b = in.read()) != -1) {
      final char c = (char) b;
      if (Character.isDigit(c)) {
        ret = ret * 10 + Character.digit(c, 10);
        foundDigit = true;
      } else {
        if (c == '#') { // skip to the end of comment line
          final int length = this.lineSeparator.length;

          while ((b = in.read()) != -1) {
            boolean eol = false;
            for (int i = 0; i < length; i++) {
              if (b == this.lineSeparator[i]) {
                eol = true;
                break;
              }
            }
            if (eol) {
              break;
            }
          }
          if (b == -1) {
            break;
          }
        }
        if (foundDigit) {
          break;
        }
      }
    }

    return ret;
  }

  private Raster computeTile(final int tileX, final int tileY) {
    // Create a new tile.
    final Point org = new Point(tileXToX(tileX), tileYToY(tileY));
    final WritableRaster tile = Raster.createWritableRaster(this.sampleModel, org);
    final Rectangle tileRect = tile.getBounds();

    // There should only be one tile.
    try {
      switch (this.variant) {
        case PBM_ASCII:
        case PBM_RAW:
          // SampleModel for these cases should be MultiPixelPacked.

          final DataBuffer dataBuffer = tile.getDataBuffer();
          if (isRaw(this.variant)) {
            // Read the entire image.
            final byte[] buf = ((DataBufferByte) dataBuffer).getData();
            this.input.readFully(buf, 0, buf.length);
          } else {
            // Read 8 rows at a time
            final byte[] pixels = new byte[8 * this.width];
            final int offset = 0;
            for (int row = 0; row < this.tileHeight; row += 8) {
              final int rows = Math.min(8, this.tileHeight - row);
              final int len = (rows * this.width + 7) / 8;

              for (int i = 0; i < rows * this.width; i++) {
                pixels[i] = (byte) readInteger(this.input);
              }
              this.sampleModel.setDataElements(tileRect.x, row, tileRect.width, rows, pixels, dataBuffer);
            }
          }
          break;

        case PGM_ASCII:
        case PGM_RAW:
        case PPM_ASCII:
        case PPM_RAW:
          // SampleModel for these cases should be PixelInterleaved.
          final int size = this.width * this.height * this.numBands;

          switch (this.dataType) {
            case DataBuffer.TYPE_BYTE:
              final DataBufferByte bbuf = (DataBufferByte) tile.getDataBuffer();
              final byte[] byteArray = bbuf.getData();
              if (isRaw(this.variant)) {
                this.input.readFully(byteArray);
              } else {
                for (int i = 0; i < size; i++) {
                  byteArray[i] = (byte) readInteger(this.input);
                }
              }
              break;

            case DataBuffer.TYPE_USHORT:
              final DataBufferUShort sbuf = (DataBufferUShort) tile.getDataBuffer();
              final short[] shortArray = sbuf.getData();
              for (int i = 0; i < size; i++) {
                shortArray[i] = (short) readInteger(this.input);
              }
              break;

            case DataBuffer.TYPE_INT:
              final DataBufferInt ibuf = (DataBufferInt) tile.getDataBuffer();
              final int[] intArray = ibuf.getData();
              for (int i = 0; i < size; i++) {
                intArray[i] = readInteger(this.input);
              }
              break;
          }
          break;
      }

      // Close the PNM stream and release system resources.
      this.input.close();
    } catch (final IOException e) {
      final String message = JaiI18N.getString("PNMImageDecoder7");
      sendExceptionToListener(message, e);
      //            e.printStackTrace();
      //            throw new RuntimeException(JaiI18N.getString("PNMImageDecoder3"));
    }

    return tile;
  }

  @Override
  public synchronized Raster getTile(final int tileX, final int tileY) {
    if ((tileX != 0) || (tileY != 0)) {
      throw new IllegalArgumentException(JaiI18N.getString("PNMImageDecoder4"));
    }

    if (this.theTile == null) {
      this.theTile = computeTile(tileX, tileY);
    }

    return this.theTile;
  }

  public void dispose() {
    this.theTile = null;
  }

  private void sendExceptionToListener(final String message, final Exception e) {
    ImagingListenerProxy.errorOccurred(message, new ImagingException(message, e), this, false);
  }
}
