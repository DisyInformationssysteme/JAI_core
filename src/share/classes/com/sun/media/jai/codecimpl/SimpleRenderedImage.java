/*
 * $RCSfile: SimpleRenderedImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005-02-11 04:55:38 $
 * $State: Exp $
 */
package com.sun.media.jai.codecimpl;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.media.jai.RasterFactory;

/**
 * A simple class implemented the <code>RenderedImage</code>
 * interface.  Only the <code>getTile()</code> method needs to be
 * implemented by subclasses.  The instance variables must also be
 * filled in properly.
 *
 * <p> Normally in JAI <code>PlanarImage</code> is used for this
 * purpose, but in the interest of making
 * <code>com.sun.media.jai.codec</code> and
 * <code>com.sun.media.jai.codecimpl</code> be as modular as possible the
 * use of <code>PlanarImage</code> has been avoided.
 */
public abstract class SimpleRenderedImage implements RenderedImage {

  /** The X coordinate of the image's upper-left pixel. */
  protected int minX;

  /** The Y coordinate of the image's upper-left pixel. */
  protected int minY;

  /** The image's width in pixels. */
  protected int width;

  /** The image's height in pixels. */
  protected int height;

  /** The width of a tile. */
  protected int tileWidth;

  /** The height of a tile. */
  protected int tileHeight;

  /** The X coordinate of the upper-left pixel of tile (0, 0). */
  protected int tileGridXOffset = 0;

  /** The Y coordinate of the upper-left pixel of tile (0, 0). */
  protected int tileGridYOffset = 0;

  /** The image's SampleModel. */
  protected SampleModel sampleModel = null;

  /** The image's ColorModel. */
  protected ColorModel colorModel = null;

  /** The image's sources, stored in a Vector. */
  protected Vector sources = new Vector();

  /** A Hashtable containing the image properties. */
  protected Hashtable properties = new Hashtable();

  public SimpleRenderedImage() {
  }

  /** Returns the X coordinate of the leftmost column of the image. */
  @Override
  public int getMinX() {
    return this.minX;
  }

  /**
   * Returns the X coordinate of the column immediatetely to the
   * right of the rightmost column of the image.  getMaxX() is
   * implemented in terms of getMinX() and getWidth() and so does
   * not need to be implemented by subclasses.
   */
  public final int getMaxX() {
    return getMinX() + getWidth();
  }

  /** Returns the X coordinate of the uppermost row of the image. */
  @Override
  public int getMinY() {
    return this.minY;
  }

  /**
   * Returns the Y coordinate of the row immediately below the
   * bottom row of the image.  getMaxY() is implemented in terms of
   * getMinY() and getHeight() and so does not need to be
   * implemented by subclasses.
   */
  public final int getMaxY() {
    return getMinY() + getHeight();
  }

  /** Returns the width of the image. */
  @Override
  public int getWidth() {
    return this.width;
  }

  /** Returns the height of the image. */
  @Override
  public int getHeight() {
    return this.height;
  }

  /** Returns a Rectangle indicating the image bounds. */
  public Rectangle getBounds() {
    return new Rectangle(getMinX(), getMinY(), getWidth(), getHeight());
  }

  /** Returns the width of a tile. */
  @Override
  public int getTileWidth() {
    return this.tileWidth;
  }

  /** Returns the height of a tile. */
  @Override
  public int getTileHeight() {
    return this.tileHeight;
  }

  /**
   * Returns the X coordinate of the upper-left pixel of tile (0, 0).
   */
  @Override
  public int getTileGridXOffset() {
    return this.tileGridXOffset;
  }

  /**
   * Returns the Y coordinate of the upper-left pixel of tile (0, 0).
   */
  @Override
  public int getTileGridYOffset() {
    return this.tileGridYOffset;
  }

  /**
   * Returns the horizontal index of the leftmost column of tiles.
   * getMinTileX() is implemented in terms of getMinX()
   * and so does not need to be implemented by subclasses.
   */
  @Override
  public int getMinTileX() {
    return XToTileX(getMinX());
  }

  /**
   * Returns the horizontal index of the rightmost column of tiles.
   * getMaxTileX() is implemented in terms of getMaxX()
   * and so does not need to be implemented by subclasses.
   */
  public int getMaxTileX() {
    return XToTileX(getMaxX() - 1);
  }

