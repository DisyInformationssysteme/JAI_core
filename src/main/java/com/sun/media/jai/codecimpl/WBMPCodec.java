/*
 * $RCSfile: WBMPCodec.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005-12-14 19:24:54 $
 * $State: Exp $
 */
package com.sun.media.jai.codecimpl;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import com.sun.media.jai.codec.ForwardSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageDecoderImpl;
import com.sun.media.jai.codec.ImageDecodeParam;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.ImageEncodeParam;
import com.sun.media.jai.codec.SeekableStream;

/**
 * A subclass of <code>ImageCodec</code> that handles the WBMP format.
 */
public final class WBMPCodec extends ImageCodec {

    public WBMPCodec() {}

    public String getFormatName() {
        return "wbmp";
    }

    public Class getEncodeParamClass() {
        return Object.class;
    }

    public Class getDecodeParamClass() {
        return Object.class;
    }

    public boolean canEncodeImage(RenderedImage im,
                                  ImageEncodeParam param) {
        SampleModel sampleModel = im.getSampleModel();

        int dataType = sampleModel.getTransferType();
        if (dataType == DataBuffer.TYPE_FLOAT  ||
            dataType == DataBuffer.TYPE_DOUBLE ||
            sampleModel.getNumBands() != 1     ||
            sampleModel.getSampleSize(0) != 1) {
            return false;
        }

        return true;
    }

    protected ImageEncoder createImageEncoder(OutputStream dst,
                                              ImageEncodeParam param) {
        return new WBMPImageEncoder(dst, null);
    }

    protected ImageDecoder createImageDecoder(InputStream src,
                                              ImageDecodeParam param) {
        // Add buffering for efficiency
        if (!(src instanceof BufferedInputStream)) {
            src = new BufferedInputStream(src);
        }
        return new WBMPImageDecoder(new ForwardSeekableStream(src), null);
    }

    protected ImageDecoder createImageDecoder(SeekableStream src,
                                              ImageDecodeParam param) {
        return new WBMPImageDecoder(src, null);
    }

    public int getNumHeaderBytes() {
         return 3;
    }

    public boolean isFormatRecognized(byte[] header) {
        // WBMP has no magic bytes at the beginning so simply check
        // the first three bytes for known constraints.
        return ((header[0] == (byte)0) &&  // TypeField == 0
                header[1] == 0 && // FixHeaderField == 0xxx00000; not support ext header
                ((header[2] & 0x8f) != 0 || (header[2] & 0x7f) != 0));  // First width byte
                //XXX: header[2] & 0x8f) != 0 for the bug in Sony Ericsson encoder.
    }
}

