/*
 * $RCSfile: GIFImageDecoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2006-06-17 00:02:28 $
 * $State: Exp $
 */
package com.sun.media.jai.codecimpl;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.media.jai.util.ImagingException;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecodeParam;
import com.sun.media.jai.codec.ImageDecoderImpl;
import com.sun.media.jai.codec.SeekableStream;

/**
 * @since EA3
 */
public class GIFImageDecoder extends ImageDecoderImpl {

  // The global color table.
  private byte[] globalColorTable = null;

  // Whether the last page has been encountered.
  private boolean maxPageFound = false;

  // The maximum allowable page for reading.
  private int maxPage;

  // The previous page read.
  private int prevPage = -1;

  // The previous page on which getTile() was invoked in this object.
  private int prevSyncedPage = -1;

  // Map of Integer page numbers to RenderedImages.
  private final HashMap images = new HashMap();

  /**
   * Read the overall stream header and return the global color map
   * or <code>null</code>.
   */
  private static byte[] readHeader(final SeekableStream input) throws IOException {
    byte[] globalColorTable = null;
    try {
      // Skip the version string and logical screen dimensions.
      input.skipBytes(10);

      final int packedFields = input.readUnsignedByte();
      final boolean globalColorTableFlag = (packedFields & 0x80) != 0;
      final int numGCTEntries = 1 << ((packedFields & 0x7) + 1);

      final int backgroundColorIndex = input.readUnsignedByte();

      // Read the aspect ratio but ignore the returned value.
      input.read();

      if (globalColorTableFlag) {
        globalColorTable = new byte[3 * numGCTEntries];
        input.readFully(globalColorTable);
      } else {
        globalColorTable = null;
      }
    } catch (final IOException e) {
      final String message = JaiI18N.getString("GIFImageDecoder0");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, e), GIFImageDecoder.class, false);
      //            throw new IOException(JaiI18N.getString("GIFImageDecoder0"));
    }

    return globalColorTable;
  }

  public GIFImageDecoder(final SeekableStream input, final ImageDecodeParam param) {
    super(input, param);
  }

  public GIFImageDecoder(final InputStream input, final ImageDecodeParam param) {
    super(input, param);
  }

  @Override
  public int getNumPages() throws IOException {
    int page = this.prevPage + 1;

    while (!this.maxPageFound) {
      try {
        decodeAsRenderedImage(page++);
      } catch (final IOException e) {
        // Ignore
      }
    }

    return this.maxPage + 1;
  }

  @Override
  public synchronized RenderedImage decodeAsRenderedImage(final int page) throws IOException {

    // Verify that the index is in range.
    if (page < 0 || (this.maxPageFound && page > this.maxPage)) {
      throw new IOException(JaiI18N.getString("GIFImageDecoder1"));
    }

    // Attempt to get the image from the cache.
    final Integer pageKey = new Integer(page);
    if (this.images.containsKey(pageKey)) {
      return (RenderedImage) this.images.get(pageKey);
    }

    // If the zeroth image, set the global color table.
    if (this.prevPage == -1) {
      try {
        this.globalColorTable = readHeader(this.input);
      } catch (final IOException e) {
        this.maxPageFound = true;
        this.maxPage = -1;
        throw e;
      }
    }

    // Force previous data to be read.
    if (page > 0) {
      for (int idx = this.prevSyncedPage + 1; idx < page; idx++) {
        final RenderedImage im = (RenderedImage) this.images.get(new Integer(idx));
        im.getTile(0, 0);
        this.prevSyncedPage = idx;
      }
    }

    // Read as many images as possible.
    RenderedImage image = null;
    while (this.prevPage < page) {
      final int index = this.prevPage + 1;
      RenderedImage ri = null;
      try {
        ri = new GIFImage(this.input, this.globalColorTable);
        this.images.put(new Integer(index), ri);
        if (index < page) {
          ri.getTile(0, 0);
          this.prevSyncedPage = index;
        }
        this.prevPage = index;
        if (index == page) {
          image = ri;
          break;
        }
      } catch (final IOException e) {
        this.maxPageFound = true;
        this.maxPage = this.prevPage;
        final String message = JaiI18N.getString("GIFImage3");
        ImagingListenerProxy.errorOccurred(message, new ImagingException(message, e), this, false);
        //                throw e;
      }
    }

    return image;
  }
}