  /**
   * Returns the number of tiles along the tile grid in the
   * horizontal direction.  getNumXTiles() is implemented in terms
   * of getMinTileX() and getMaxTileX() and so does not need to be
   * implemented by subclasses.
   */
  @Override
  public int getNumXTiles() {
    return getMaxTileX() - getMinTileX() + 1;
  }

  /**
   * Returns the vertical index of the uppermost row of tiles.  getMinTileY()
   * is implemented in terms of getMinY() and so does not need to be
   * implemented by subclasses.
   */
  @Override
  public int getMinTileY() {
    return YToTileY(getMinY());
  }

  /**
   * Returns the vertical index of the bottom row of tiles.  getMaxTileY()
   * is implemented in terms of getMaxY() and so does not need to
   * be implemented by subclasses.
   */
  public int getMaxTileY() {
    return YToTileY(getMaxY() - 1);
  }

  /**
   * Returns the number of tiles along the tile grid in the vertical
   * direction.  getNumYTiles() is implemented in terms
   * of getMinTileY() and getMaxTileY() and so does not need to be
   * implemented by subclasses.
   */
  @Override
  public int getNumYTiles() {
    return getMaxTileY() - getMinTileY() + 1;
  }

  /** Returns the SampleModel of the image. */
  @Override
  public SampleModel getSampleModel() {
    return this.sampleModel;
  }

  /** Returns the ColorModel of the image. */
  @Override
  public ColorModel getColorModel() {
    return this.colorModel;
  }

  /**
   * Gets a property from the property set of this image.  If the
   * property name is not recognized,
   * <code>java.awt.Image.UndefinedProperty</code> will be returned.
   *
   * @param name the name of the property to get, as a
   * <code>String</code>.  @return a reference to the property
   * <code>Object</code>, or the value
   * <code>java.awt.Image.UndefinedProperty.</code>
   */
  @Override
  public Object getProperty(String name) {
    name = name.toLowerCase();
    final Object value = this.properties.get(name);
    return value != null ? value : java.awt.Image.UndefinedProperty;
  }

  /**
   * Returns a list of the properties recognized by this image.  If
   * no properties are available, <code>null</code> will be
   * returned.
   *
   * @return an array of <code>String</code>s representing valid
   *         property names.
   */
  @Override
  public String[] getPropertyNames() {
    String[] names = null;

    if (this.properties.size() > 0) {
      names = new String[this.properties.size()];
      int index = 0;

      final Enumeration e = this.properties.keys();
      while (e.hasMoreElements()) {
        final String name = (String) e.nextElement();
        names[index++] = name;
      }
    }

    return names;
  }

  /**
   * Returns an array of <code>String</code>s recognized as names by
   * this property source that begin with the supplied prefix.  If
   * no property names match, <code>null</code> will be returned.
   * The comparison is done in a case-independent manner.
   *
   * <p> The default implementation calls
   * <code>getPropertyNames()</code> and searches the list of names
   * for matches.
   *
   * @return an array of <code>String</code>s giving the valid
   * property names.
   */
  public String[] getPropertyNames(String prefix) {
    final String propertyNames[] = getPropertyNames();
    if (propertyNames == null) {
      return null;
    }

    prefix = prefix.toLowerCase();

    final Vector names = new Vector();
    for (int i = 0; i < propertyNames.length; i++) {
      if (propertyNames[i].startsWith(prefix)) {
        names.addElement(propertyNames[i]);
      }
    }

    if (names.size() == 0) {
      return null;
    }

    // Copy the strings from the Vector over to a String array.
    final String prefixNames[] = new String[names.size()];
    int count = 0;
    for (final Iterator it = names.iterator(); it.hasNext();) {
      prefixNames[count++] = (String) it.next();
    }

    return prefixNames;
  }

  // Utility methods.

  /**
   * Converts a pixel's X coordinate into a horizontal tile index
   * relative to a given tile grid layout specified by its X offset
   * and tile width.
   */
  public static int XToTileX(int x, final int tileGridXOffset, final int tileWidth) {
    x -= tileGridXOffset;
    if (x < 0) {
      x += 1 - tileWidth; // Force round to -infinity
    }
    return x / tileWidth;
  }

  /**
   * Converts a pixel's Y coordinate into a vertical tile index
   * relative to a given tile grid layout specified by its Y offset
   * and tile height.
   */
  public static int YToTileY(int y, final int tileGridYOffset, final int tileHeight) {
    y -= tileGridYOffset;
    if (y < 0) {
      y += 1 - tileHeight; // Force round to -infinity
    }
    return y / tileHeight;
  }

