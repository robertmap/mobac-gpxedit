# Mobile Atlas Creator - Readme for Developers

Welcome to the developer documentation of Mobile Atlas Creator (MOBAC). First please read the [standard readme](README.html) for all users.

## Table of contents

*   [Code access](#CodeAccess)
*   [Building Mobile Atlas Creator](#BuildingMOBAC)
*   [MOBAC in IntelliJ](#Intellij)
*   [Guidelines for publishing custom builds/releases](#GuidelinesCustomReleases)
*   [Map sources and map packs](#MapPacks)
*   [Developing a custom map pack](#CustomMapPack)
    *   [Building the map pack](#MapPackBuilding)
*   [Source code overview - important packages](#ImportantPackages)
*   [Participation](#Participation)

## Code access

If you want do get your hands on the latest source code of Mobile Atlas Creator you can check out the code from the Subversion repository at SourceForge:

[https://svn.code.sf.net/p/mobac/code/trunk/MOBAC/](https://svn.code.sf.net/p/mobac/code/trunk/MOBAC/)

There you will find the latest sources of Mobile Atlas Creator in form of an Gradle project which can be imported for example into IntelliJ. All sources, tools and build files are included in this repository. Java libraries are automatically retrieved from Maven central repository.

## Building Mobile Atlas Creator

If you want to compile MOBAC, an installation of Java Development Tools (JDK) is required. Using OpenJDK version 8 or 11 is recommended. For building MOBAC use the provided gradlew command-line script:

    # Linux/MacOS:
    chmod u+x gradlew
    ./gradlew dist

    # Windows
    gradlew dist

## MOBAC in IntelliJ

The main class for starting MOBAC in IntelliJ is in module mobac-run class mobac.StartMOBACdev

## Guidelines for publishing custom builds/releases

If you modify Mobile Atlas Creator and you want to publish it yourself please consider the following guidelines:

1.  Chose a version/release name that makes it clear that it is not an official release: Change the version string to reflect that.  
    Example: 2.2 beta 3 XYZ edition  
    The version string is located in the file /build.gradle. Change the entry **version ...** to your custom release name.
2.  Do not forget that Mobile Atlas Creator is a GPL project - therefore publishing the source code together with the binary release should went without saying.
3.  If your modification is useful you may consider to present it to the Mobile Atlas Creator development team. [Patches](https://sourceforge.net/p/mobac/patches/) are always welcome. Useful modifications have a great chance to be integrated into the main branch.

## Map sources and map packs

Since version v1.9 map sources are no longer part of Mobile\_Atlas\_Creator.jar. All map sources implementation are located in jar files in the mapsources sub-directory. Those map sources implementation packages are called "map-packs". Map packs files always starts with the term mp- and they end with the term .jar.

For implementing new map sources in a development environment like Eclipse it is sometimes faster to load the map sources directly from class-path rather from the map-packs. You can enable it by setting devmode in settings.xml to true. Afterwards map sources will be loaded directly from the bin directory of Eclipse (if available).

## Developing a custom map pack

For creating your own custom map pack you have to create a new module (Java) inside /mappacks. for example /mappacks/mymappack

Place the map source code in directory /mappacks/mymappack/src/main/java/mobac/mapsources/mappacks/mymappack

Additionally you have to create a text file named mobac.program.interfaces.MapSource inside the directory src/main/resources/META-INF/services/. This file contains a list of class names (full class name including the package name, one per line) that should be loaded by MOBAC as map source.

Background: During the build process this file will be included into the map-pack jar as META-INF/services/mobac.program.interfaces.MapSource so that it can be found by the [ServiceLoader](http://download.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).

### Building the map pack

Map packs can be built separately using [Apache Ant](http://ant.apache.org/) and the build target **build\_mapsources**:

ant build\_mapsources

Apache Ant is already included in Eclipse, so that you only have to select the file build.xml, press the right mouse button and select **Run As** - **Ant Build...**. In the dialogs that opens deselect the build target **all** and select instead **build\_mapsources**.

Each sub-package of mobac.mapsources.mappacks will be compiled and packed to an map package. The created map packages are saved into the mapsources directory. Therefore our example map pack will be packed into the file mapsources/mp-mymappack.jar

## Source code overview - important packages

### [mobac.gui](src/main/java/mobac/gui)

This package contains the implementation of all dialogs/windows. In the sub-packages you can find the related implementations of used graphical components - e.g. [mobac.gui.mapview.PreviewMap](src/main/java/mobac/gui/mapview/PreviewMap.java) - the component that draws the movable map background used by Mobile Atlas Creator.

### [mobac.mapsources](src/main/java/mobac/mapsources)

Holds the infrastructure and the implementation of all map sources available within Mobile Atlas Creator. For implementing own map sources that uses an online map you should derive your map source from the abstract base class [mobac.mapsources.AbstractHttpMapSource](src/main/java/mobac/mapsources/AbstractHttpMapSource.java). Map sources should be compiled and packed to a map pack so that they can be automatically detected and loaded while MOBAC is starting-up. For details on that topic please read section [Developing a custom map pack](#CustomMapPack).

### [mobac.program.atlascreators](src/main/java/mobac/program/atlascreators)

Holds the implementations of all atlas creators (atlas output formats) provided by Mobile Atlas Creator. Each class in the package implements exactly one atlas output format. Of special interest is the abstract class [AtlasCreator](src/main/java/mobac/program/atlascreators/AtlasCreator.java) which is the super class every atlas creator is derived of. The list of available formats is maintained in the enumeration [mobac.program.model.AtlasOutputFormat](src/main/java/mobac/program/model/AtlasOutputFormat.java).

### [mobac.tools](src/main/java/mobac/tools)

This package contains additional stand-alone tools that are not shipped with the Mobile Atlas Creator binary release. For example the [mobac.tools.MapSourcesTester](src/main/java/mobac/tools/MapSourcesTester.java) downloads one tile from each map source for verifying that the map source is functional.

## Participation

If you are familiar with the programming language Java and you want to contribute or participate in the development process of Mobile Atlas Creator feel free to contact one of the other developers of [Mobile Atlas Creator](https://sourceforge.net/projects/mobac/).