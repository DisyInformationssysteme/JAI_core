/*
 * $RCSfile: MultiplyDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005-02-11 04:57:41 $
 * $State: Exp $
 */
package javax.media.jai.operator;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderableRegistryMode;
import javax.media.jai.registry.RenderedRegistryMode;


/**
 * An <code>OperationDescriptor</code> describing the "Multiply" operation.
 *
 * <p> The Multiply operation takes two rendered or renderable source
 * images,  and multiplies every pair of pixels, one from each source
 * image of the corresponding position and band. No additional
 * parameters are required.
 *
 * <p> The two source images may have different numbers of bands and
 * data types. By default, the destination image bounds are the
 * intersection of the two source image bounds.  If the sources don't
 * intersect, the destination will have a width and height of 0.
 *
 * <p> The default number of bands of the destination image is equal
 * to the smallest number of bands of the sources, and the data type
 * is the smallest data type with sufficient range to cover the range
 * of both source data types (not necessarily the range of their
 * sums).
 *
 * <p> As a special case, if one of the source images has N bands (N >
 * 1), the other source has 1 band, and an <code>ImageLayout</code>
 * hint is provided containing a destination <code>SampleModel</code>
 * with K bands (1 < K <= N), then the single band of the 1-banded
 * source is added to each of the first K bands of the N-band source.
 *
 * <p> If the result of the operation underflows/overflows the
 * minimum/maximum value supported by the destination data type, then
 * it will be clamped to the minimum/maximum value respectively.
 *
 * <p> The destination pixel values are defined by the pseudocode:
 * <pre>
 * dst[x][y][dstBand] = clamp(srcs[0][x][y][src0Band] *
 *                            srcs[1][x][y][src1Band]);
 * </pre>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Multiply</td></tr>
 * <tr><td>LocalName</td>   <td>Multiply</td></tr>
 * <tr><td>Vendor</td>      <td>com.sun.media.jai</td></tr>
 * <tr><td>Description</td> <td>Multiplies two images.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/MultiplyDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * </table></p>
 *
 * <p> No parameters are needed for this operation.
 *
 * @see javax.media.jai.OperationDescriptor
 */
public class MultiplyDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Multiply"},
        {"LocalName",   "Multiply"},
        {"Vendor",      "com.sun.media.jai"},
        {"Description", JaiI18N.getString("MultiplyDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/MultiplyDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")}
    };

    /** Constructor. */
    public MultiplyDescriptor() {
        super(resources, 2, null, null, null);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }


    /**
     * Multiplies two images.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     *
     * @param source0 <code>RenderedImage</code> source 0.
     * @param source1 <code>RenderedImage</code> source 1.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    RenderedImage source1,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Multiply",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);

        return JAI.create("Multiply", pb, hints);
    }

    /**
     * Multiplies two images.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderableOp
     *
     * @param source0 <code>RenderableImage</code> source 0.
     * @param source1 <code>RenderableImage</code> source 1.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                RenderableImage source1,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Multiply",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);

        return JAI.createRenderable("Multiply", pb, hints);
    }
}
