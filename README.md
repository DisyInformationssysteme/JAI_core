# Customized JAI core

This repository contains a slightly customized/minimized version of the Java Advanced Imaging (JAI) library.

As part of the changes in Java 9, the `com.sun.image.codec.jpeg` package is no longer part of the JDK.

The following changes were made to remove references to said package and enable a Java 9 build:

* moved the project to maven
* the JPEG codec itself and TIF methods referencing JPEG have been removed
* added a maven profile that is used to build on Java 9 with the necessary arguments