  /**
   * Converts a pixel's X coordinate into a horizontal tile index.
   * This is a convenience method.  No attempt is made to detect
   * out-of-range coordinates.
   *
   * @param x the X coordinate of a pixel.
   * @return the X index of the tile containing the pixel.
   */
  public int XToTileX(final int x) {
    return XToTileX(x, getTileGridXOffset(), getTileWidth());
  }

  /**
   * Converts a pixel's Y coordinate into a vertical tile index.
   * This is a convenience method.  No attempt is made to detect
   * out-of-range coordinates.
   *
   * @param y the Y coordinate of a pixel.
   * @return the Y index of the tile containing the pixel.
   */
  public int YToTileY(final int y) {
    return YToTileY(y, getTileGridYOffset(), getTileHeight());
  }

  /**
   * Converts a horizontal tile index into the X coordinate of its
   * upper left pixel relative to a given tile grid layout specified
   * by its X offset and tile width.
   */
  public static int tileXToX(final int tx, final int tileGridXOffset, final int tileWidth) {
    return tx * tileWidth + tileGridXOffset;
  }

  /**
   * Converts a vertical tile index into the Y coordinate of
   * its upper left pixel relative to a given tile grid layout
   * specified by its Y offset and tile height.
   */
  public static int tileYToY(final int ty, final int tileGridYOffset, final int tileHeight) {
    return ty * tileHeight + tileGridYOffset;
  }

  /**
   * Converts a horizontal tile index into the X coordinate of its
   * upper left pixel.  This is a convenience method.  No attempt is made
   * to detect out-of-range indices.
   *
   * @param tx the horizontal index of a tile.
   * @return the X coordinate of the tile's upper left pixel.
   */
  public int tileXToX(final int tx) {
    return tx * this.tileWidth + this.tileGridXOffset;
  }

  /**
   * Converts a vertical tile index into the Y coordinate of its
   * upper left pixel.  This is a convenience method.  No attempt is made
   * to detect out-of-range indices.
   *
   * @param ty the vertical index of a tile.
   * @return the Y coordinate of the tile's upper left pixel.
   */
  public int tileYToY(final int ty) {
    return ty * this.tileHeight + this.tileGridYOffset;
  }

  @Override
  public Vector getSources() {
    return null;
  }

  /**
   * Returns the entire image in a single Raster.  For images with
   * multiple tiles this will require making a copy.
   *
   * <p> The returned Raster is semantically a copy.  This means
   * that updates to the source image will not be reflected in the
   * returned Raster.  For non-writable (immutable) source images,
   * the returned value may be a reference to the image's internal
   * data.  The returned Raster should be considered non-writable;
   * any attempt to alter its pixel data (such as by casting it to
   * WritableRaster or obtaining and modifying its DataBuffer) may
   * result in undefined behavior.  The copyData method should be
   * used if the returned Raster is to be modified.
   *
   * @return a Raster containing a copy of this image's data.
   */
  @Override
  public Raster getData() {
    final Rectangle rect = new Rectangle(getMinX(), getMinY(), getWidth(), getHeight());
    return getData(rect);
  }

