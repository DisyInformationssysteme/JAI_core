/*
 * $RCSfile: PropertySet.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005-02-11 04:55:41 $
 * $State: Exp $
 */
package com.sun.media.jai.codecimpl.fpx;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;

import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codecimpl.ImagingListenerProxy;

class Property {

  private final int type;
  private final int offset;

  public Property(final int type, final int offset) {
    this.type = type;
    this.offset = offset;
  }

  public int getType() {
    return this.type;
  }

  public int getOffset() {
    return this.offset;
  }
}

class PropertySet {

  private static final int TYPE_VT_EMPTY = -1;
  private static final int TYPE_VT_NULL = -1;
  private static final int TYPE_VT_I2 = 2;
  private static final int TYPE_VT_I4 = 3;
  private static final int TYPE_VT_R4 = -1;
  private static final int TYPE_VT_R8 = -1;
  private static final int TYPE_VT_CY = -1;
  private static final int TYPE_VT_DATE = -1;
  private static final int TYPE_VT_BSTR = -1;
  private static final int TYPE_VT_ERROR = -1;
  private static final int TYPE_VT_BOOL = -1;
  private static final int TYPE_VT_VARIANT = -1;
  private static final int TYPE_VT_UI1 = -1;
  private static final int TYPE_VT_UI2 = -1;
  private static final int TYPE_VT_UI4 = 19;
  private static final int TYPE_VT_I8 = -1;
  private static final int TYPE_VT_UI8 = -1;
  private static final int TYPE_VT_LPSTR = 30;
  private static final int TYPE_VT_LPWSTR = 31;
  private static final int TYPE_VT_FILETIME = 64;
  private static final int TYPE_VT_BLOB = 65;
  private static final int TYPE_VT_STREAM = -1;
  private static final int TYPE_VT_STORAGE = -1;
  private static final int TYPE_VT_STREAMED_OBJECT = -1;
  private static final int TYPE_VT_STORED_OBJECT = -1;
  private static final int TYPE_VT_BLOB_OBJECT = -1;
  private static final int TYPE_VT_CF = 71;
  private static final int TYPE_VT_CLSID = 72;
  private static final int TYPE_VT_VECTOR = 4096;

  SeekableStream stream;
  Hashtable properties = new Hashtable();

  public PropertySet(final SeekableStream stream) throws IOException {
    this.stream = stream;

    stream.seek(44);
    final int sectionOffset = stream.readIntLE();

    stream.seek(sectionOffset);
    final int sectionSize = stream.readIntLE();
    final int sectionCount = stream.readIntLE();

    for (int i = 0; i < sectionCount; i++) {
      stream.seek(sectionOffset + 8 * i + 8);
      final int pid = stream.readIntLE();
      final int offset = stream.readIntLE();

      stream.seek(sectionOffset + offset);
      final int type = stream.readIntLE();

      final Property p = new Property(type, sectionOffset + offset + 4);
      this.properties.put(new Integer(pid), p);
    }
  }

  public boolean hasProperty(final int id) {
    final Property p = (Property) this.properties.get(new Integer(id));
    return (p != null);
  }

  public int getI4(final int id) {
    final Property p = (Property) this.properties.get(new Integer(id));
    try {
      final int offset = p.getOffset();
      this.stream.seek(offset);
      return this.stream.readIntLE();
    } catch (final IOException e) {
      ImagingListenerProxy.errorOccurred(JaiI18N.getString("PropertySet1"), e, this, false);
      //            e.printStackTrace();
    }

    return -1;
  }

  public int getUI1(final int id) {
    final Property p = (Property) this.properties.get(new Integer(id));
    try {
      final int offset = p.getOffset();
      this.stream.seek(offset);
      return this.stream.readUnsignedByte();
    } catch (final IOException e) {
      ImagingListenerProxy.errorOccurred(JaiI18N.getString("PropertySet1"), e, this, false);
      //            e.printStackTrace();
    }

    return -1;
  }

  public int getUI2(final int id) {
    final Property p = (Property) this.properties.get(new Integer(id));
    try {
      final int offset = p.getOffset();
      this.stream.seek(offset);
      return this.stream.readUnsignedShortLE();
    } catch (final IOException e) {
      ImagingListenerProxy.errorOccurred(JaiI18N.getString("PropertySet2"), e, this, false);
      //            e.printStackTrace();
    }

    return -1;
  }