/**
 * @since 1.1.1
 */
class GIFImage extends SimpleRenderedImage {
  // Constants used to control interlacing.
  private static final int[] INTERLACE_INCREMENT = { 8, 8, 4, 2, -1 };
  private static final int[] INTERLACE_OFFSET = { 0, 4, 2, 1, -1 };

  // The source stream.
  private final SeekableStream input;

  // The interlacing flag.
  private boolean interlaceFlag = false;

  // Variables used by LZW decoding
  private final byte[] block = new byte[255];
  private int blockLength = 0;
  private int bitPos = 0;
  private int nextByte = 0;
  private int initCodeSize;
  private int clearCode;
  private int eofCode;
  private int bitsLeft;

  // 32-bit lookahead buffer
  private int next32Bits = 0;

  // True if the end of the data blocks has been found,
  // and we are simply draining the 32-bit buffer
  private boolean lastBlockFound = false;

  // The current interlacing pass, starting with 0.
  private int interlacePass = 0;

  // The image's tile.
  private WritableRaster theTile = null;

  // Read blocks of 1-255 bytes, stop at a 0-length block
  private void skipBlocks() throws IOException {
    while (true) {
      final int length = this.input.readUnsignedByte();
      if (length == 0) {
        break;
      }
      this.input.skipBytes(length);
    }
  }

  /**
   * Create a new <code>GIFImage</code>.  The input stream must
   * be positioned at the start of the image, i.e., not at the
   * start of the overall stream.
   *
   * @param input the stream from which to read.
   * @param globalColorTable the global colormap of <code>null</code>.
   *
   * @throws IOException.
   */
  GIFImage(final SeekableStream input, final byte[] globalColorTable) throws IOException {
    this.input = input;

    byte[] localColorTable = null;
    boolean transparentColorFlag = false;
    int transparentColorIndex = 0;

    // Read the image header initializing the local color table,
    // if any, and the transparent index, if any.

    try {
      final long startPosition = input.getFilePointer();
      while (true) {
        final int blockType = input.readUnsignedByte();
        if (blockType == 0x2c) { // Image Descriptor
          // Skip image top and left position.
          input.skipBytes(4);

          this.width = input.readUnsignedShortLE();
          this.height = input.readUnsignedShortLE();

          final int idPackedFields = input.readUnsignedByte();
          final boolean localColorTableFlag = (idPackedFields & 0x80) != 0;
          this.interlaceFlag = (idPackedFields & 0x40) != 0;
          final int numLCTEntries = 1 << ((idPackedFields & 0x7) + 1);

          if (localColorTableFlag) {
            // Read color table if any
            localColorTable = new byte[3 * numLCTEntries];
            input.readFully(localColorTable);
          } else {
            localColorTable = null;
          }

          // Now positioned at start of LZW-compressed pixels
          break;
        } else if (blockType == 0x21) { // Extension block
          final int label = input.readUnsignedByte();

          if (label == 0xf9) { // Graphics Control Extension
            input.read(); // extension length
            final int gcePackedFields = input.readUnsignedByte();
            transparentColorFlag = (gcePackedFields & 0x1) != 0;

            input.skipBytes(2); // delay time

            transparentColorIndex = input.readUnsignedByte();

            input.read(); // terminator
          } else if (label == 0x1) { // Plain text extension
            // Skip content.
            input.skipBytes(13);
            // Read but do not save content.
            skipBlocks();
          } else if (label == 0xfe) { // Comment extension
            // Read but do not save content.
            skipBlocks();
          } else if (label == 0xff) { // Application extension
            // Skip content.
            input.skipBytes(12);
            // Read but do not save content.
            skipBlocks();
          } else {
            // Skip over unknown extension blocks
            int length = 0;
            do {
              length = input.readUnsignedByte();
              input.skipBytes(length);
            } while (length > 0);
          }
        } else {
          throw new IOException(JaiI18N.getString("GIFImage0") + " " + blockType + "!");
        }
      }
    } catch (final IOException ioe) {
      throw new IOException(JaiI18N.getString("GIFImage1"));
    }

    // Set the image layout from the header information.

    // Set the image and tile grid origin to (0, 0).
    this.minX = this.minY = this.tileGridXOffset = this.tileGridYOffset = 0;

    // Force the image to have a single tile.
    this.tileWidth = this.width;
    this.tileHeight = this.height;

    byte[] colorTable;
    if (localColorTable != null) {
      colorTable = localColorTable;
    } else {
      colorTable = globalColorTable;
    }

    // Normalize color table length to 2^1, 2^2, 2^4, or 2^8
    final int length = colorTable.length / 3;
    int bits;
    if (length == 2) {
      bits = 1;
    } else if (length == 4) {
      bits = 2;
    } else if (length == 8 || length == 16) {
      // Bump from 3 to 4 bits
      bits = 4;
    } else {
      // Bump to 8 bits
      bits = 8;
    }
    final int lutLength = 1 << bits;
    final byte[] r = new byte[lutLength];
    final byte[] g = new byte[lutLength];
    final byte[] b = new byte[lutLength];

    // Entries from length + 1 to lutLength - 1 will be 0
    int rgbIndex = 0;
    for (int i = 0; i < length; i++) {
      r[i] = colorTable[rgbIndex++];
      g[i] = colorTable[rgbIndex++];
      b[i] = colorTable[rgbIndex++];
    }

    final int[] bitsPerSample = new int[1];
    bitsPerSample[0] = bits;

    this.sampleModel = new PixelInterleavedSampleModel(
        DataBuffer.TYPE_BYTE,
        this.width,
        this.height,
        1,
        this.width,
        new int[]{ 0 });

    if (!transparentColorFlag) {
      if (ImageCodec.isIndicesForGrayscale(r, g, b)) {
        this.colorModel = ImageCodec.createComponentColorModel(this.sampleModel);
      } else {
        this.colorModel = new IndexColorModel(bits, r.length, r, g, b);
      }
    } else {
      this.colorModel = new IndexColorModel(bits, r.length, r, g, b, transparentColorIndex);
    }
  }

