// CHECKSTYLE OFF: IllegalCallsCheck
/*
 * $RCSfile: TIFFImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.6 $
 * $Date: 2006-02-17 17:59:15 $
 * $State: Exp $
 */
package com.sun.media.jai.codecimpl;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.media.jai.DataBufferFloat;
import javax.media.jai.FloatDoubleColorModel;
import javax.media.jai.RasterFactory;
import javax.media.jai.util.ImagingException;

//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGDecodeParam;
//import com.sun.image.codec.jpeg.JPEGImageDecoder;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.sun.media.jai.codec.TIFFDirectory;
import com.sun.media.jai.codec.TIFFField;
import com.sun.media.jai.util.SimpleCMYKColorSpace;

public class TIFFImage extends SimpleRenderedImage {

  // Compression types
  public static final int COMP_NONE = 1;
  public static final int COMP_FAX_G3_1D = 2;
  public static final int COMP_FAX_G3_2D = 3;
  public static final int COMP_FAX_G4_2D = 4;
  public static final int COMP_LZW = 5;
  public static final int COMP_JPEG_OLD = 6;
  public static final int COMP_JPEG_TTN2 = 7;
  public static final int COMP_PACKBITS = 32773;
  public static final int COMP_DEFLATE = 32946;

  // Image types
  private static final int TYPE_UNSUPPORTED = -1;
  private static final int TYPE_BILEVEL = 0;
  private static final int TYPE_GRAY_4BIT = 1;
  private static final int TYPE_GRAY = 2;
  private static final int TYPE_GRAY_ALPHA = 3;
  private static final int TYPE_PALETTE = 4;
  private static final int TYPE_RGB = 5;
  private static final int TYPE_RGB_ALPHA = 6;
  private static final int TYPE_YCBCR_SUB = 7;
  private static final int TYPE_GENERIC = 8;
  private static final int TYPE_CMYK = 9;

  // Incidental tags
  private static final int TIFF_JPEG_TABLES = 347;
  private static final int TIFF_YCBCR_SUBSAMPLING = 530;

  SeekableStream stream;
  private boolean isTiled;
  int tileSize;
  int tilesX, tilesY;
  long[] tileOffsets;
  long[] tileByteCounts;
  char[] colormap;
  int sampleSize;
  int compression;
  byte[] palette;
  int numBands;

  int chromaSubH;
  int chromaSubV;

  // Fax compression related variables
  long tiffT4Options;
  long tiffT6Options;
  int fillOrder;

  // LZW compression related variable
  int predictor;

  // TTN2 JPEG related variables
//  JPEGDecodeParam decodeParam = null;
  boolean colorConvertJPEG = false;

  // DEFLATE variables
  Inflater inflater = null;

  // Endian-ness indicator
  boolean isBigEndian;

  int imageType;
  boolean isWhiteZero = false;
  int dataType;

  boolean decodePaletteAsShorts;

  // Decoders
  private TIFFFaxDecoder decoder = null;
  private TIFFLZWDecoder lzwDecoder = null;

  /**
   * Decode a buffer of data into a Raster with the specified location.
   *
   * @param data buffer contain an interchange or abbreviated datastream.
   * @param decodeParam decoding parameters; may be null unless the
   *        data buffer contains an abbreviated datastream in which case
   *        it may not be null or an error will occur.
   * @param colorConvert whether to perform color conversion; in this
   *        case that would be limited to YCbCr-to-RGB.
   * @param minX the X position of the returned Raster.
   * @param minY the Y position of the returned Raster.
   */
//  private static final Raster decodeJPEG(
//      final byte[] data,
//      final JPEGDecodeParam decodeParam,
//      final boolean colorConvert,
//      final int minX,
//      final int minY) {
//    // Create an InputStream from the compressed data array.
//    final ByteArrayInputStream jpegStream = new ByteArrayInputStream(data);
//
//    // Create a decoder.
//    final JPEGImageDecoder decoder = decodeParam == null
//        ? JPEGCodec.createJPEGDecoder(jpegStream)
//        : JPEGCodec.createJPEGDecoder(jpegStream, decodeParam);
//
//    // Decode the compressed data into a Raster.
//    Raster jpegRaster = null;
//    try {
//      jpegRaster = colorConvert ? decoder.decodeAsBufferedImage().getWritableTile(0, 0) : decoder.decodeAsRaster();
//    } catch (final IOException ioe) {
//      final String message = JaiI18N.getString("TIFFImage13");
//      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), TIFFImage.class, false);
//      //            throw new RuntimeException(JaiI18N.getString("TIFFImage13"));
//    }
//
//    // Translate the decoded Raster to the specified location and return.
//    return jpegRaster.createTranslatedChild(minX, minY);
//  }

