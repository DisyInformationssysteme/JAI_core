/*
 * $RCSfile: JPEGImageDecoder.java,v $
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

import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.media.jai.util.ImagingException;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.media.jai.codec.ImageDecodeParam;
import com.sun.media.jai.codec.ImageDecoderImpl;
import com.sun.media.jai.codec.JPEGDecodeParam;

/**
 * @since EA2
 */
public class JPEGImageDecoder extends ImageDecoderImpl {

  public JPEGImageDecoder(final InputStream input, final ImageDecodeParam param) {
    super(input, param);
  }

  @Override
  public RenderedImage decodeAsRenderedImage(final int page) throws IOException {
    if (page != 0) {
      throw new IOException(JaiI18N.getString("JPEGImageDecoder0"));
    }
    try {
      return new JPEGImage(this.input, this.param);
    } catch (final Exception e) {
      throw CodecUtils.toIOException(e);
    }
  }
}

/**
 * FilterInputStream subclass which does not support mark/reset.
 * Used to work around a failure of com.sun.image.codec.jpeg.JPEGImageDecoder
 * in which decodeAsBufferedImage() blocks in reset() if a corrupted JPEG
 * image is encountered.
 */
class NoMarkStream extends FilterInputStream {
  NoMarkStream(final InputStream in) {
    super(in);
  }

  /**
   * Disallow mark/reset.
   */
  @Override
  public boolean markSupported() {
    return false;
  }

  /**
   * Disallow close() from closing the stream passed in.
   */
  @Override
  public final void close() throws IOException {
    // Deliberately do nothing.
  }
}

class JPEGImage extends SimpleRenderedImage {

  /**
   * Mutex for the entire class to circumvent thread unsafety of
   * com.sun.image.codec.jpeg.JPEGImageDecoder implementation.
   */
  private static final Object LOCK = new Object();

  private Raster theTile = null;

  /**
   * Construct a JPEGmage.
   *
   * @param stream The JPEG InputStream.
   * @param param The decoding parameters.
   */
  public JPEGImage(InputStream stream, final ImageDecodeParam param) {
    // If the supplied InputStream supports mark/reset wrap it so
    // it does not.
    if (stream.markSupported()) {
      stream = new NoMarkStream(stream);
    }

    // Lock the entire class to work around lack of thread safety
    // in com.sun.image.codec.jpeg.JPEGImageDecoder implementation.
    BufferedImage image = null;
    synchronized (LOCK) {
      final com.sun.image.codec.jpeg.JPEGImageDecoder decoder = com.sun.image.codec.jpeg.JPEGCodec
          .createJPEGDecoder(stream);
      try {
        // decodeAsBufferedImage performs default color conversions
        image = decoder.decodeAsBufferedImage();
      } catch (final ImageFormatException e) {
        final String message = JaiI18N.getString("JPEGImageDecoder1");
        sendExceptionToListener(message, e);
        //                throw new RuntimeException(JaiI18N.getString("JPEGImageDecoder1"));
      } catch (final IOException e) {
        final String message = JaiI18N.getString("JPEGImageDecoder1");
        sendExceptionToListener(message, e);
        //                throw new RuntimeException(JaiI18N.getString("JPEGImageDecoder2"));
      }
    }

    this.minX = 0;
    this.minY = 0;
    this.tileWidth = this.width = image.getWidth();
    this.tileHeight = this.height = image.getHeight();

    // Force image to have a ComponentSampleModel if it does not have one
    // and the ImageDecodeParam is either null or is a JPEGDecodeParam
    // with 'decodeToCSM' set to 'true'.
    if ((param == null || (param instanceof JPEGDecodeParam && ((JPEGDecodeParam) param).getDecodeToCSM()))
        && !(image.getSampleModel() instanceof ComponentSampleModel)) {

      int type = -1;
      final int numBands = image.getSampleModel().getNumBands();
      if (numBands == 1) {
        type = BufferedImage.TYPE_BYTE_GRAY;
      } else if (numBands == 3) {
        type = BufferedImage.TYPE_3BYTE_BGR;
      } else if (numBands == 4) {
        type = BufferedImage.TYPE_4BYTE_ABGR;
      } else {
        throw new RuntimeException(JaiI18N.getString("JPEGImageDecoder3"));
      }

      final BufferedImage bi = new BufferedImage(this.width, this.height, type);
      bi.getWritableTile(0, 0).setRect(image.getWritableTile(0, 0));
      bi.releaseWritableTile(0, 0);
      image = bi;
    }

    this.sampleModel = image.getSampleModel();
    this.colorModel = image.getColorModel();

    this.theTile = image.getWritableTile(0, 0);
  }

  @Override
  public synchronized Raster getTile(final int tileX, final int tileY) {
    if (tileX != 0 || tileY != 0) {
      throw new IllegalArgumentException(JaiI18N.getString("JPEGImageDecoder4"));
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