  // BEGIN LZW CODE

  private void initNext32Bits() {
    this.next32Bits = this.block[0] & 0xff;
    this.next32Bits |= (this.block[1] & 0xff) << 8;
    this.next32Bits |= (this.block[2] & 0xff) << 16;
    this.next32Bits |= this.block[3] << 24;
    this.nextByte = 4;
  }

  // Load a block (1-255 bytes) at a time, and maintain
  // a 32-bit lookahead buffer that is filled from the left
  // and extracted from the right.
  private int getCode(final int codeSize, final int codeMask) throws IOException {
    //if (bitPos + codeSize > 32) {
    if (this.bitsLeft <= 0) {
      return this.eofCode; // No more data available
    }

    final int code = (this.next32Bits >> this.bitPos) & codeMask;
    this.bitPos += codeSize;
    this.bitsLeft -= codeSize;

    // Shift in a byte of new data at a time
    while (this.bitPos >= 8 && !this.lastBlockFound) {
      this.next32Bits >>>= 8;
      this.bitPos -= 8;

      // Check if current block is out of bytes
      if (this.nextByte >= this.blockLength) {
        // Get next block size
        this.blockLength = this.input.readUnsignedByte();
        if (this.blockLength == 0) {
          this.lastBlockFound = true;
          if (this.bitsLeft < 0) {
            return this.eofCode;
          } else {
            return code;
          }
        } else {
          int left = this.blockLength;
          int off = 0;
          while (left > 0) {
            final int nbytes = this.input.read(this.block, off, left);
            off += nbytes;
            left -= nbytes;
          }

          this.bitsLeft += this.blockLength << 3;
          this.nextByte = 0;
        }
      }

      this.next32Bits |= this.block[this.nextByte++] << 24;
    }

    return code;
  }

  private void initializeStringTable(
      final int[] prefix,
      final byte[] suffix,
      final byte[] initial,
      final int[] length) {
    final int numEntries = 1 << this.initCodeSize;
    for (int i = 0; i < numEntries; i++) {
      prefix[i] = -1;
      suffix[i] = (byte) i;
      initial[i] = (byte) i;
      length[i] = 1;
    }

    // Fill in the entire table for robustness against
    // out-of-sequence codes.
    for (int i = numEntries; i < 4096; i++) {
      prefix[i] = -1;
      length[i] = 1;
    }
  }

