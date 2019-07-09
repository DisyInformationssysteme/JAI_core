/*
 * $RCSfile: SampleModelProxy.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005-02-11 04:56:54 $
 * $State: Exp $
 */
package com.sun.media.jai.rmi;

import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.media.jai.ComponentSampleModelJAI;
import javax.media.jai.RasterFactory;


/**
 * This class is a serializable proxy for a SampleModel from which the
 * SampleModel may be reconstituted.
 *
 *
 * @since EA3
 */
public class SampleModelProxy implements Serializable {
    /** Flag indicating a BandedSampleModel. */
    private static final int TYPE_BANDED = 1;

    /** Flag indicating a PixelInterleavedSampleModel. */
    private static final int TYPE_PIXEL_INTERLEAVED = 2;

    /** Flag indicating a SinglePixelPackedSampleModel. */
    private static final int TYPE_SINGLE_PIXEL_PACKED = 3;

    /** Flag indicating a MultiPixelPackedSampleModel. */
    private static final int TYPE_MULTI_PIXEL_PACKED = 4;

    /** Flag indicating a ComponentSampleModelJAI. */
    private static final int TYPE_COMPONENT_JAI = 5;

    /** Flag indicating a generic ComponentSampleModel. */
    private static final int TYPE_COMPONENT = 6;

    /** The SampleModel. */
    private transient SampleModel sampleModel;

    /*
      parameter	        banded	ileaved	packed1 packedN
      ---------	        ------	-------	------- -------
      dataType	        *       *       *       *
      width		*       *       *       *
      height		*       *       *       *
      numBands          *
      bankIndices       *
      bandOffsets       *       *
      pixelStride               *
      scanlineStride            *       *       *
      bitMasks                          *
      numberOfBits                              *
      dataBitOffset                             *
    */

    /**
      * Constructs a <code>SampleModelProxy</code> from a
      * <code>SampleModel</code>.
      *
      * @param source The <code>SampleModel</code> to be serialized.
      */
    public SampleModelProxy(SampleModel source) {
        sampleModel = source;
    }

    /**
      * Retrieves the associated <code>SampleModel</code>.
      * @return The (perhaps reconstructed) <code>SampleModel</code>.
      */
    public SampleModel getSampleModel() {
        return sampleModel;
    }

    /**
      * Serialize the <code>SampleModelProxy</code>.
      *
      * @param out The <code>ObjectOutputStream</code>.
      */
    private void writeObject(ObjectOutputStream out) throws IOException {
        if(sampleModel instanceof ComponentSampleModel) {
            ComponentSampleModel sm = (ComponentSampleModel)sampleModel;
            int sampleModelType = TYPE_COMPONENT;
            int transferType = sm.getTransferType();
            if(sampleModel instanceof PixelInterleavedSampleModel) {
                sampleModelType = TYPE_PIXEL_INTERLEAVED;
            } else if(sampleModel instanceof BandedSampleModel) {
                sampleModelType = TYPE_BANDED;
            } else if(sampleModel instanceof ComponentSampleModelJAI ||
                      transferType == DataBuffer.TYPE_FLOAT ||
                      transferType == DataBuffer.TYPE_DOUBLE) {
                sampleModelType = TYPE_COMPONENT_JAI;
            }
            out.writeInt(sampleModelType);
            out.writeInt(transferType);
            out.writeInt(sm.getWidth());
            out.writeInt(sm.getHeight());
            if(sampleModelType != TYPE_BANDED) {
                out.writeInt(sm.getPixelStride());
            }
            out.writeInt(sm.getScanlineStride());
            if(sampleModelType != TYPE_PIXEL_INTERLEAVED) {
                out.writeObject(sm.getBankIndices());
            }
            out.writeObject(sm.getBandOffsets());
        } else if(sampleModel instanceof
                  SinglePixelPackedSampleModel) {
            SinglePixelPackedSampleModel sm =
                (SinglePixelPackedSampleModel)sampleModel;
            out.writeInt(TYPE_SINGLE_PIXEL_PACKED);
            out.writeInt(sm.getTransferType());
            out.writeInt(sm.getWidth());
            out.writeInt(sm.getHeight());
            out.writeInt(sm.getScanlineStride());
            out.writeObject(sm.getBitMasks());
        } else if(sampleModel instanceof MultiPixelPackedSampleModel) {
            MultiPixelPackedSampleModel sm =
                (MultiPixelPackedSampleModel)sampleModel;
            out.writeInt(TYPE_MULTI_PIXEL_PACKED);
            out.writeInt(sm.getTransferType());
            out.writeInt(sm.getWidth());
            out.writeInt(sm.getHeight());
            out.writeInt(sm.getPixelBitStride());
            out.writeInt(sm.getScanlineStride());
            out.writeInt(sm.getDataBitOffset());
        } else {
            throw new RuntimeException(JaiI18N.getString("SampleModelProxy0"));
        }
    }

    /**
      * Deserialize the <code>SampleModelProxy</code>.
      *
      * @param out The <code>ObjectInputStream</code>.
      */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        int sampleModelType = (int)in.readInt();
        switch(sampleModelType) {
        case TYPE_PIXEL_INTERLEAVED:
            sampleModel =
                RasterFactory.createPixelInterleavedSampleModel(in.readInt(),
                                                                in.readInt(),
                                                                in.readInt(),
                                                                in.readInt(),
                                                                in.readInt(),
                                                                (int[])in.readObject());
            break;
        case TYPE_BANDED:
            sampleModel =
                RasterFactory.createBandedSampleModel(in.readInt(),
                                                      in.readInt(),
                                                      in.readInt(),
                                                      in.readInt(),
                                                      (int[])in.readObject(),
                                                      (int[])in.readObject());
            break;
        case TYPE_COMPONENT_JAI:
            sampleModel =
                new ComponentSampleModelJAI(in.readInt(),
                                            in.readInt(),
                                            in.readInt(),
                                            in.readInt(),
                                            in.readInt(),
                                            (int[])in.readObject(),
                                            (int[])in.readObject());
            break;
        case TYPE_COMPONENT:
            sampleModel =
                new ComponentSampleModel(in.readInt(),
                                         in.readInt(),
                                         in.readInt(),
                                         in.readInt(),
                                         in.readInt(),
                                         (int[])in.readObject(),
                                         (int[])in.readObject());
            break;
        case TYPE_SINGLE_PIXEL_PACKED:
            sampleModel = new SinglePixelPackedSampleModel(in.readInt(),
                                                           in.readInt(),
                                                           in.readInt(),
                                                           in.readInt(),
                                                           (int[])in.readObject());
            break;
        case TYPE_MULTI_PIXEL_PACKED:
            sampleModel = new MultiPixelPackedSampleModel(in.readInt(),
                                                          in.readInt(),
                                                          in.readInt(),
                                                          in.readInt(),
                                                          in.readInt(),
                                                          in.readInt());
            break;
        default:
            throw new RuntimeException(JaiI18N.getString("SampleModelProxy0"));
        }
    }
}