  /**
   * Returns an arbitrary rectangular region of the RenderedImage
   * in a Raster.  The rectangle of interest will be clipped against
   * the image bounds.
   *
   * <p> The returned Raster is semantically a copy.  This means
   * that updates to the source image will not be reflected in the
   * returned Raster.  For non-writable (immutable) source images,
   * the returned value may be a reference to the image's internal
   * data.  The returned Raster should be considered non-writable;
   * any attempt to alter its pixel data (such as by casting it to
   * WritableRaster or obtaining and modifying its DataBuffer) may
   * result in undefined behavior.  The copyData method should be
   * used if the returned Raster is to be modified.
   *
   * @param bounds the region of the RenderedImage to be returned.
   */
  @Override
  public Raster getData(Rectangle bounds) {
    // Get the image bounds.
    final Rectangle imageBounds = getBounds();

    // Check for parameter validity.
    if (bounds == null) {
      bounds = imageBounds;
    } else if (!bounds.intersects(imageBounds)) {
      throw new IllegalArgumentException(JaiI18N.getString("SimpleRenderedImage0"));
    }

    // Determine tile limits for the prescribed bounds.
    int startX = XToTileX(bounds.x);
    int startY = YToTileY(bounds.y);
    int endX = XToTileX(bounds.x + bounds.width - 1);
    int endY = YToTileY(bounds.y + bounds.height - 1);

    // If the bounds are contained in a single tile, return a child
    // of that tile's Raster.
    if ((startX == endX) && (startY == endY)) {
      final Raster tile = getTile(startX, startY);
      return tile.createChild(bounds.x, bounds.y, bounds.width, bounds.height, bounds.x, bounds.y, null);
    } else {
      // Recalculate the tile limits if the data bounds are not a
      // subset of the image bounds.
      if (!imageBounds.contains(bounds)) {
        final Rectangle xsect = bounds.intersection(imageBounds);
        startX = XToTileX(xsect.x);
        startY = YToTileY(xsect.y);
        endX = XToTileX(xsect.x + xsect.width - 1);
        endY = YToTileY(xsect.y + xsect.height - 1);
      }

      // Create a WritableRaster of the desired size
      final SampleModel sm = this.sampleModel.createCompatibleSampleModel(bounds.width, bounds.height);

      // Translate it
      final WritableRaster dest = RasterFactory.createWritableRaster(sm, bounds.getLocation());

      // Loop over the tiles in the intersection.
      for (int j = startY; j <= endY; j++) {
        for (int i = startX; i <= endX; i++) {
          // Retrieve the tile.
          final Raster tile = getTile(i, j);

          // Create a child of the tile for the intersection of
          // the tile bounds and the bounds of the requested area.
          final Rectangle tileRect = tile.getBounds();
          final Rectangle intersectRect = bounds.intersection(tile.getBounds());
          final Raster liveRaster = tile.createChild(
              intersectRect.x,
              intersectRect.y,
              intersectRect.width,
              intersectRect.height,
              intersectRect.x,
              intersectRect.y,
              null);

          // Copy the data from the child.
          dest.setRect(liveRaster);
        }
      }

      return dest;
    }
  }

  /**
   * Copies an arbitrary rectangular region of the RenderedImage
   * into a caller-supplied WritableRaster.  The region to be
   * computed is determined by clipping the bounds of the supplied
   * WritableRaster against the bounds of the image.  The supplied
   * WritableRaster must have a SampleModel that is compatible with
   * that of the image.
   *
   * <p> If the raster argument is null, the entire image will
   * be copied into a newly-created WritableRaster with a SampleModel
   * that is compatible with that of the image.
   *
   * @param dest a WritableRaster to hold the returned portion of
   *        the image.
   * @return a reference to the supplied WritableRaster, or to a
   *         new WritableRaster if the supplied one was null.
   */
  @Override
  public WritableRaster copyData(WritableRaster dest) {
    // Get the image bounds.
    final Rectangle imageBounds = getBounds();

    Rectangle bounds;
    if (dest == null) {
      // Create a WritableRaster for the entire image.
      bounds = imageBounds;
      final Point p = new Point(this.minX, this.minY);
      final SampleModel sm = this.sampleModel.createCompatibleSampleModel(this.width, this.height);
      dest = RasterFactory.createWritableRaster(sm, p);
    } else {
      bounds = dest.getBounds();
    }

    // Determine tile limits for the intersection of the prescribed
    // bounds with the image bounds.
    final Rectangle xsect = imageBounds.contains(bounds) ? bounds : bounds.intersection(imageBounds);
    final int startX = XToTileX(xsect.x);
    final int startY = YToTileY(xsect.y);
    final int endX = XToTileX(xsect.x + xsect.width - 1);
    final int endY = YToTileY(xsect.y + xsect.height - 1);

    // Loop over the tiles in the intersection.
    for (int j = startY; j <= endY; j++) {
      for (int i = startX; i <= endX; i++) {
        // Retrieve the tile.
        final Raster tile = getTile(i, j);

        // Create a child of the tile for the intersection of
        // the tile bounds and the bounds of the requested area.
        final Rectangle tileRect = tile.getBounds();
        final Rectangle intersectRect = bounds.intersection(tile.getBounds());
        final Raster liveRaster = tile.createChild(
            intersectRect.x,
            intersectRect.y,
            intersectRect.width,
            intersectRect.height,
            intersectRect.x,
            intersectRect.y,
            null);

        // Copy the data from the child.
        dest.setRect(liveRaster);
      }
    }

    return dest;
  }
}