  private Point outputPixels(final byte[] string, final int len, final Point streamPos, final byte[] rowBuf) {
    if (this.interlacePass < 0 || this.interlacePass > 3) {
      return streamPos;
    }

    for (int i = 0; i < len; i++) {
      if (streamPos.x >= this.minX) {
        rowBuf[streamPos.x - this.minX] = string[i];
      }

      // Process end-of-row
      ++streamPos.x;
      if (streamPos.x == this.width) {
        this.theTile.setDataElements(this.minX, streamPos.y, this.width, 1, rowBuf);

        streamPos.x = 0;
        if (this.interlaceFlag) {
          streamPos.y += INTERLACE_INCREMENT[this.interlacePass];
          if (streamPos.y >= this.height) {
            ++this.interlacePass;
            if (this.interlacePass > 3) {
              return streamPos;
            }
            streamPos.y = INTERLACE_OFFSET[this.interlacePass];
          }
        } else {
          ++streamPos.y;
        }
      }
    }

    return streamPos;
  }

  // END LZW CODE

  @Override
  public synchronized Raster getTile(final int tileX, final int tileY) {

    // Should be a unique tile.
    if (tileX != 0 || tileY != 0) {
      throw new IllegalArgumentException(JaiI18N.getString("GIFImage2"));
    }

    // Return the tile if it's already computed.
    if (this.theTile != null) {
      return this.theTile;
    }

    // Initialize the destination image
    this.theTile = WritableRaster.createWritableRaster(this.sampleModel, this.sampleModel.createDataBuffer(), null);

    // Position in stream coordinates.
    final Point streamPos = new Point(0, 0);

    // Allocate a row of memory.
    final byte[] rowBuf = new byte[this.width];

    try {
      // Read and decode the image data, fill in theTile.
      this.initCodeSize = this.input.readUnsignedByte();

      // Read first data block
      this.blockLength = this.input.readUnsignedByte();
      int left = this.blockLength;
      int off = 0;
      while (left > 0) {
        final int nbytes = this.input.read(this.block, off, left);
        left -= nbytes;
        off += nbytes;
      }

      this.bitPos = 0;
      this.nextByte = 0;
      this.lastBlockFound = false;
      this.bitsLeft = this.blockLength << 3;

      // Init 32-bit buffer
      initNext32Bits();

      this.clearCode = 1 << this.initCodeSize;
      this.eofCode = this.clearCode + 1;

      int code, oldCode = 0;

      final int[] prefix = new int[4096];
      final byte[] suffix = new byte[4096];
      final byte[] initial = new byte[4096];
      final int[] length = new int[4096];
      final byte[] string = new byte[4096];

      initializeStringTable(prefix, suffix, initial, length);
      int tableIndex = (1 << this.initCodeSize) + 2;
      int codeSize = this.initCodeSize + 1;
      int codeMask = (1 << codeSize) - 1;

      while (true) {
        code = getCode(codeSize, codeMask);

        if (code == this.clearCode) {
          initializeStringTable(prefix, suffix, initial, length);
          tableIndex = (1 << this.initCodeSize) + 2;
          codeSize = this.initCodeSize + 1;
          codeMask = (1 << codeSize) - 1;
          code = getCode(codeSize, codeMask);
          if (code == this.eofCode) {
            return this.theTile;
          }
        } else if (code == this.eofCode) {
          return this.theTile;
        } else {
          int newSuffixIndex;
          if (code < tableIndex) {
            newSuffixIndex = code;
          } else { // code == tableIndex
            newSuffixIndex = oldCode;
          }

          final int ti = tableIndex;
          final int oc = oldCode;

          prefix[ti] = oc;
          suffix[ti] = initial[newSuffixIndex];
          initial[ti] = initial[oc];
          length[ti] = length[oc] + 1;

          ++tableIndex;
          if ((tableIndex == (1 << codeSize)) && (tableIndex < 4096)) {
            ++codeSize;
            codeMask = (1 << codeSize) - 1;
          }
        }

        // Reverse code
        int c = code;
        final int len = length[c];
        for (int i = len - 1; i >= 0; i--) {
          string[i] = suffix[c];
          c = prefix[c];
        }

        outputPixels(string, len, streamPos, rowBuf);
        oldCode = code;
      }
    } catch (final IOException e) {
      final String message = JaiI18N.getString("GIFImage3");
      ImagingListenerProxy.errorOccurred(message, new ImagingException(message, e), this, false);
      //            throw new RuntimeException(JaiI18N.getString("GIFImage3"));
    } finally {
      return this.theTile;
    }
  }

  public void dispose() {
    this.theTile = null;
  }
}
