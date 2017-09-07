// CHECKSTYLE OFF: IllegalCallsCheck
/*
 * $RCSfile: ImagingListenerProxy.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005-02-11 04:55:36 $
 * $State: Exp $
 */
package com.sun.media.jai.codecimpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.media.jai.util.ImagingException;

public class ImagingListenerProxy {
  public static synchronized boolean errorOccurred(
      final String message,
      final Throwable thrown,
      final Object where,
      final boolean isRetryable) throws RuntimeException {
    Method errorOccurred = null;
    Object listener = null;

    try {
      final Class jaiClass = Class.forName("javax.media.jai.JAI");
      if (jaiClass == null) {
        return defaultImpl(message, thrown, where, isRetryable);
      }

      final Method jaiInstance = jaiClass.getMethod("getDefaultInstance", null);
      final Method getListener = jaiClass.getMethod("getImagingListener", null);

      final Object jai = jaiInstance.invoke(null, null);
      if (jai == null) {
        return defaultImpl(message, thrown, where, isRetryable);
      }

      listener = getListener.invoke(jai, null);
      final Class listenerClass = listener.getClass();

      errorOccurred = listenerClass
          .getMethod("errorOccurred", new Class[]{ String.class, Throwable.class, Object.class, boolean.class });
    } catch (final Throwable e) {
      return defaultImpl(message, thrown, where, isRetryable);
    }

    try {
      final Boolean result = (Boolean) errorOccurred
          .invoke(listener, new Object[]{ message, thrown, where, new Boolean(isRetryable) });
      return result.booleanValue();
    } catch (final InvocationTargetException e) {
      final Throwable te = e.getTargetException();
      throw new ImagingException(te);
    } catch (final Throwable e) {
      return defaultImpl(message, thrown, where, isRetryable);
    }
  }

  private static synchronized boolean defaultImpl(
      final String message,
      final Throwable thrown,
      final Object where,
      final boolean isRetryable) throws RuntimeException {
    // Silent the RuntimeException occuring in any OperationRegistry
    // and rethrown all the other RuntimeExceptions.
    if (thrown instanceof RuntimeException) {
      throw (RuntimeException) thrown;
    }

    System.err.println("Error: " + message);
    System.err
        .println("Occurs in: " + ((where instanceof Class) ? ((Class) where).getName() : where.getClass().getName()));
    thrown.printStackTrace(System.err);
    return false;
  }
}
