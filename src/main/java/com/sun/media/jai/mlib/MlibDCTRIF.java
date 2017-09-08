/*
 * $RCSfile: MlibDCTRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005-02-11 04:55:53 $
 * $State: Exp $
 */
package com.sun.media.jai.mlib;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import javax.media.jai.ImageLayout;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "DCT" operation in the
 * rendered image mode using MediaLib.
 *
 * @see javax.media.jai.operator.DCTDescriptor
 * @see com.sun.media.jai.opimage.DCTOpImage
 *
 * @since EA4
 *
 */
public class MlibDCTRIF implements RenderedImageFactory {

    /** Constructor. */
    public MlibDCTRIF() {}

    /**
     * Creates a new instance of <code>DCTOpImage</code> in
     * the rendered image mode.
     *
     * @param args  The source image.
     * @param hints  May contain rendering hints and destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints hints) {
        /* Get ImageLayout and TileCache from RenderingHints. */
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);


        if (!MediaLibAccessor.isMediaLibCompatible(new ParameterBlock())) {
            return null;
        }

        return new DCTOpImage(args.getRenderedSource(0),
                              hints, layout,
                              new FCTmediaLib(true, 2));
    }
}