  /**
   * Inflates <code>deflated</code> into <code>inflated</code> using the
   * <code>Inflater</code> constructed during class instantiation.
   */
  private final void inflate(final byte[] deflated, final byte[] inflated) {
    this.inflater.setInput(deflated);
    try {
      this.inflater.inflate(inflated);
    } catch (final DataFormatException dfe) {
      final String message = JaiI18N.getString("TIFFImage17");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, dfe), this, false);
      //            throw new RuntimeException(JaiI18N.getString("TIFFImage17")+": "+
      //                                       dfe.getMessage());
    }
    this.inflater.reset();
  }

  /**
   * Creates a pixel-interleaved <code>SampleModel</code>. This is a hack
   * to work around a cast exception when using JAI with float data.
   */
  private final static SampleModel createPixelInterleavedSampleModel(
      final int dataType,
      final int tileWidth,
      final int tileHeight,
      final int pixelStride,
      final int scanlineStride,
      final int bandOffsets[]) {
    SampleModel sampleModel = null;

    if (dataType == DataBuffer.TYPE_FLOAT) {
      // This is a hack to make this work with JAI which in some
      // cases downcasts the DataBuffer to a type-specific class.
      // In the case of float data this current means the JAI class
      // javax.media.jai.DataBufferFloat.
      try {
        final Class rfClass = Class.forName("javax.media.jai.RasterFactory");
        final Class[] paramTypes = new Class[]{ int.class, int.class, int.class, int.class, int.class, int[].class };
        final Method rfMthd = rfClass.getMethod("createPixelInterleavedSampleModel", paramTypes);
        final Object[] params = new Object[]{
            new Integer(dataType),
            new Integer(tileWidth),
            new Integer(tileHeight),
            new Integer(pixelStride),
            new Integer(scanlineStride),
            bandOffsets };
        sampleModel = (SampleModel) rfMthd.invoke(null, params);
      } catch (final Exception e) {
        // Deliberately ignore the Exception.
      }
    }

    // Create a SampleModel for non-float data or, in the case of
    // float data, if it is still null. This latter case should occur
    // if and only if the decoder is being used without JAI.
    if (dataType != DataBuffer.TYPE_FLOAT || sampleModel == null) {
      sampleModel = RasterFactory
          .createPixelInterleavedSampleModel(dataType, tileWidth, tileHeight, pixelStride, scanlineStride, bandOffsets);
    }

    return sampleModel;
  }

  /**
   * Return as a long[] the value of a TIFF_LONG or TIFF_SHORT field.
   */
  private final long[] getFieldAsLongs(final TIFFField field) {
    long[] value = null;

    if (field.getType() == TIFFField.TIFF_SHORT) {
      final char[] charValue = field.getAsChars();
      value = new long[charValue.length];
      for (int i = 0; i < charValue.length; i++) {
        value[i] = charValue[i] & 0xffff;
      }
    } else if (field.getType() == TIFFField.TIFF_LONG) {
      value = field.getAsLongs();
    } else {
      throw new RuntimeException();
    }

    return value;
  }

  /*
   * Check whether the specified tag exists in the specified
   * TIFFDirectory. If not, throw an error message. Otherwise
   * return the TIFFField.
   */
  private TIFFField getField(final TIFFDirectory dir, final int tagID, final String tagName) {
    final TIFFField field = dir.getField(tagID);
    if (field == null) {
      final MessageFormat mf = new MessageFormat(JaiI18N.getString("TIFFImage5"));
      mf.setLocale(Locale.getDefault());
      throw new RuntimeException(mf.format(new Object[]{ tagName }));
    } else {
      return field;
    }
  }

  /**
   * Constructs a TIFFImage that acquires its data from a given
   * SeekableStream and reads from a particular IFD of the stream.
   * The index of the first IFD is 0.
   *
   * @param stream the SeekableStream to read from.
   * @param param an instance of TIFFDecodeParam, or null.
   * @param directory the index of the IFD to read from.
   */
  public TIFFImage(final SeekableStream stream, TIFFDecodeParam param, final int directory) throws IOException {

    this.stream = stream;
    if (param == null) {
      param = new TIFFDecodeParam();
    }

    this.decodePaletteAsShorts = param.getDecodePaletteAsShorts();

    // Read the specified directory.
    final TIFFDirectory dir = param.getIFDOffset() == null
        ? new TIFFDirectory(stream, directory)
        : new TIFFDirectory(stream, param.getIFDOffset().longValue(), directory);

    // Set a property "tiff_directory".
    this.properties.put("tiff_directory", dir);

    // Get the number of samples per pixel
    final TIFFField sfield = dir.getField(TIFFImageDecoder.TIFF_SAMPLES_PER_PIXEL);
    final int samplesPerPixel = sfield == null ? 1 : (int) sfield.getAsLong(0);

    // Read the TIFF_PLANAR_CONFIGURATION field
    final TIFFField planarConfigurationField = dir.getField(TIFFImageDecoder.TIFF_PLANAR_CONFIGURATION);
    final char[] planarConfiguration = planarConfigurationField == null
        ? new char[]{ 1 }
        : planarConfigurationField.getAsChars();

    // Support planar format (band sequential) only for 1 sample/pixel.
    if (planarConfiguration[0] != 1 && samplesPerPixel != 1) {
      throw new RuntimeException(JaiI18N.getString("TIFFImage0"));
    }

    // Read the TIFF_BITS_PER_SAMPLE field
    final TIFFField bitsField = dir.getField(TIFFImageDecoder.TIFF_BITS_PER_SAMPLE);
    char[] bitsPerSample = null;
    if (bitsField != null) {
      bitsPerSample = bitsField.getAsChars();
    } else {
      bitsPerSample = new char[]{ 1 };

      // Ensure that all samples have the same bit depth.
      for (int i = 1; i < bitsPerSample.length; i++) {
        if (bitsPerSample[i] != bitsPerSample[0]) {
          throw new RuntimeException(JaiI18N.getString("TIFFImage1"));
        }
      }
    }
    this.sampleSize = bitsPerSample[0];

    // Read the TIFF_SAMPLE_FORMAT tag to see whether the data might be
    // signed or floating point
    final TIFFField sampleFormatField = dir.getField(TIFFImageDecoder.TIFF_SAMPLE_FORMAT);

    char[] sampleFormat = null;
    if (sampleFormatField != null) {
      sampleFormat = sampleFormatField.getAsChars();

      // Check that all the samples have the same format
      for (int l = 1; l < sampleFormat.length; l++) {
        if (sampleFormat[l] != sampleFormat[0]) {
          throw new RuntimeException(JaiI18N.getString("TIFFImage2"));
        }
      }

    } else {
      sampleFormat = new char[]{ 1 };
    }

    // Set the data type based on the sample size and format.
    boolean isValidDataFormat = false;
    switch (this.sampleSize) {
      case 1:
      case 4:
      case 8:
        if (sampleFormat[0] != 3) {
          // Ignore whether signed or unsigned: treat all as unsigned.
          this.dataType = DataBuffer.TYPE_BYTE;
          isValidDataFormat = true;
        }
        break;
      case 16:
        if (sampleFormat[0] != 3) {
          this.dataType = sampleFormat[0] == 2 ? DataBuffer.TYPE_SHORT : DataBuffer.TYPE_USHORT;
          isValidDataFormat = true;
        }
        break;
      case 32:
        this.dataType = sampleFormat[0] == 3 ? DataBuffer.TYPE_FLOAT : DataBuffer.TYPE_INT;
        isValidDataFormat = true;
        break;
    }

    if (!isValidDataFormat) {
      throw new RuntimeException(JaiI18N.getString("TIFFImage3"));
    }

    // Figure out what compression if any, is being used.
    final TIFFField compField = dir.getField(TIFFImageDecoder.TIFF_COMPRESSION);
    this.compression = compField == null ? COMP_NONE : compField.getAsInt(0);

    // Get the photometric interpretation field.
    final TIFFField photoInterpField = dir.getField(TIFFImageDecoder.TIFF_PHOTOMETRIC_INTERPRETATION);

    // Set the photometric interpretation variable.
    int photometricType;
    if (photoInterpField != null) {
      // Set the variable from the photometric interpretation field.
      photometricType = (int) photoInterpField.getAsLong(0);
    } else {
      // The photometric interpretation field is missing; attempt
      // to infer the type from other information.
      if (dir.getField(TIFFImageDecoder.TIFF_COLORMAP) != null) {
        // There is a colormap so most likely a palette color image.
        photometricType = 3; // RGB Palette
      } else if (this.sampleSize == 1) {
        // Bilevel image so most likely a document; switch based
        // on the compression type of the image.
        if (this.compression == COMP_FAX_G3_1D
            || this.compression == COMP_FAX_G3_2D
            || this.compression == COMP_FAX_G4_2D) {
          photometricType = 0; // WhiteIsZero
        } else {
          photometricType = 1; // BlackIsZero
        }
      } else if (samplesPerPixel == 3 || samplesPerPixel == 4) {
        // Assume 3 bands is RGB and 4 bands is RGBA.
        photometricType = 2; // RGB
      } else {
        // Default to multi-band grayscale.
        photometricType = 1; // BlackIsZero
      }
    }

    // Determine which kind of image we are dealing with.
    this.imageType = TYPE_UNSUPPORTED;
    switch (photometricType) {
      case 0: // WhiteIsZero
        this.isWhiteZero = true;
      case 1: // BlackIsZero
        if (this.sampleSize == 1 && samplesPerPixel == 1) {
          this.imageType = TYPE_BILEVEL;
        } else if (this.sampleSize == 4 && samplesPerPixel == 1) {
          this.imageType = TYPE_GRAY_4BIT;
        } else if (this.sampleSize % 8 == 0) {
          if (samplesPerPixel == 1) {
            this.imageType = TYPE_GRAY;
          } else if (samplesPerPixel == 2) {
            this.imageType = TYPE_GRAY_ALPHA;
          } else {
            this.imageType = TYPE_GENERIC;
          }
        }
        break;
      case 2: // RGB
        if (this.sampleSize % 8 == 0) {
          if (samplesPerPixel == 3) {
            this.imageType = TYPE_RGB;
          } else if (samplesPerPixel == 4) {
            this.imageType = TYPE_RGB_ALPHA;
          } else {
            this.imageType = TYPE_GENERIC;
          }
        }
        break;
      case 3: // RGB Palette
        if (samplesPerPixel == 1 && (this.sampleSize == 4 || this.sampleSize == 8 || this.sampleSize == 16)) {
          this.imageType = TYPE_PALETTE;
        }
        break;
      case 4: // Transparency mask
        if (this.sampleSize == 1 && samplesPerPixel == 1) {
          this.imageType = TYPE_BILEVEL;
        }
        break;
      case 5: // Separated image, usually CMYK
        if (this.sampleSize == 8 && samplesPerPixel == 4) {
          this.imageType = TYPE_CMYK;
        }
      case 6: // YCbCr
        if (this.compression == COMP_JPEG_TTN2 && this.sampleSize == 8 && samplesPerPixel == 3) {
          // Set color conversion flag.
          this.colorConvertJPEG = param.getJPEGDecompressYCbCrToRGB();

          // Set type to RGB if color converting.
          this.imageType = this.colorConvertJPEG ? TYPE_RGB : TYPE_GENERIC;
        } else {
          final TIFFField chromaField = dir.getField(TIFF_YCBCR_SUBSAMPLING);
          if (chromaField != null) {
            this.chromaSubH = chromaField.getAsInt(0);
            this.chromaSubV = chromaField.getAsInt(1);
          } else {
            this.chromaSubH = this.chromaSubV = 2;
          }

          if (this.chromaSubH * this.chromaSubV == 1) {
            this.imageType = TYPE_GENERIC;
          } else if (this.sampleSize == 8 && samplesPerPixel == 3) {
            this.imageType = TYPE_YCBCR_SUB;
          }
        }
        break;
      default: // Other including CIE L*a*b*, unknown.
        if (this.sampleSize % 8 == 0) {
          this.imageType = TYPE_GENERIC;
        }
    }

    // Bail out if not one of the supported types.
    if (this.imageType == TYPE_UNSUPPORTED) {
      throw new RuntimeException(JaiI18N.getString("TIFFImage4"));
    }

    // Set basic image layout
    this.minX = this.minY = 0;
    this.width = (int) (getField(dir, TIFFImageDecoder.TIFF_IMAGE_WIDTH, "Image Width").getAsLong(0));

    this.height = (int) (getField(dir, TIFFImageDecoder.TIFF_IMAGE_LENGTH, "Image Length").getAsLong(0));

    // Set a preliminary band count. This may be changed later as needed.
    this.numBands = samplesPerPixel;

    // Figure out if any extra samples are present.
    final TIFFField efield = dir.getField(TIFFImageDecoder.TIFF_EXTRA_SAMPLES);
    final int extraSamples = efield == null ? 0 : (int) efield.getAsLong(0);

    if (dir.getField(TIFFImageDecoder.TIFF_TILE_OFFSETS) != null) {
      // Image is in tiled format
      this.isTiled = true;

      this.tileWidth = (int) (getField(dir, TIFFImageDecoder.TIFF_TILE_WIDTH, "Tile Width").getAsLong(0));
      this.tileHeight = (int) (getField(dir, TIFFImageDecoder.TIFF_TILE_LENGTH, "Tile Length").getAsLong(0));
      this.tileOffsets = (getField(dir, TIFFImageDecoder.TIFF_TILE_OFFSETS, "Tile Offsets")).getAsLongs();

      this.tileByteCounts = getFieldAsLongs(getField(dir, TIFFImageDecoder.TIFF_TILE_BYTE_COUNTS, "Tile Byte Counts"));

    } else {

      // Image is in stripped format, looks like tiles to us
      this.isTiled = false;

      // Note: Some legacy files may have tile width and height
      // written but use the strip offsets and byte counts fields
      // instead of the tile offsets and byte counts. Therefore
      // we default here to the tile dimensions if they are written.
      this.tileWidth = dir.getField(TIFFImageDecoder.TIFF_TILE_WIDTH) != null
          ? (int) dir.getFieldAsLong(TIFFImageDecoder.TIFF_TILE_WIDTH)
          : this.width;
      final TIFFField field = dir.getField(TIFFImageDecoder.TIFF_ROWS_PER_STRIP);
      if (field == null) {
        // Default is infinity (2^32 -1), basically the entire image
        // TODO: Can do a better job of tiling here
        this.tileHeight = dir.getField(TIFFImageDecoder.TIFF_TILE_LENGTH) != null
            ? (int) dir.getFieldAsLong(TIFFImageDecoder.TIFF_TILE_LENGTH)
            : this.height;
      } else {
        final long l = field.getAsLong(0);
        long infinity = 1;
        infinity = (infinity << 32) - 1;
        if (l == infinity || l > this.height) {
          // 2^32 - 1 (effectively infinity, entire image is 1 strip)
          // or RowsPerStrip > ImageLength so clamp as having a tile
          // larger than the image is pointless.
          this.tileHeight = this.height;
        } else {
          this.tileHeight = (int) l;
        }
      }

      final TIFFField tileOffsetsField = getField(dir, TIFFImageDecoder.TIFF_STRIP_OFFSETS, "Strip Offsets");
      this.tileOffsets = getFieldAsLongs(tileOffsetsField);

      final TIFFField tileByteCountsField = dir.getField(TIFFImageDecoder.TIFF_STRIP_BYTE_COUNTS);
      if (tileByteCountsField == null) {
        // Attempt to infer the number of bytes in each strip.
        final int totalBytes = ((this.sampleSize + 7) / 8) * this.numBands * this.width * this.height;
        final int bytesPerStrip = ((this.sampleSize + 7) / 8) * this.numBands * this.width * this.tileHeight;
        int cumulativeBytes = 0;
        this.tileByteCounts = new long[this.tileOffsets.length];
        for (int i = 0; i < this.tileOffsets.length; i++) {
          this.tileByteCounts[i] = Math.min(totalBytes - cumulativeBytes, bytesPerStrip);
          cumulativeBytes += bytesPerStrip;
        }

        if (this.compression != COMP_NONE) {
          // Replace the stream with one that will not throw
          // an EOFException when it runs past the end.
          this.stream = new NoEOFStream(stream);
        }
      } else {
        this.tileByteCounts = getFieldAsLongs(tileByteCountsField);
      }

      // Uncompressed image provided in a single tile: clamp to max bytes.
      final int maxBytes = this.width * this.height * this.numBands * ((this.sampleSize + 7) / 8);
      if (this.tileByteCounts.length == 1 && this.compression == COMP_NONE && this.tileByteCounts[0] > maxBytes) {
        this.tileByteCounts[0] = maxBytes;
      }
    }

    // Calculate number of tiles and the tileSize in bytes
    this.tilesX = (this.width + this.tileWidth - 1) / this.tileWidth;
    this.tilesY = (this.height + this.tileHeight - 1) / this.tileHeight;
    this.tileSize = this.tileWidth * this.tileHeight * this.numBands;

    // Check whether big endian or little endian format is used.
    this.isBigEndian = dir.isBigEndian();

    final TIFFField fillOrderField = dir.getField(TIFFImageDecoder.TIFF_FILL_ORDER);
    if (fillOrderField != null) {
      this.fillOrder = fillOrderField.getAsInt(0);
    } else {
      // Default Fill Order
      this.fillOrder = 1;
    }

    switch (this.compression) {
      case COMP_NONE:
      case COMP_PACKBITS:
        // Do nothing.
        break;
      case COMP_DEFLATE:
        this.inflater = new Inflater();
        break;
      case COMP_FAX_G3_1D:
      case COMP_FAX_G3_2D:
      case COMP_FAX_G4_2D:
        if (this.sampleSize != 1) {
          throw new RuntimeException(JaiI18N.getString("TIFFImage7"));
        }

        // Fax T.4 compression options
        if (this.compression == 3) {
          final TIFFField t4OptionsField = dir.getField(TIFFImageDecoder.TIFF_T4_OPTIONS);
          if (t4OptionsField != null) {
            this.tiffT4Options = t4OptionsField.getAsLong(0);
          } else {
            // Use default value
            this.tiffT4Options = 0;
          }
        }

        // Fax T.6 compression options
        if (this.compression == 4) {
          final TIFFField t6OptionsField = dir.getField(TIFFImageDecoder.TIFF_T6_OPTIONS);
          if (t6OptionsField != null) {
            this.tiffT6Options = t6OptionsField.getAsLong(0);
          } else {
            // Use default value
            this.tiffT6Options = 0;
          }
        }

        // Fax encoding, need to create the Fax decoder.
        this.decoder = new TIFFFaxDecoder(this.fillOrder, this.tileWidth, this.tileHeight);
        break;

      case COMP_LZW:
        // LZW compression used, need to create the LZW decoder.
        final TIFFField predictorField = dir.getField(TIFFImageDecoder.TIFF_PREDICTOR);

        if (predictorField == null) {
          this.predictor = 1;
        } else {
          this.predictor = predictorField.getAsInt(0);

          if (this.predictor != 1 && this.predictor != 2) {
            throw new RuntimeException(JaiI18N.getString("TIFFImage8"));
          }

          if (this.predictor == 2 && this.sampleSize != 8) {
            throw new RuntimeException(this.sampleSize + JaiI18N.getString("TIFFImage9"));
          }
        }

        this.lzwDecoder = new TIFFLZWDecoder(this.tileWidth, this.predictor, samplesPerPixel);
        break;

      case COMP_JPEG_OLD:
        throw new RuntimeException(JaiI18N.getString("TIFFImage15"));

//      case COMP_JPEG_TTN2:
//        if (!(this.sampleSize == 8
//            && ((this.imageType == TYPE_GRAY && samplesPerPixel == 1)
//                || (this.imageType == TYPE_PALETTE && samplesPerPixel == 1)
//                || (this.imageType == TYPE_RGB && samplesPerPixel == 3)))) {
//          throw new RuntimeException(JaiI18N.getString("TIFFImage16"));
//        }
//
//        // Create decodeParam from JPEGTables field if present.
//        if (dir.isTagPresent(TIFF_JPEG_TABLES)) {
//          final TIFFField jpegTableField = dir.getField(TIFF_JPEG_TABLES);
//          final byte[] jpegTable = jpegTableField.getAsBytes();
//          final ByteArrayInputStream tableStream = new ByteArrayInputStream(jpegTable);
//          final JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(tableStream);
//          decoder.decodeAsRaster();
//          this.decodeParam = decoder.getJPEGDecodeParam();
//        }
//
//        break;
      default:
        throw new RuntimeException(JaiI18N.getString("TIFFImage10"));
    }

    switch (this.imageType) {
      case TYPE_BILEVEL:
      case TYPE_GRAY_4BIT:
        this.sampleModel = new MultiPixelPackedSampleModel(
            this.dataType,
            this.tileWidth,
            this.tileHeight,
            this.sampleSize);
        if (this.imageType == TYPE_BILEVEL) {
          final byte[] map = new byte[]{ (byte) (this.isWhiteZero ? 255 : 0), (byte) (this.isWhiteZero ? 0 : 255) };
          this.colorModel = new IndexColorModel(1, 2, map, map, map);
        } else {
          this.colorModel = ImageCodec.createGrayIndexColorModel(this.sampleModel, !this.isWhiteZero);
        }
        break;

      case TYPE_GRAY:
      case TYPE_GRAY_ALPHA:
      case TYPE_RGB:
      case TYPE_RGB_ALPHA:
      case TYPE_CMYK:
        // Create a pixel interleaved SampleModel with decreasing
        // band offsets.
        final int[] RGBOffsets = new int[this.numBands];
        if (this.compression == COMP_JPEG_TTN2) {
          for (int i = 0; i < this.numBands; i++) {
            RGBOffsets[i] = this.numBands - 1 - i;
          }
        } else {
          for (int i = 0; i < this.numBands; i++) {
            RGBOffsets[i] = i;
          }
        }
        this.sampleModel = createPixelInterleavedSampleModel(
            this.dataType,
            this.tileWidth,
            this.tileHeight,
            this.numBands,
            this.numBands * this.tileWidth,
            RGBOffsets);

        if (this.imageType == TYPE_GRAY || this.imageType == TYPE_RGB) {
          this.colorModel = ImageCodec.createComponentColorModel(this.sampleModel);
        } else if (this.imageType == TYPE_CMYK) {
          this.colorModel = ImageCodec.createComponentColorModel(this.sampleModel, SimpleCMYKColorSpace.getInstance());
        } else { // hasAlpha
                   // Transparency.OPAQUE signifies image data that is
                 // completely opaque, meaning that all pixels have an alpha
                 // value of 1.0. So the extra band gets ignored, which is
                 // what we want.
          int transparency = Transparency.OPAQUE;
          if (extraSamples == 1 || extraSamples == 2) {
            // associated (premultiplied) alpha when == 1
            // unassociated alpha when ==2
            // Fix bug: 4699316
            transparency = Transparency.TRANSLUCENT;
          }

          this.colorModel = createAlphaComponentColorModel(
              this.dataType,
              this.numBands,
              extraSamples == 1,
              transparency);
        }
        break;

      case TYPE_GENERIC:
      case TYPE_YCBCR_SUB:
        // For this case we can't display the image, so we create a
        // SampleModel with increasing bandOffsets, and keep the
        // ColorModel as null, as there is no appropriate ColorModel.

        final int[] bandOffsets = new int[this.numBands];
        for (int i = 0; i < this.numBands; i++) {
          bandOffsets[i] = i;
        }

        this.sampleModel = createPixelInterleavedSampleModel(
            this.dataType,
            this.tileWidth,
            this.tileHeight,
            this.numBands,
            this.numBands * this.tileWidth,
            bandOffsets);
        this.colorModel = null;
        break;

      case TYPE_PALETTE:
        // Get the colormap
        final TIFFField cfield = getField(dir, TIFFImageDecoder.TIFF_COLORMAP, "Colormap");
        this.colormap = cfield.getAsChars();

        // Could be either 1 or 3 bands depending on whether we use
        // IndexColorModel or not.
        if (this.decodePaletteAsShorts) {
          this.numBands = 3;

          // If no SampleFormat tag was specified and if the
          // sampleSize is less than or equal to 8, then the
          // dataType was initially set to byte, but now we want to
          // expand the palette as shorts, so the dataType should
          // be ushort.
          if (this.dataType == DataBuffer.TYPE_BYTE) {
            this.dataType = DataBuffer.TYPE_USHORT;
          }

          // Data will have to be unpacked into a 3 band short image
          // as we do not have a IndexColorModel that can deal with
          // a colormodel whose entries are of short data type.
          this.sampleModel = RasterFactory
              .createPixelInterleavedSampleModel(this.dataType, this.tileWidth, this.tileHeight, this.numBands);
          this.colorModel = ImageCodec.createComponentColorModel(this.sampleModel);

        } else {

          this.numBands = 1;

          if (this.sampleSize == 4) {
            // Pixel data will not be unpacked, will use MPPSM to store
            // packed data and IndexColorModel to do the unpacking.
            this.sampleModel = new MultiPixelPackedSampleModel(
                DataBuffer.TYPE_BYTE,
                this.tileWidth,
                this.tileHeight,
                this.sampleSize);
          } else if (this.sampleSize == 8) {
            this.sampleModel = RasterFactory.createPixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE,
                this.tileWidth,
                this.tileHeight,
                this.numBands);
          } else if (this.sampleSize == 16) {

            // Here datatype has to be unsigned since we are storing
            // indices into the IndexColorModel palette. Ofcourse
            // the actual palette entries are allowed to be negative.
            this.dataType = DataBuffer.TYPE_USHORT;
            this.sampleModel = RasterFactory.createPixelInterleavedSampleModel(
                DataBuffer.TYPE_USHORT,
                this.tileWidth,
                this.tileHeight,
                this.numBands);
          }

          final int bandLength = this.colormap.length / 3;
          final byte r[] = new byte[bandLength];
          final byte g[] = new byte[bandLength];
          final byte b[] = new byte[bandLength];

          final int gIndex = bandLength;
          final int bIndex = bandLength * 2;

          if (this.dataType == DataBuffer.TYPE_SHORT) {

            for (int i = 0; i < bandLength; i++) {
              r[i] = param.decodeSigned16BitsTo8Bits((short) this.colormap[i]);
              g[i] = param.decodeSigned16BitsTo8Bits((short) this.colormap[gIndex + i]);
              b[i] = param.decodeSigned16BitsTo8Bits((short) this.colormap[bIndex + i]);
            }

          } else {

            for (int i = 0; i < bandLength; i++) {
              r[i] = param.decode16BitsTo8Bits(this.colormap[i] & 0xffff);
              g[i] = param.decode16BitsTo8Bits(this.colormap[gIndex + i] & 0xffff);
              b[i] = param.decode16BitsTo8Bits(this.colormap[bIndex + i] & 0xffff);
            }

          }

          this.colorModel = new IndexColorModel(this.sampleSize, bandLength, r, g, b);
        }
        break;

      default:
        throw new RuntimeException("TIFFImage4");
    }
  }

  /**
   * Reads a private IFD from a given offset in the stream.  This
   * method may be used to obtain IFDs that are referenced
   * only by private tag values.
   */
  public TIFFDirectory getPrivateIFD(final long offset) throws IOException {
    return new TIFFDirectory(this.stream, offset, 0);
  }

  /**
   * Returns tile (tileX, tileY) as a Raster.
   */
  @Override
  public synchronized Raster getTile(final int tileX, final int tileY) {
    // Check parameters.
    if ((tileX < 0) || (tileX >= this.tilesX) || (tileY < 0) || (tileY >= this.tilesY)) {
      throw new IllegalArgumentException(JaiI18N.getString("TIFFImage12"));
    }

    // The tile to return.
    WritableRaster tile = null;

    // Synchronize the rest of the method in case other TIFFImage
    // instances using the same stream were created by the same
    // TIFFImageDecoder. This fixes 4690773.
    synchronized (this.stream) {

      // Get the data array out of the DataBuffer
      byte bdata[] = null;
      short sdata[] = null;
      int idata[] = null;
      float fdata[] = null;
      final DataBuffer buffer = this.sampleModel.createDataBuffer();

      final int dataType = this.sampleModel.getDataType();
      if (dataType == DataBuffer.TYPE_BYTE) {
        bdata = ((DataBufferByte) buffer).getData();
      } else if (dataType == DataBuffer.TYPE_USHORT) {
        sdata = ((DataBufferUShort) buffer).getData();
      } else if (dataType == DataBuffer.TYPE_SHORT) {
        sdata = ((DataBufferShort) buffer).getData();
      } else if (dataType == DataBuffer.TYPE_INT) {
        idata = ((DataBufferInt) buffer).getData();
      } else if (dataType == DataBuffer.TYPE_FLOAT) {
        if (buffer instanceof DataBufferFloat) {
          fdata = ((DataBufferFloat) buffer).getData();
        } else {
          // This is a hack to make this work with JAI which in some
          // cases downcasts the DataBuffer to a type-specific class.
          // In the case of float data this current means the JAI class
          // javax.media.jai.DataBufferFloat.
          try {
            final Method getDataMethod = buffer.getClass().getMethod("getData", null);
            fdata = (float[]) getDataMethod.invoke(buffer, null);
          } catch (final Exception e) {
            final String message = JaiI18N.getString("TIFFImage18");
            ImagingListenerProxy.errorOccurred(message, new ImagingException(message, e), this, false);
            //                    throw new RuntimeException(JaiI18N.getString("TIFFImage18"));
          }
        }
      }

      tile = RasterFactory.createWritableRaster(this.sampleModel, buffer, new Point(tileXToX(tileX), tileYToY(tileY)));

      // Save original file pointer position and seek to tile data location.
      long save_offset = 0;
      try {
        save_offset = this.stream.getFilePointer();
        this.stream.seek(this.tileOffsets[tileY * this.tilesX + tileX]);
      } catch (final IOException ioe) {
        final String message = JaiI18N.getString("TIFFImage13");
        ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
        //	    throw new RuntimeException(JaiI18N.getString("TIFFImage13"));
      }

      // Number of bytes in this tile (strip) after compression.
      final int byteCount = (int) this.tileByteCounts[tileY * this.tilesX + tileX];

      // Find out the number of bytes in the current tile. If the image is
      // tiled this may include pixels which are outside of the image bounds
      // if the image width and height are not multiples of the tile width
      // and height respectively.
      final Rectangle tileRect = new Rectangle(tileXToX(tileX), tileYToY(tileY), this.tileWidth, this.tileHeight);
      final Rectangle newRect = this.isTiled ? tileRect : tileRect.intersection(getBounds());
      final int unitsInThisTile = newRect.width * newRect.height * this.numBands;

      // Allocate read buffer if needed.
      byte data[] = this.compression != COMP_NONE || this.imageType == TYPE_PALETTE ? new byte[byteCount] : null;

      // Read the data, uncompressing as needed. There are four cases:
      // bilevel, palette-RGB, 4-bit grayscale, and everything else.
      if (this.imageType == TYPE_BILEVEL) { // bilevel
        try {
          if (this.compression == COMP_PACKBITS) {
            this.stream.readFully(data, 0, byteCount);

            // Since the decompressed data will still be packed
            // 8 pixels into 1 byte, calculate bytesInThisTile
            int bytesInThisTile;
            if ((newRect.width % 8) == 0) {
              bytesInThisTile = (newRect.width / 8) * newRect.height;
            } else {
              bytesInThisTile = (newRect.width / 8 + 1) * newRect.height;
            }
            decodePackbits(data, bytesInThisTile, bdata);
          } else if (this.compression == COMP_LZW) {
            this.stream.readFully(data, 0, byteCount);
            this.lzwDecoder.decode(data, bdata, newRect.height);
          } else if (this.compression == COMP_FAX_G3_1D) {
            this.stream.readFully(data, 0, byteCount);
            this.decoder.decode1D(bdata, data, 0, newRect.height);
          } else if (this.compression == COMP_FAX_G3_2D) {
            this.stream.readFully(data, 0, byteCount);
            this.decoder.decode2D(bdata, data, 0, newRect.height, this.tiffT4Options);
          } else if (this.compression == COMP_FAX_G4_2D) {
            this.stream.readFully(data, 0, byteCount);
            this.decoder.decodeT6(bdata, data, 0, newRect.height, this.tiffT6Options);
          } else if (this.compression == COMP_DEFLATE) {
            this.stream.readFully(data, 0, byteCount);
            inflate(data, bdata);
          } else if (this.compression == COMP_NONE) {
            this.stream.readFully(bdata, 0, byteCount);
          }

          this.stream.seek(save_offset);
        } catch (final IOException ioe) {
          final String message = JaiI18N.getString("TIFFImage13");
          ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
          //		throw new RuntimeException(JaiI18N.getString("TIFFImage13"));
        }
      } else if (this.imageType == TYPE_PALETTE) { // palette-RGB
        if (this.sampleSize == 16) {

          if (this.decodePaletteAsShorts) {

            short tempData[] = null;

            // At this point the data is 1 banded and will
            // become 3 banded only after we've done the palette
            // lookup, since unitsInThisTile was calculated with
            // 3 bands, we need to divide this by 3.
            final int unitsBeforeLookup = unitsInThisTile / 3;

            // Since unitsBeforeLookup is the number of shorts,
            // but we do our decompression in terms of bytes, we
            // need to multiply it by 2 in order to figure out
            // how many bytes we'll get after decompression.
            final int entries = unitsBeforeLookup * 2;

            // Read the data, if compressed, decode it, reset the pointer
            try {

              if (this.compression == COMP_PACKBITS) {

                this.stream.readFully(data, 0, byteCount);

                final byte byteArray[] = new byte[entries];
                decodePackbits(data, entries, byteArray);
                tempData = new short[unitsBeforeLookup];
                interpretBytesAsShorts(byteArray, tempData, unitsBeforeLookup);

              } else if (this.compression == COMP_LZW) {

                // Read in all the compressed data for this tile
                this.stream.readFully(data, 0, byteCount);

                final byte byteArray[] = new byte[entries];
                this.lzwDecoder.decode(data, byteArray, newRect.height);
                tempData = new short[unitsBeforeLookup];
                interpretBytesAsShorts(byteArray, tempData, unitsBeforeLookup);

              } else if (this.compression == COMP_DEFLATE) {

                this.stream.readFully(data, 0, byteCount);
                final byte byteArray[] = new byte[entries];
                inflate(data, byteArray);
                tempData = new short[unitsBeforeLookup];
                interpretBytesAsShorts(byteArray, tempData, unitsBeforeLookup);

              } else if (this.compression == COMP_NONE) {

                // byteCount tells us how many bytes are there
                // in this tile, but we need to read in shorts,
                // which will take half the space, so while
                // allocating we divide byteCount by 2.
                tempData = new short[byteCount / 2];
                readShorts(byteCount / 2, tempData);
              }

              this.stream.seek(save_offset);

            } catch (final IOException ioe) {
              final String message = JaiI18N.getString("TIFFImage13");
              ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
              //			throw new RuntimeException(
              //					JaiI18N.getString("TIFFImage13"));
            }

            if (dataType == DataBuffer.TYPE_USHORT) {

              // Expand the palette image into an rgb image with ushort
              // data type.
              int cmapValue;
              int count = 0, lookup;
              final int len = this.colormap.length / 3;
              final int len2 = len * 2;
              for (int i = 0; i < unitsBeforeLookup; i++) {
                // Get the index into the colormap
                lookup = tempData[i] & 0xffff;
                // Get the blue value
                cmapValue = this.colormap[lookup + len2];
                sdata[count++] = (short) (cmapValue & 0xffff);
                // Get the green value
                cmapValue = this.colormap[lookup + len];
                sdata[count++] = (short) (cmapValue & 0xffff);
                // Get the red value
                cmapValue = this.colormap[lookup];
                sdata[count++] = (short) (cmapValue & 0xffff);
              }

            } else if (dataType == DataBuffer.TYPE_SHORT) {

              // Expand the palette image into an rgb image with
              // short data type.
              int cmapValue;
              int count = 0, lookup;
              final int len = this.colormap.length / 3;
              final int len2 = len * 2;
              for (int i = 0; i < unitsBeforeLookup; i++) {
                // Get the index into the colormap
                lookup = tempData[i] & 0xffff;
                // Get the blue value
                cmapValue = this.colormap[lookup + len2];
                sdata[count++] = (short) cmapValue;
                // Get the green value
                cmapValue = this.colormap[lookup + len];
                sdata[count++] = (short) cmapValue;
                // Get the red value
                cmapValue = this.colormap[lookup];
                sdata[count++] = (short) cmapValue;
              }
            }

          } else {

            // No lookup being done here, when RGB values are needed,
            // the associated IndexColorModel can be used to get them.

            try {

              if (this.compression == COMP_PACKBITS) {

                this.stream.readFully(data, 0, byteCount);

                // Since unitsInThisTile is the number of shorts,
                // but we do our decompression in terms of bytes, we
                // need to multiply unitsInThisTile by 2 in order to
                // figure out how many bytes we'll get after
                // decompression.
                final int bytesInThisTile = unitsInThisTile * 2;

                final byte byteArray[] = new byte[bytesInThisTile];
                decodePackbits(data, bytesInThisTile, byteArray);
                interpretBytesAsShorts(byteArray, sdata, unitsInThisTile);

              } else if (this.compression == COMP_LZW) {

                this.stream.readFully(data, 0, byteCount);

                // Since unitsInThisTile is the number of shorts,
                // but we do our decompression in terms of bytes, we
                // need to multiply unitsInThisTile by 2 in order to
                // figure out how many bytes we'll get after
                // decompression.
                final byte byteArray[] = new byte[unitsInThisTile * 2];
                this.lzwDecoder.decode(data, byteArray, newRect.height);
                interpretBytesAsShorts(byteArray, sdata, unitsInThisTile);

              } else if (this.compression == COMP_DEFLATE) {

                this.stream.readFully(data, 0, byteCount);
                final byte byteArray[] = new byte[unitsInThisTile * 2];
                inflate(data, byteArray);
                interpretBytesAsShorts(byteArray, sdata, unitsInThisTile);

              } else if (this.compression == COMP_NONE) {

                readShorts(byteCount / 2, sdata);
              }

              this.stream.seek(save_offset);

            } catch (final IOException ioe) {
              final String message = JaiI18N.getString("TIFFImage13");
              ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
              //			throw new RuntimeException(
              //					JaiI18N.getString("TIFFImage13"));
            }
          }

        } else if (this.sampleSize == 8) {

          if (this.decodePaletteAsShorts) {

            byte tempData[] = null;

            // At this point the data is 1 banded and will
            // become 3 banded only after we've done the palette
            // lookup, since unitsInThisTile was calculated with
            // 3 bands, we need to divide this by 3.
            final int unitsBeforeLookup = unitsInThisTile / 3;

            // Read the data, if compressed, decode it, reset the pointer
            try {

              if (this.compression == COMP_PACKBITS) {

                this.stream.readFully(data, 0, byteCount);
                tempData = new byte[unitsBeforeLookup];
                decodePackbits(data, unitsBeforeLookup, tempData);

              } else if (this.compression == COMP_LZW) {

                this.stream.readFully(data, 0, byteCount);
                tempData = new byte[unitsBeforeLookup];
                this.lzwDecoder.decode(data, tempData, newRect.height);

//              } else if (this.compression == COMP_JPEG_TTN2) {
//
//                this.stream.readFully(data, 0, byteCount);
//                final Raster tempTile = decodeJPEG(
//                    data,
//                    this.decodeParam,
//                    this.colorConvertJPEG,
//                    tile.getMinX(),
//                    tile.getMinY());
//                final int[] tempPixels = new int[unitsBeforeLookup];
//                tempTile.getPixels(tile.getMinX(), tile.getMinY(), tile.getWidth(), tile.getHeight(), tempPixels);
//                tempData = new byte[unitsBeforeLookup];
//                for (int i = 0; i < unitsBeforeLookup; i++) {
//                  tempData[i] = (byte) tempPixels[i];
//                }

              } else if (this.compression == COMP_DEFLATE) {

                this.stream.readFully(data, 0, byteCount);
                tempData = new byte[unitsBeforeLookup];
                inflate(data, tempData);

              } else if (this.compression == COMP_NONE) {

                tempData = new byte[byteCount];
                this.stream.readFully(tempData, 0, byteCount);
              }

              this.stream.seek(save_offset);

            } catch (final IOException ioe) {
              final String message = JaiI18N.getString("TIFFImage13");
              ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
              //		throw new RuntimeException(
              //					JaiI18N.getString("TIFFImage13"));
            }

            // Expand the palette image into an rgb image with ushort
            // data type.
            int cmapValue;
            int count = 0, lookup;
            final int len = this.colormap.length / 3;
            final int len2 = len * 2;
            for (int i = 0; i < unitsBeforeLookup; i++) {
              // Get the index into the colormap
              lookup = tempData[i] & 0xff;
              // Get the blue value
              cmapValue = this.colormap[lookup + len2];
              sdata[count++] = (short) (cmapValue & 0xffff);
              // Get the green value
              cmapValue = this.colormap[lookup + len];
              sdata[count++] = (short) (cmapValue & 0xffff);
              // Get the red value
              cmapValue = this.colormap[lookup];
              sdata[count++] = (short) (cmapValue & 0xffff);
            }
          } else {

            // No lookup being done here, when RGB values are needed,
            // the associated IndexColorModel can be used to get them.

            try {

              if (this.compression == COMP_PACKBITS) {

                this.stream.readFully(data, 0, byteCount);
                decodePackbits(data, unitsInThisTile, bdata);

              } else if (this.compression == COMP_LZW) {

                this.stream.readFully(data, 0, byteCount);
                this.lzwDecoder.decode(data, bdata, newRect.height);

//              } else if (this.compression == COMP_JPEG_TTN2) {
//
//                this.stream.readFully(data, 0, byteCount);
//                tile.setRect(decodeJPEG(data, this.decodeParam, this.colorConvertJPEG, tile.getMinX(), tile.getMinY()));

              } else if (this.compression == COMP_DEFLATE) {

                this.stream.readFully(data, 0, byteCount);
                inflate(data, bdata);

              } else if (this.compression == COMP_NONE) {

                this.stream.readFully(bdata, 0, byteCount);
              }

              this.stream.seek(save_offset);

            } catch (final IOException ioe) {
              final String message = JaiI18N.getString("TIFFImage13");
              ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
              //		throw new RuntimeException(
              //					JaiI18N.getString("TIFFImage13"));
            }
          }

        } else if (this.sampleSize == 4) {

          final int padding = (newRect.width % 2 == 0) ? 0 : 1;
          final int bytesPostDecoding = ((newRect.width / 2 + padding) * newRect.height);

          // Output short images
          if (this.decodePaletteAsShorts) {

            byte tempData[] = null;

            try {
              this.stream.readFully(data, 0, byteCount);
              this.stream.seek(save_offset);
            } catch (final IOException ioe) {
              final String message = JaiI18N.getString("TIFFImage13");
              ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
              //			throw new RuntimeException(
              //					JaiI18N.getString("TIFFImage13"));
            }

            // If compressed, decode the data.
            if (this.compression == COMP_PACKBITS) {

              tempData = new byte[bytesPostDecoding];
              decodePackbits(data, bytesPostDecoding, tempData);

            } else if (this.compression == COMP_LZW) {

              tempData = new byte[bytesPostDecoding];
              this.lzwDecoder.decode(data, tempData, newRect.height);

            } else if (this.compression == COMP_DEFLATE) {

              tempData = new byte[bytesPostDecoding];
              inflate(data, tempData);

            } else if (this.compression == COMP_NONE) {

              tempData = data;
            }

            final int bytes = unitsInThisTile / 3;

            // Unpack the 2 pixels packed into each byte.
            data = new byte[bytes];

            int srcCount = 0, dstCount = 0;
            for (int j = 0; j < newRect.height; j++) {
              for (int i = 0; i < newRect.width / 2; i++) {
                data[dstCount++] = (byte) ((tempData[srcCount] & 0xf0) >> 4);
                data[dstCount++] = (byte) (tempData[srcCount++] & 0x0f);
              }

              if (padding == 1) {
                data[dstCount++] = (byte) ((tempData[srcCount++] & 0xf0) >> 4);
              }
            }

            final int len = this.colormap.length / 3;
            final int len2 = len * 2;
            int cmapValue, lookup;
            int count = 0;
            for (int i = 0; i < bytes; i++) {
              lookup = data[i] & 0xff;
              cmapValue = this.colormap[lookup + len2];
              sdata[count++] = (short) (cmapValue & 0xffff);
              cmapValue = this.colormap[lookup + len];
              sdata[count++] = (short) (cmapValue & 0xffff);
              cmapValue = this.colormap[lookup];
              sdata[count++] = (short) (cmapValue & 0xffff);
            }
          } else {

            // Output byte values, use IndexColorModel for unpacking
            try {

              // If compressed, decode the data.
              if (this.compression == COMP_PACKBITS) {

                this.stream.readFully(data, 0, byteCount);
                decodePackbits(data, bytesPostDecoding, bdata);

              } else if (this.compression == COMP_LZW) {

                this.stream.readFully(data, 0, byteCount);
                this.lzwDecoder.decode(data, bdata, newRect.height);

              } else if (this.compression == COMP_DEFLATE) {

                this.stream.readFully(data, 0, byteCount);
                inflate(data, bdata);

              } else if (this.compression == COMP_NONE) {

                this.stream.readFully(bdata, 0, byteCount);
              }

              this.stream.seek(save_offset);

            } catch (final IOException ioe) {
              final String message = JaiI18N.getString("TIFFImage13");
              ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
              //			throw new RuntimeException(
              //					JaiI18N.getString("TIFFImage13"));
            }
          }
        }
      } else if (this.imageType == TYPE_GRAY_4BIT) { // 4-bit gray
        try {
          if (this.compression == COMP_PACKBITS) {

            this.stream.readFully(data, 0, byteCount);

            // Since the decompressed data will still be packed
            // 2 pixels into 1 byte, calculate bytesInThisTile
            int bytesInThisTile;
            if ((newRect.width % 8) == 0) {
              bytesInThisTile = (newRect.width / 2) * newRect.height;
            } else {
              bytesInThisTile = (newRect.width / 2 + 1) * newRect.height;
            }

            decodePackbits(data, bytesInThisTile, bdata);

          } else if (this.compression == COMP_LZW) {

            this.stream.readFully(data, 0, byteCount);
            this.lzwDecoder.decode(data, bdata, newRect.height);

          } else if (this.compression == COMP_DEFLATE) {

            this.stream.readFully(data, 0, byteCount);
            inflate(data, bdata);

          } else {

            this.stream.readFully(bdata, 0, byteCount);
          }

          this.stream.seek(save_offset);
        } catch (final IOException ioe) {
          final String message = JaiI18N.getString("TIFFImage13");
          ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
          //		throw new RuntimeException(JaiI18N.getString("TIFFImage13"));
        }
      } else { // everything else
        try {

          if (this.sampleSize == 8) {

            if (this.compression == COMP_NONE) {

              this.stream.readFully(bdata, 0, byteCount);

            } else if (this.compression == COMP_LZW) {

              this.stream.readFully(data, 0, byteCount);
              this.lzwDecoder.decode(data, bdata, newRect.height);

            } else if (this.compression == COMP_PACKBITS) {

              this.stream.readFully(data, 0, byteCount);
              decodePackbits(data, unitsInThisTile, bdata);
//
//            } else if (this.compression == COMP_JPEG_TTN2) {
//
//              this.stream.readFully(data, 0, byteCount);
//              tile.setRect(decodeJPEG(data, this.decodeParam, this.colorConvertJPEG, tile.getMinX(), tile.getMinY()));
            } else if (this.compression == COMP_DEFLATE) {

              this.stream.readFully(data, 0, byteCount);
              inflate(data, bdata);
            }

          } else if (this.sampleSize == 16) {

            if (this.compression == COMP_NONE) {

              readShorts(byteCount / 2, sdata);

            } else if (this.compression == COMP_LZW) {

              this.stream.readFully(data, 0, byteCount);

              // Since unitsInThisTile is the number of shorts,
              // but we do our decompression in terms of bytes, we
              // need to multiply unitsInThisTile by 2 in order to
              // figure out how many bytes we'll get after
              // decompression.
              final byte byteArray[] = new byte[unitsInThisTile * 2];
              this.lzwDecoder.decode(data, byteArray, newRect.height);
              interpretBytesAsShorts(byteArray, sdata, unitsInThisTile);

            } else if (this.compression == COMP_PACKBITS) {

              this.stream.readFully(data, 0, byteCount);

              // Since unitsInThisTile is the number of shorts,
              // but we do our decompression in terms of bytes, we
              // need to multiply unitsInThisTile by 2 in order to
              // figure out how many bytes we'll get after
              // decompression.
              final int bytesInThisTile = unitsInThisTile * 2;

              final byte byteArray[] = new byte[bytesInThisTile];
              decodePackbits(data, bytesInThisTile, byteArray);
              interpretBytesAsShorts(byteArray, sdata, unitsInThisTile);
            } else if (this.compression == COMP_DEFLATE) {

              this.stream.readFully(data, 0, byteCount);
              final byte byteArray[] = new byte[unitsInThisTile * 2];
              inflate(data, byteArray);
              interpretBytesAsShorts(byteArray, sdata, unitsInThisTile);

            }
          } else if (this.sampleSize == 32 && dataType == DataBuffer.TYPE_INT) { // redundant
            if (this.compression == COMP_NONE) {

              readInts(byteCount / 4, idata);

            } else if (this.compression == COMP_LZW) {

              this.stream.readFully(data, 0, byteCount);

              // Since unitsInThisTile is the number of ints,
              // but we do our decompression in terms of bytes, we
              // need to multiply unitsInThisTile by 4 in order to
              // figure out how many bytes we'll get after
              // decompression.
              final byte byteArray[] = new byte[unitsInThisTile * 4];
              this.lzwDecoder.decode(data, byteArray, newRect.height);
              interpretBytesAsInts(byteArray, idata, unitsInThisTile);

            } else if (this.compression == COMP_PACKBITS) {

              this.stream.readFully(data, 0, byteCount);

              // Since unitsInThisTile is the number of ints,
              // but we do our decompression in terms of bytes, we
              // need to multiply unitsInThisTile by 4 in order to
              // figure out how many bytes we'll get after
              // decompression.
              final int bytesInThisTile = unitsInThisTile * 4;

              final byte byteArray[] = new byte[bytesInThisTile];
              decodePackbits(data, bytesInThisTile, byteArray);
              interpretBytesAsInts(byteArray, idata, unitsInThisTile);
            } else if (this.compression == COMP_DEFLATE) {

              this.stream.readFully(data, 0, byteCount);
              final byte byteArray[] = new byte[unitsInThisTile * 4];
              inflate(data, byteArray);
              interpretBytesAsInts(byteArray, idata, unitsInThisTile);

            }
          } else if (this.sampleSize == 32 && dataType == DataBuffer.TYPE_FLOAT) { // redundant
            if (this.compression == COMP_NONE) {

              readFloats(byteCount / 4, fdata);

            } else if (this.compression == COMP_LZW) {

              this.stream.readFully(data, 0, byteCount);

              // Since unitsInThisTile is the number of floats,
              // but we do our decompression in terms of bytes, we
              // need to multiply unitsInThisTile by 4 in order to
              // figure out how many bytes we'll get after
              // decompression.
              final byte byteArray[] = new byte[unitsInThisTile * 4];
              this.lzwDecoder.decode(data, byteArray, newRect.height);
              interpretBytesAsFloats(byteArray, fdata, unitsInThisTile);

            } else if (this.compression == COMP_PACKBITS) {

              this.stream.readFully(data, 0, byteCount);

              // Since unitsInThisTile is the number of floats,
              // but we do our decompression in terms of bytes, we
              // need to multiply unitsInThisTile by 4 in order to
              // figure out how many bytes we'll get after
              // decompression.
              final int bytesInThisTile = unitsInThisTile * 4;

              final byte byteArray[] = new byte[bytesInThisTile];
              decodePackbits(data, bytesInThisTile, byteArray);
              interpretBytesAsFloats(byteArray, fdata, unitsInThisTile);
            } else if (this.compression == COMP_DEFLATE) {

              this.stream.readFully(data, 0, byteCount);
              final byte byteArray[] = new byte[unitsInThisTile * 4];
              inflate(data, byteArray);
              interpretBytesAsFloats(byteArray, fdata, unitsInThisTile);

            }
          }

          this.stream.seek(save_offset);

        } catch (final IOException ioe) {
          final String message = JaiI18N.getString("TIFFImage13");
          ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
          //		throw new RuntimeException(JaiI18N.getString("TIFFImage13"));
        }

        // Modify the data for certain special cases.
        switch (this.imageType) {
          case TYPE_GRAY:
          case TYPE_GRAY_ALPHA:
            if (this.isWhiteZero) {
              // Since we are using a ComponentColorModel with this
              // image, we need to change the WhiteIsZero data to
              // BlackIsZero data so it will display properly.
              if (dataType == DataBuffer.TYPE_BYTE && !(this.colorModel instanceof IndexColorModel)) {

                for (int l = 0; l < bdata.length; l += this.numBands) {
                  bdata[l] = (byte) (255 - bdata[l]);
                }
              } else if (dataType == DataBuffer.TYPE_USHORT) {

                final int ushortMax = Short.MAX_VALUE - Short.MIN_VALUE;
                for (int l = 0; l < sdata.length; l += this.numBands) {
                  sdata[l] = (short) (ushortMax - sdata[l]);
                }

              } else if (dataType == DataBuffer.TYPE_SHORT) {

                for (int l = 0; l < sdata.length; l += this.numBands) {
                  sdata[l] = (short) (~sdata[l]);
                }
              } else if (dataType == DataBuffer.TYPE_INT) {

                final long uintMax = Integer.MAX_VALUE - Integer.MIN_VALUE;
                for (int l = 0; l < idata.length; l += this.numBands) {
                  idata[l] = (int) (uintMax - idata[l]);
                }
              }
            }
            break;
          case TYPE_YCBCR_SUB:
            // Post-processing for YCbCr with subsampled chrominance:
            // simply replicate the chroma channels for displayability.
            final int pixelsPerDataUnit = this.chromaSubH * this.chromaSubV;

            final int numH = newRect.width / this.chromaSubH;
            final int numV = newRect.height / this.chromaSubV;

            final byte[] tempData = new byte[numH * numV * (pixelsPerDataUnit + 2)];
            System.arraycopy(bdata, 0, tempData, 0, tempData.length);

            final int samplesPerDataUnit = pixelsPerDataUnit * 3;
            final int[] pixels = new int[samplesPerDataUnit];

            int bOffset = 0;
            final int offsetCb = pixelsPerDataUnit;
            final int offsetCr = offsetCb + 1;

            int y = newRect.y;
            for (int j = 0; j < numV; j++) {
              int x = newRect.x;
              for (int i = 0; i < numH; i++) {
                final int Cb = tempData[bOffset + offsetCb];
                final int Cr = tempData[bOffset + offsetCr];
                int k = 0;
                while (k < samplesPerDataUnit) {
                  pixels[k++] = tempData[bOffset++];
                  pixels[k++] = Cb;
                  pixels[k++] = Cr;
                }
                bOffset += 2;
                tile.setPixels(x, y, this.chromaSubH, this.chromaSubV, pixels);
                x += this.chromaSubH;
              }
              y += this.chromaSubV;
            }

            break;
        }
      }

    } // synchronized(this.stream)

    return tile;
  }

  private void readShorts(final int shortCount, final short shortArray[]) {

    // Since each short consists of 2 bytes, we need a
    // byte array of double size
    final int byteCount = 2 * shortCount;
    final byte byteArray[] = new byte[byteCount];

    try {
      this.stream.readFully(byteArray, 0, byteCount);
    } catch (final IOException ioe) {
      final String message = JaiI18N.getString("TIFFImage13");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
      //	   throw new RuntimeException(JaiI18N.getString("TIFFImage13"));
    }

    interpretBytesAsShorts(byteArray, shortArray, shortCount);
  }

  private void readInts(final int intCount, final int intArray[]) {

    // Since each int consists of 4 bytes, we need a
    // byte array of quadruple size
    final int byteCount = 4 * intCount;
    final byte byteArray[] = new byte[byteCount];

    try {
      this.stream.readFully(byteArray, 0, byteCount);
    } catch (final IOException ioe) {
      final String message = JaiI18N.getString("TIFFImage13");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
      //	   throw new RuntimeException(JaiI18N.getString("TIFFImage13"));
    }

    interpretBytesAsInts(byteArray, intArray, intCount);
  }

  private void readFloats(final int floatCount, final float floatArray[]) {

    // Since each float consists of 4 bytes, we need a
    // byte array of quadruple size
    final int byteCount = 4 * floatCount;
    final byte byteArray[] = new byte[byteCount];

    try {
      this.stream.readFully(byteArray, 0, byteCount);
    } catch (final IOException ioe) {
      final String message = JaiI18N.getString("TIFFImage13");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ioe), this, false);
      //	   throw new RuntimeException(JaiI18N.getString("TIFFImage13"));
    }

    interpretBytesAsFloats(byteArray, floatArray, floatCount);
  }

  // Method to interpret a byte array to a short array, depending on
  // whether the bytes are stored in a big endian or little endian format.
  private void interpretBytesAsShorts(final byte byteArray[], final short shortArray[], final int shortCount) {

    int j = 0;
    int firstByte, secondByte;

    if (this.isBigEndian) {

      for (int i = 0; i < shortCount; i++) {
        firstByte = byteArray[j++] & 0xff;
        secondByte = byteArray[j++] & 0xff;
        shortArray[i] = (short) ((firstByte << 8) + secondByte);
      }

    } else {

      for (int i = 0; i < shortCount; i++) {
        firstByte = byteArray[j++] & 0xff;
        secondByte = byteArray[j++] & 0xff;
        shortArray[i] = (short) ((secondByte << 8) + firstByte);
      }
    }
  }

  // Method to interpret a byte array to a int array, depending on
  // whether the bytes are stored in a big endian or little endian format.
  private void interpretBytesAsInts(final byte byteArray[], final int intArray[], final int intCount) {

    int j = 0;

    if (this.isBigEndian) {

      for (int i = 0; i < intCount; i++) {
        intArray[i] = ((byteArray[j++] & 0xff) << 24)
            | ((byteArray[j++] & 0xff) << 16)
            | ((byteArray[j++] & 0xff) << 8)
            | (byteArray[j++] & 0xff);
      }

    } else {

      for (int i = 0; i < intCount; i++) {
        intArray[i] = (byteArray[j++] & 0xff)
            | ((byteArray[j++] & 0xff) << 8)
            | ((byteArray[j++] & 0xff) << 16)
            | ((byteArray[j++] & 0xff) << 24);
      }
    }
  }

  // Method to interpret a byte array to a float array, depending on
  // whether the bytes are stored in a big endian or little endian format.
  private void interpretBytesAsFloats(final byte byteArray[], final float floatArray[], final int floatCount) {

    int j = 0;

    if (this.isBigEndian) {

      for (int i = 0; i < floatCount; i++) {
        final int value = ((byteArray[j++] & 0xff) << 24)
            | ((byteArray[j++] & 0xff) << 16)
            | ((byteArray[j++] & 0xff) << 8)
            | (byteArray[j++] & 0xff);
        floatArray[i] = Float.intBitsToFloat(value);
      }

    } else {

      for (int i = 0; i < floatCount; i++) {
        final int value = (byteArray[j++] & 0xff)
            | ((byteArray[j++] & 0xff) << 8)
            | ((byteArray[j++] & 0xff) << 16)
            | ((byteArray[j++] & 0xff) << 24);
        floatArray[i] = Float.intBitsToFloat(value);
      }
    }
  }

  // Uncompress packbits compressed image data.
  private byte[] decodePackbits(final byte data[], final int arraySize, byte[] dst) {

    if (dst == null) {
      dst = new byte[arraySize];
    }

    int srcCount = 0, dstCount = 0;
    final int srcArraySize = data.length;
    byte repeat, b;

    try {

      while (dstCount < arraySize && srcCount < srcArraySize) {

        b = data[srcCount++];

        if (b >= 0 && b <= 127) {

          // literal run packet
          for (int i = 0; i < (b + 1); i++) {
            dst[dstCount++] = data[srcCount++];
          }

        } else if (b <= -1 && b >= -127) {

          // 2 byte encoded run packet
          repeat = data[srcCount++];
          for (int i = 0; i < (-b + 1); i++) {
            dst[dstCount++] = repeat;
          }

        } else {
          // no-op packet. Do nothing
          srcCount++;
        }
      }
    } catch (final java.lang.ArrayIndexOutOfBoundsException ae) {
      final String message = JaiI18N.getString("TIFFImage14");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, ae), this, false);
      //	    throw new RuntimeException(JaiI18N.getString("TIFFImage14"));
    }

    return dst;
  }

  // Need a createColorModel().
  // Create ComponentColorModel for TYPE_RGB images
  private ComponentColorModel createAlphaComponentColorModel(
      final int dataType,
      final int numBands,
      final boolean isAlphaPremultiplied,
      final int transparency) {

    ComponentColorModel ccm = null;
    int RGBBits[] = null;
    ColorSpace cs = null;
    switch (numBands) {
      case 2: // gray+alpha
        cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        break;
      case 4: // RGB+alpha
        cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        break;
      default:
        throw new IllegalArgumentException();
    }

    if (dataType == DataBuffer.TYPE_FLOAT) {
      ccm = new FloatDoubleColorModel(cs, true, isAlphaPremultiplied, transparency, dataType);
    } else { // all other types
      int componentSize = 0;
      switch (dataType) {
        case DataBuffer.TYPE_BYTE:
          componentSize = 8;
          break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
          componentSize = 16;
          break;
        case DataBuffer.TYPE_INT:
          componentSize = 32;
          break;
        default:
          throw new IllegalArgumentException();
      }

      RGBBits = new int[numBands];
      for (int i = 0; i < numBands; i++) {
        RGBBits[i] = componentSize;
      }

      ccm = new ComponentColorModel(cs, RGBBits, true, isAlphaPremultiplied, transparency, dataType);
    }

    return ccm;
  }
}

/**
 * Wrapper class for a <code>SeekableStream</code> but which does not throw
 * an <code>EOFException</code> from <code>readFully()</code> when the end
 * of stream is encountered.
 */
// NB This is a hack to fix bug 4823200 "Make TIFF decoder robust to (comp)
// images with no strip/tile byte counts field" but there does not seem to
// be any other way to work around this without extensive code changes.
class NoEOFStream extends SeekableStream {
  private final SeekableStream stream;

  NoEOFStream(final SeekableStream ss) {
    if (ss == null) {
      throw new IllegalArgumentException();
    }

    this.stream = ss;
  }

  @Override
  public int read() throws IOException {
    final int b = this.stream.read();
    return b < 0 ? 0 : b;
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {
    final int count = this.stream.read(b, off, len);
    return count < 0 ? len : count;
  }

  @Override
  public long getFilePointer() throws IOException {
    return this.stream.getFilePointer();
  }

  @Override
  public void seek(final long pos) throws IOException {
    this.stream.seek(pos);
  }
}