  public long getUI4(final int id) {
    final Property p = (Property) this.properties.get(new Integer(id));
    try {
      final int offset = p.getOffset();
      this.stream.seek(offset);
      return this.stream.readUnsignedIntLE();
    } catch (final IOException e) {
      ImagingListenerProxy.errorOccurred(JaiI18N.getString("PropertySet4"), e, this, false);
      //            e.printStackTrace();
    }

    return -1;
  }

  public long getUI4(final int id, final long defaultValue) {
    final Property p = (Property) this.properties.get(new Integer(id));
    if (p == null) {
      return defaultValue;
    }

    try {
      final int offset = p.getOffset();
      this.stream.seek(offset);
      return this.stream.readUnsignedIntLE();
    } catch (final IOException e) {
      ImagingListenerProxy.errorOccurred(JaiI18N.getString("PropertySet4"), e, this, false);
      //            e.printStackTrace();
    }

    return -1;
  }

  public String getLPSTR(final int id) {
    final Property p = (Property) this.properties.get(new Integer(id));
    if (p == null) {
      return null;
    }

    try {
      final int offset = p.getOffset();

      this.stream.seek(offset);
      final int length = this.stream.readIntLE();
      final StringBuffer sb = new StringBuffer(length);
      for (int i = 0; i < length; i++) {
        sb.append((char) this.stream.read());
      }

      return sb.toString();
    } catch (final IOException e) {
      ImagingListenerProxy.errorOccurred(JaiI18N.getString("PropertySet5"), e, this, false);
      //            e.printStackTrace();
      return null;
    }
  }

  public String getLPWSTR(final int id) {
    final Property p = (Property) this.properties.get(new Integer(id));
    try {
      final int offset = p.getOffset();

      this.stream.seek(offset);
      final int length = this.stream.readIntLE();
      final StringBuffer sb = new StringBuffer(length);
      for (int i = 0; i < length; i++) {
        sb.append(this.stream.readCharLE());
      }

      return sb.toString();
    } catch (final IOException e) {
      ImagingListenerProxy.errorOccurred(JaiI18N.getString("PropertySet5"), e, this, false);
      //            e.printStackTrace();
      return null;
    }
  }

  public float getR4(final int id) {
    final Property p = (Property) this.properties.get(new Integer(id));
    try {
      final int offset = p.getOffset();
      this.stream.seek(offset);
      return this.stream.readFloatLE();
    } catch (final IOException e) {
      ImagingListenerProxy.errorOccurred(JaiI18N.getString("PropertySet6"), e, this, false);
      //            e.printStackTrace();
      return -1.0F;
    }
  }

  public Date getDate(final int id) {
    throw new RuntimeException(JaiI18N.getString("PropertySet0"));
  }

  public Date getFiletime(final int id) {
    throw new RuntimeException(JaiI18N.getString("PropertySet0"));
  }

  public byte[] getBlob(final int id) {
    final Property p = (Property) this.properties.get(new Integer(id));
    try {
      final int offset = p.getOffset();
      this.stream.seek(offset);
      final int length = this.stream.readIntLE();

      final byte[] buf = new byte[length];
      this.stream.seek(offset + 4);
      this.stream.readFully(buf);

      return buf;
    } catch (final IOException e) {
      ImagingListenerProxy.errorOccurred(JaiI18N.getString("PropertySet7"), e, this, false);
      //            e.printStackTrace();
      return null;
    }
  }

  public int[] getUI1Vector(final int id) {
    throw new RuntimeException(JaiI18N.getString("PropertySet0"));
  }

  public int[] getUI2Vector(final int id) {
    throw new RuntimeException(JaiI18N.getString("PropertySet0"));
  }

  public long[] getUI4Vector(final int id) {
    throw new RuntimeException(JaiI18N.getString("PropertySet0"));
  }

  public float[] getR4Vector(final int id) {
    throw new RuntimeException(JaiI18N.getString("PropertySet0"));
  }

  public String[] getLPWSTRVector(final int id) {
    throw new RuntimeException(JaiI18N.getString("PropertySet0"));
  }
}
