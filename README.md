# GPX Edit Fork of Mobile Atlas Creator

This is a personal fork of [Mobile Atlas Creator (MOBAC)](https://mobac.sourceforge.io/), focused on adding simple and effective GPX/waypoint editing features. I created this fork because I couldn’t find a GPX/waypoint editor that fit my needs, so I (with help from GitHub Copilot) extended MOBAC to support my workflow.

**Note:** This fork is not intended to be merged back into the main MOBAC repository. The code is functional but not up to the standards of the main project, and I do not plan to maintain it actively. However, I may update it as needed.

## Features Added in This Fork

- GPX (waypoint) viewer and editor functionality
- Multi-selection and multi-delete support in the GPX file tree
- Copy/Move waypoints via context menu
- Enhanced context menu appearance and usability
- Reordering of GPX files in the tree
- Session persistence: loaded files, file order, expanded/collapsed state, and visibility
- Warnings for missing files and unsaved changes on exit
- “Show on map” improvements for edge cases and immediate waypoint edits
- Live map updates for waypoint position and name changes
- Improved application exit logic and save prompts
- Compilation and robustness fixes

Download for Windows:
https://github.com/robertmap/mobac-gpxedit/releases/download/2.3.3-gpxedit-beta0.1/Mobile.Atlas.Creator.2.3.3-gpxedit-beta0.1.zip
 
 # Mobile Atlas Creator - README

Welcome to Mobile Atlas Creator README file for end users.

Before January 2010 this project was known as "TrekBuddy Atlas Creator".

Mobile Atlas Creator is an open source project hosted on SourceForge.net:  
[https://mobac.sourceforge.io/](https://mobac.sourceforge.io/)

## Table of contents

*   [License](#License)
*   [Description](#Description)
*   [Requirements](#Requirements)
    *   [Java Advanced Imaging" (JAI)](#JAI)
*   [Installation](#Installation)
*   [Application start](#Start)
*   [The different atlas formats](#AtlasFormats)
    *   [AFTrack (OSZ)](#AFTrackOSZ)
    *   [AlpineQuestMap (AQM)](#AlpineQuestMap)
    *   [AndNav](#AndNav)
    *   [Big Planet Tracks](#BigPlanet)
    *   [CacheWolf](#CacheWolf)
    *   [Garmin Custom maps](#GarminCustom)
    *   [Glopus](#Glopus)
    *   [Glopus Map File / AFTrack (GMF)](#GMF)
    *   [Google Earth Overlay (KMZ)](#KMZ)
    *   [Magellan RMP](#Magellan)
    *   [MAPLORER](#MAPLORER)
    *   [Maverick](#Maverick)
    *   [MGMaps](#MGMaps)
    *   [Mobile Trail Explorer (MTE)](#MTE)
    *   [NaviComputer](#NaviComputer)
    *   [nfComPass](#nfComPass)
    *   [OruxMaps](#OruxMaps)
    *   [OSMAND](#OSMAND)
    *   [Osmdroid](#Osmdroid)
    *   [OSMTracker](#OSMTracker)
    *   [OziExplorer / large PNG image](#Ozi)
    *   [PathAway](#PathAway)
    *   [RMaps](#RMaps)
    *   [\[Nokia\] Sports Tracker](#SportsTracker)
    *   [TomTom Raster](#TomTomRaster)
    *   [Touratech QV](#TTQV)
    *   [TrekBuddy](#TrekBuddy)
    *   [TwoNav (RMAP)](#TwoNav)
    *   [Viewranger](#Viewranger)
*   [Enhancement requests](#Enhancement)
*   [Problems, Bugs &amp; Errors](#Bugs)
*   [Known problems and limitations](#Limitations)
*   [Details for advanced users](#AdvancedUsers)
    *   [Specifying directory configuration](#DirectoryConfig)
    *   [Moving the tile store directory](#TileStoreDir)
    *   [Starting external tools from within MOBAC](#ExternalTools)
*   [Custom map sources](#CustomMapSource)
    *   [Simple custom map sources](#SimpleCustomMapSource)
    *   [Custom WMS map sources](#CustomWMSMapSource)
    *   [Custom multi-layer map sources](#CustomMultiLayerMapSource)
    *   [Custom CloudMade map sources](#CloudMadeMapSource)
    *   [Custom BeanShell map sources](#BeanShellMapSource)
    *   [Custom MapPack](#CustomMapPack)
    *   [MOBAC integrated rendering of tiles based on OpenStreetmap (mapsforge) vector data](#Mapsforge)
    *   [Custom atlas as map source / locally generated tiles](#FileBased)

- - -

## License

Mobile Atlas Creator is under GNU General Public License Version 2 (GPL). For details on the GPL see the license file [gpl.txt](gpl.txt).  
The source code is available on the [SourceForge.net](https://sourceforge.net/projects/mobac/files/) for download and in the [project's source code GIT repository](https://sourceforge.net/p/mobac/git/ci/main/tree).

## Description

Mobile Atlas Creator creates atlases for several applications. For example [TrekBuddy](http://www.trekbuddy.net) and [Mobile Trail Explorer (MTE)](https://code.google.com/p/mobile-trail-explorer/), the Android applications [AndNav](http://www.andnav.org/), [Maverick](http://www.codesector.com/maverick.php), [Big Planet Tracks](https://code.google.com/p/big-planet-tracks/), [RMaps](http://robertdeveloper.blogspot.com/search/label/rmaps.release), [OruxMaps](https://www.oruxmaps.com), the Pocket PC application [Glopus](http://www.glopus.de/) and the commercial Windows application [OziExplorer](https://www.oziexplorer4.com/). The map image created for OziExplorer can also be used with any PNG capable image viewer (map image in one large PNG file).

## Requirements

This application requires that a Java SE Runtime Environment 11 or higher is installed on the computer.

If you have the choice between different Java Runtimes you should prefer the Java Runtime provided by Sun/Oracle. Especially the OpenJDK has some bugs regarding MOBAC and tends to crash on certain situations.

## Installation

Copy or move the unzipped files to a folder where you would like to have Mobile Atlas Creator installed. On computers running Windows please make sure **not to install MOBAC into Program Files sub-directory!**

## Application start

### Windows

The application is started by executing the Mobile Atlas Creator.exe. During the first application start all necessary files and folders are automatically created by the application.

### Linux and OSX

You can start MOBAC by executing the start-up script start.sh. Before starting MOBAC the first time it may be necessary to set the executable bit for start.sh e.g. by executing the following command:

chmod u+x start.sh

During the first application start all necessary files and folders are automatically created by the application.

- - -

## The different atlas formats

### AFTrack OSZ

For creating atlases compatible with AFTrack (Symbian S60) you have to select **AFTrack (OSZ)** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).

OSZ is a ZIP-archive. Within this zip archive a (large) number of tiles with the calibrated folder structure (coordinates) are stored. OSZ only works if each tile has got a resolution of 256x256 Pixels. So be sure to uncheck **Recreate/adjust map tiles (CPU intensive)**.

The output format is one file for each layer in the corresponding folder.

#### Restrictions

Depending on the device free memory the file maybe not work. So more then 80000 tiles should not be used.

### AlpineQuestMap (AQM)

Sorry no further details are available for this atlas format.

### Creating and using atlases with AndNav

For creating atlases compatible with AndNav you have to select **AndNav atlas format** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).  
As atlases for AndNav do not support all features provided by Mobile Atlas Creator the following settings are ignored when creating atlases for AndNav:

*   Change height or width of the map tiles
*   Name of all layers and maps
*   Structure of the atlas (which maps belongs to which layer)

### Creating and using atlases with Big Planet Tracks

For creating offline atlases compatible with [Big Planet Tracks](https://code.google.com/p/big-planet-tracks/) (Android application) you have to select **Big Planet Tracks SQLite** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).  
The atlas format BigPlanet SQLite does not support all features provided by Mobile Atlas Creator the following settings can not be used or are ignored when creating atlases using this output format:

*   Recreate/adjust map tiles with custom tile size (height and
*   Name of all layers and maps
*   Structure of the atlas (which maps belongs to which layer)
*   Atlases created after another are merged into one Database named \[Atlasname\] atlas.sqlitedb which is located in the atlas output directory.

### Creating atlases for CacheWolf

For creating atlases compatible with CacheWolf you have to select **CacheWolf WFL** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).

#### Custom tile processing settings enabled

Maps that have custom tile processing options set are saved as tiled images with one WFL calibration file for each tile. All processing options such as image format and tile size can be used.

#### Custom tile processing settings disabled

In this mode automatic tiling is disabled and the same [restrictions](#OziRestriction) and the [warning](#OziWarning) as for the OziExplorer output format apply.

The output format of an "atlas" for CacheWolf is one subdirectory per layer and within this subdirectory one PNG image and one WFL file per defined map.

### Creating Garmin Custom maps

For creating atlases of Creating Garmin Custom maps you have to select **Garmin Custom Map (KMZ)** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).  
Details about the Garmin Custom Map format can be found in the [Garmin Forums](https://forums.garmin.com/forumdisplay.php?f=205)

Using this atlas output format the following features are ignored when creating atlases:

*   Recreate/adjust map tiles (custom tile size and image format)

The output format of an "atlas" for Garmin Custom maps is one KMZ file per layer containing all maps (max 100) as separate JPG image files. The JPEG compression rate can be specified for each map/image using image format selector in the custom tile processing section. The Garmin Custom Map format defines a maximum image size of 1024x1024 pixels. If a map is larger it will be automatically scaled down to fit into this size. You can prevent scaling when setting the max map size in the settings dialog to 1024.

### Creating atlases for Glopus

For creating atlases compatible with Glopus you have to select **Glopus (PNG & KAL)** or **Glopus Map File (GMF)** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).

#### Glopus (PNG & KAL)

Using this atlas output format the same [restrictions](#OziRestriction) and the [warning](#OziWarning) as for the OziExplorer output format apply.

The output format of an "atlas" for Glopus is one subdirectory per layer and within this subdirectory one PNG image and one KAL file per defined map.

#### Glopus Map File (GMF)

Within this file a (large) number of tiles with its calibrated coordinates are stored. Glopus works best if each tile has a resolution of 1024x1024 pixels. So check **Recreate/adjust map tiles (CPU intensive)**. Width and height should be set to **1024**.

The output format is one file for each layer in the corresponding folder.

### Glopus Map File (GMF) / AFTrack

For creating atlases compatible with AFTrack (Symbian S60) you have to select **Glopus Map File (GMF)** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).

Within this file a (large) number of tiles with its calibrated coordinates are stored. AFTrack works best if each tile has got a resolution of 256x256 Pixels. So uncheck **Recreate/adjust map tiles (CPU intensive)**.

The output format is one file for each layer in the corresponding folder.

#### Restrictions

AFTrack can handle a maximum of 4096 tiles - so be sure not to select more.

### Google Earth Overlay (KMZ)

For creating Google Earth Overlays you have to select **Google Earth Overlay (KMZ)** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).

Using this atlas output format the following features are ignored when creating atlases:

*   Recreate/adjust map tiles (custom tile size and image format)

#### Warning

The Google Earth Overlay atlas format uses JPG images. The image size depends on the selected maximum map size (see settings dialog). The theoretical maximum map size for this atlas format is 25000. However it is **strongly recommended not to set the maximum map size higher than 10000** (this will result in a image with uncompressed size about 286 MB).

### Creating Magellan RMP atlases

For creating atlases of Creating Garmin Custom Maps you have to select **Magellan (RMP)** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).

Using this atlas output format the following features are ignored when creating atlases:

*   Recreate/adjust map tiles (custom tile size and image format)

Additionally some other limitations may apply, based on which device and firmware version you are using. Mobile Atlas Creator does not chek those limitations - therefore it may work or not if you are using:

*   An atlas with more than 5 maps.
*   Maps with a zoom level higher than 15.

The output format of an "atlas" for Magellan is one RMP file in the atlas directory. You can directly load this file in Magellan VantagePoint or transfer it onto your device.

### Creating atlases for MAPLORER

For creating atlases compatible with [MAPLORER](https://maplorer.com) you have to select the **Maplorer atlas format** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**). You have to choose **JPG image format** under **Layer settings/tile format** (PNG is not supported by Maplorer).

There are no specific limits on tile sizes and/or numbers; good results for hiking/biking can be obtained using zoom level 15, custom tile size of 1024x1024 (check **Recreate/adjust map tiles (CPU intensive)** under **Layer settings**. For good performance, avoid using too big or too many tiles (the maximum number of columns in Maplorer is currently 26 ('A' to 'Z'), but it is preferable to use less).

After defining the maps and layers (select region and click **Add selection** under **Atlas Content)** for the atlas, click the button Create Atlas. Once atlas download and creation has completed, all necessary files belonging to the atlas can be found in the directory `atlases/[atlas name]_[current date and time]`.

To install the atlas on your device, simply connect it to your PC and copy the content of the respective atlas subdirectory (i.e. all .jpg and .pos files generated) to the Maplorer directory (the one which contains maplorer.exe) on your device. Starting Maplorer on the device will automatically read all tiles and create the index map. Detailed instructions on making maps for Maplorer can be found at [https://maplorer.com](https://maplorer.com)

### Creating and using atlases with Maverick

For creating atlases compatible with Maverick you have to select **Maverick atlas format** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).  
As atlases for Maverick do not support all features provided by Mobile Atlas Creator the following settings are ignored when creating atlases for AndNav:

*   Recreate/adjust map tiles (custom tile size and image format)
*   Name of all layers and maps
*   Structure of the atlas (which maps belongs to which layer)

For further information how to use offline atlases with Maverick please see [Maverick Online Help on Setting up offline maps](https://www.codesector.com/maverick).

### Creating and using atlases with MGMaps

For creating atlases compatible with MGMaps you have to select **MGMaps** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**). Select a name for the maps, this is important as you will use it to view the maps in MGMaps.

#### How to copy the maps to your device

1.  Create a folder named MGMapsCache in the root of you device or memorycard
2.  Copy the created folders into this MGMapsCache folder so the directory structure looks like:

```
MGMapsCache
MGMapsCache/cache.conf
MGMapsCache/macos_10
MGMapsCache/macos_11
MGMapsCache/macos_12
MGMapsCache/macos_10/59_53.mgm
MGMapsCache/macos_11/150_106.mgm
MGMapsCache/macos_11/150_107.mgm
MGMapsCache/macos_11/151_106.mgm
MGMapsCache/macos_11/151_107.mgm
MGMapsCache/macos_12/365_213.mgm
MGMapsCache/macos_12/365_214.mgm
MGMapsCache/macos_12/366_213.mgm
MGMapsCache/macos_12/366_214.mgm
```
	

#### How to set up MGMaps to use the maps

1.  Select Settings/Map/Stored Maps, Click on the two boxes labelled Stored Maps and Offline Mode. The first box enables the use of stored map mode. The second box prohibits the use of the mobile phones Internet connect to download live maps. You can leave this unchecked if you want. stored maps folder: now tell MGMaps where you have stored the map tile files. in my case SDCard/MGMapsCache.
2.  Select Settings/Map/Map types, select options/add custom map,in map type name enter the name of the map you created using MAC in my case macos, map type url you can leave blank, select ok/options/save MGMaps will then apply the settings and display your created map.

#### Warning

Avoid using names native to MGMaps such as google, each name used must be defined as a custom map so the directory structure below must have macos,macroad and macsat defined as custom map types.

```
MGMapsCache MGMapsCache/cache.conf MGMapsCache/macos\_10
MGMapsCache/macos\_11 MGMapsCache/macroad\_11 MGMapsCache/macsat\_11
MGMapsCache/macos\_12 MGMapsCache/macroad\_12 MGMapsCache/macsat\_12
```

### Creating and using atlases with Mobile Trail Explorer (MTE)

For creating atlases compatible with Mobile Trail Explorer you have to select **Mobile Trail Explorer Cache** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).  
If a map source uses an image format different to PNG the tiles will be automatically converted to the PNG format.  
The output is one MTEFileCache file per atlas which contains all maps. An existing cache file can not be updated. If you want to add maps to an existing MTEFileCache please use the atlas format **Mobile Trail Explorer** which creates a file structure identical to JTileDownloader and then process the maps using [MTE CacheCreator](https://code.google.com/p/mobile-trail-explorer/wiki/CacheCreator).  
As atlases for Mobile Trail Explorer does not support all features provided by Mobile Atlas Creator the following settings are ignored when creating atlases for Mobile Trail Explorer:

*   Recreate/adjust map tiles (custom tile size and image format)
*   Name of all layers and maps
*   Structure of the atlas (which maps belongs to which layer)

### Creating atlases for NaviComputer

For creating atlases, compatible with NaviComputer, you have to select **NaviComputer (NMAP)** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).  
After defining the maps and layers for the atlas to be created, start atlas download and creation via the button **Create Atlas**. Once download and creation has completed, the generated file with the extension nmap can be found in the MOBAC output directory. This \*.nmap-file is the required input file for NaviComputer.

### Creating atlases for nfComPass

For creating atlases, compatible with nfComPass, you have to click **Atlas -> New Atlas**, select **nfComPass** and give your Atlas a name.  
By default atlases with a tilesize of 64x64 are created (recommended). For different tile sizes check **Recreate/adjust map tiles (CPU intensive)** under Layer settings. Set Tileformat can not be changed - it is always png. Maybe you must tryout, what is the best tilesize for your device. Choose your layer and zoomlevels and click **Add selection**. Then click **Create atlas**. Once download and creation has completed, the generated folders can be found in the MOBAC output directory. You must copy the data from the nfComPass.dat to your nfComPass.dat and fill in the path to your mapdirectory. After that, copy the folder(s) to your device. If it is possible, you should not copy the files with Active Sync to your device.

### Creating OruxMaps atlases

For creating atlases compatible with [OruxMaps](https://www.oruxmaps.com) as format you have to select **OruxMaps** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).

Using this atlas output format the following features are ignored when creating atlases:

*   Layer settings, custom tile size. OruxMaps always uses 512x512 tile size. You can select the tile format (PNG or JPEG). If you do not select any, default value is JPEG - quality 90.

The output format of an "atlas" for OruxMaps is one or more map directories in the atlas directory. You have to copy those maps onto your device (default directory: /oruxmaps/mapfiles/).

### Creating atlases for OSMAND

For creating atlases compatible with OSMAND you have to select **OSMAND tile storage** or **OSMAND SQlite** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).  
As atlases for OSMAND do not support all features provided by Mobile Atlas Creator the following settings are ignored when creating atlases for OSMAND:

*   Change height or width of the map tiles
*   Name of all layers and maps
*   Structure of the atlas (which maps belongs to which layer)

### Creating atlases for Osmdroid

For creating atlases compatible with Osmdroid you have to select either **Osmdroid ZIP**, **Osmdroid SQLite** or **Osmdroid GEMF** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).  
The mentioned three formats are "single file" atlases. One of the other possible atlas formats might also be acceptable for Osmdroid. Also, there is other software that is able to deal with GEMF archives.  
As atlases for Osmdroid do not support all features provided by Mobile Atlas Creator the following settings are ignored when creating atlases for Osmdroid:

*   Change height or width of the map tiles
*   Name of all layers and maps
*   Structure of the atlas (which maps belongs to which layer)

### Creating atlases for OSMTracker

For creating atlases compatible with OSMTracker you have to select **OSMTracker tile storage** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).  
As atlases for OSMTracker do not support all features provided by Mobile Atlas Creator the following settings are ignored when creating atlases for OSMTracker:

*   Change height or width of the map tiles
*   Name of all layers and maps
*   Structure of the atlas (which maps belongs to which layer)

### Creating and using atlases with OziExplorer / large PNG image export

For creating atlases compatible with OziExplorer you have to select **OziExplorer (PNG & MAP)** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).

#### Restrictions

As OziExplorer does not support tiled maps some features are ignored when creating atlases in this format:

*   Image output format is fix: 24bit PNG
*   Recreate/adjust map tiles custom tile is ignored

The output format of an "atlas" for OziExplorer is one subdirectory per layer and within this subdirectory one PNG image and one MAP file per defined map. For opening a map in OziExplorer select the menu **File** → **Load from File** → **Load from MAP file**, browse to the layer directory of the created atlas and select the MAP file.

If you are only interested in the map image you can safely delete the created map file.

#### Warning

Mobile Atlas Creator uses a highly sophisticated and optimized algorithm for creating the PNG files for OziExplorer use. This algorithm allows to create very large maps images at low memory usage. OziExplorer and most image viewers do not use such sophisticated algorithms which can lead to the situation that you can create very large map images - but OziExplorer and other image viewers are not able to open the image.

### Creating atlases for PathAway

For creating atlases compatible with PathAway you have to select **PathAway tile cache** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).  
As atlases for PathAway do not support all features provided by Mobile Atlas Creator the following restrictions apply when creating atlases for PathAway:

*   The maximum zoom level is 16 - higher zoom levels are not possible with this atlas format
*   Tile width and width can not be changed (settings will be ignored)
*   Name of all layers and maps is ignored
*   Structure of the atlas is ignored (which maps belongs to which layer)

### Creating and using atlases with RMaps

For creating offline atlases compatible with [RMaps](https://robertdeveloper.blogspot.com/search/label/rmaps) (Android application) you have to select **RMaps SQLite** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).  
The atlas format RMaps does not support all features provided by Mobile Atlas Creator the following settings can not be used or are ignored when creating atlases using this output format:

*   Recreate/adjust map tiles with custom tile size (height and width have to be 256)
*   Name of all layers and maps
*   Structure of the atlas (which maps belongs to which layer)
*   Atlases created after another are merged into one Database named \[AtlasName\] atlas.sqlitedb which is located in the atlas output directory.

### \[Nokia\] Sports Tracker

For creating atlases compatible with [Sports Tracker(ST)](http://https://www.sports-tracker.com) you have to select **Sports Tracker** (NOT GPS Sportstracker) as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).

Do not select **Recreate/adjust map tiles**.

Maximum Zoomlevel of Sports Tracker is 17, default is 13.

Copy the created atlas (the whole folder) into the following folder of youre phone (depending of the Sports Tracker version):

*   Sports Tracker 1.76: `E:\system\data\Maps\Street\`
*   Sports Tracker 2.x: `E:\system\data\Maps\Street\`
*   Sports Tracker 3.x: `E:\system\data\STMaps\Street\`

For changing the zoom level inside Storts Tracker press 5 to zoom in and 0 to zoom out.

### Creating and using atlases with Touratech QV / large PNG image export

For creating atlases compatible with Touratech QV you have to select **Touratech QV** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).

#### Restrictions

As Touratech QV does not support tiled maps some features are ignored when creating atlases in this format:

*   Image output format is fix: 24bit PNG
*   Recreate/adjust map tiles custom tile is ignored

The output format of an "atlas" for Touratech QV is one subdirectory per layer and within this subdirectory one PNG image and one CAL file per defined map.

If you are only interested in the map image you can safely delete the created cal file.

#### Warning

Mobile Atlas Creator uses a highly sophisticated and optimized algorithm for creating the PNG files for Touratech QV use. This algorithm allows to create very large maps images at low memory usage. Touratech QV and most image viewers do not use such sophisticated algorithms which can lead to the situation that you can create very large map images - but Touratech QV and other image viewers are not able to open the image.

### Creating atlases using TomTom Raster format

TBD

### Creating and using atlases with TrekBuddy

For creating atlases compatible with TrekBuddy you have to select **TrekBuddy tared atlas** or **TrekBuddy untared atlas** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).

After defining the maps and layers for the atlas to be created start atlas download and creation via the button **Create Atlas**. Once atlas download and creation has completed all necessary files belonging to the atlas can be found in the directory `atlases/[atlas name]_[current date and time]`.

The atlas itself consists of the atlas startup file cr.tar (tar format) or cr.tba (regular/untared format) and the subdirectories containing the different maps of the atlas. For using the atlas with TrekBuddy copy the whole directory onto your J2ME device. Then use the **Load Atlas** function of TrekBuddy and open cr.tar / cr.tba (in the atlas root directory).

### Creating and using atlases with TwoNav & CompeGPS Land/Air

To create atlases compatible with TwoNav software you have to select **TwoNav (RMAP)** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**). Atlases created for TwoNav are stored in a format called rmap. It's a binary format that holds the same map data stored at different resolutions - in order to make zooming in and out on low performance mobile devices fast and efficient. However, this causes some restrictions which have to be taken into account:

*   Each map within a layer has to have the same geographical bounds.
*   There only can be one map in a layer with a given zoom level

If there are several layers in one atlas, each layer will be stored as a separate rmap file.

The easiest way to create a correct atlas content is: first select the appropriate grid zoom (combobox next to the zoom slider), then pick zoom levels from the selected grid zoom up to the zoom level of the desired most detailed level, and finally add a selection to the Atlas content by clicking on the "Add selection" button.

Rmap format also requires no gaps in a zoom level range from the maximum to the minimum selected zoom level. If there are missing zoom levels, they will be created internally by shrinking the existing downloaded tiles. If the missing zoom levels contain a lot of tiles, this operation could take a while.

Finally copy the layer.rmap file from the `atlases/[atlas name]_[current date and time]` folder to your TwoNav maps folder.

As TwoNav does not support all available features provided my MOBAC, some features can not be used when creating atlases in this format:

*   Custom tile height and width (is ignored for TwoNav RMP atlases)
*   Custom tile format: Only JPEG formats are allowed

### Creating and using atlases with Viewranger

For creating atlases compatible with [Viewranger(VR)](https://www.viewranger.com) you have to select **Viewranger** as format in atlas format selection dialog that appears when starting a new atlas (menu **Atlas** entry **New Atlas**).

Maximum Zoomlevel of Viewranger is 18, minimum is 3.

After creating your atlas the folder structure looks like:

```
Mobile Atlas Creatoratlases/VR_2012-03-28_154944/OSM/13/4308
Mobile Atlas Creator/atlases/VR\_2012-03-28\_154944/OSM/14/43
Mobile Atlas Creator/atlases/VR\_2012-03-28\_154944/google/13/4308
Mobile Atlas Creator/atlases/VR\_2012-03-28\_154944/google/14/43
```

Now we have to [Create a Folder Structure on the Device:](https://support.viewranger.com/)

If you youre VR Folder is on E: it is on Symbian located in E:/ViewRanger/MapCache/\_PAlbTN/

In this folder create following Subfolders, if not already present:

84,85,87,88,89,129

e.g. /ViewRanger/MapCache/\_PAlbTN/84

Copy the content of "OSM" into 84, "google" into 85 and so on.

Now it looks like: E:/ViewRanger/MapCache/\_PAlbTN/13/4308/2687 , 2687 is a tile

VR has different Online Mapsources which are assigned to the Numbers as shown:

*   OSM->84
*   Opencyclemap->85
*   OSM Direct->87
*   OSM Midnigth->88
*   OSM Fresh->89
*   Open Piste Map->129

Of course you can fill the Tilecache with different Maps.

I use the Open Piste Map eg. as "OSM Public Transport"

## Enhancement requests

If you are missing the map provider of your choice or have other enhancement ideas for Mobile Atlas Creator feel free to open an [Feature Request Ticket](https://sourceforge.net/p/mobac/feature-requests/) at SourceForge.

## Problems, Bugs & Errors

In case of unexpected errors while executing Mobile Atlas Creator you may get presented a exception dialog containing detailed information about the problem. In such a case please create a new [ticket in the Bug Tracker](https://sourceforge.net/p/mobac/bugs/) at SourceForge. Please add the detailed exception information and a detailed description of your last performed actions.

### The default error log

By default Mobile Atlas Creator records all errors of the current session into it's error log file Mobile Atlas Creator.log . This log file is located on Windows system in the directory `%APPDATA%\Mobile Atlas Creator\` and on Linux/Unix/OSX system in the directory `~/.mobac/`

### Activate the advanced logging system

If the recorded errors in the error log do not indicate a problem you can activate the overall message logging mechanism of Mobile Atlas Creator. The next start Mobile Atlas Creator will create a log file in the current directory (on Windows this is usually the directory where the JAR file is located on Linux usually the profile directory). Please note that the log file is erased on each program start. If you think you have found a bug please file it in the [bug tracker at SourceForge](https://sourceforge.net/projects/mobac/).

#### MOBAC 2.2.2 and newer

Download the file [logback.xml](https://mobac.sourceforge.io/logback.xml) and save it in the directory where the jar file of Mobile Atlas Creator is installed to.

#### MOBAC 2.2.1 and older

Download the file [log4j.xml](https://mobac.sourceforge.io/log4j.xml) and save it in the directory where the jar file of Mobile Atlas Creator is installed to.

## Known problems and limitations

### New map sources

Mobile Atlas Creator is limited to map sources that provide their maps if form of map tiles. Each of that map tiles has to be of size 256x256 pixels. Additionally the map source has to use the spherical Mercator projection and the number of tiles forming the world on each zoom level has to be one of the following values: 20, 21, 22 ... 221, 222.  
For more details see OpenStreetMap Wiki: [Slippy map tilenames](https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames), [Mercator](https://wiki.openstreetmap.org/wiki/Mercator), [Height and width of a map](https://wiki.openstreetmap.org/wiki/Height_and_width_of_a_map) and [Zoom levels](https://wiki.openstreetmap.org/wiki/Zoom_levels)

### Java Bugs

Due to bugs in Java you should not do the following:

*   Install Mobile Atlas Creator into a directory which path contains an "!" (exclamation mark) because of this [Oracle Java bug](https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4523159)
*   Do not use OpenJDK if you want to work with GPX tracks in Mobile Atlas Creator
*   On Windows without a system proxy set (see connection settings of IE) it is strongly recommended **not to use** the following proxy setting:_Use standard Java proxy settings_

## Details for advanced users

### Specifying directory configuration

The following steps are necessary if MOBAC is installed to to a directory that is not writable for regular users:

For changing the directory configuration pattern for all users of a MOBAC installation save [this file](https://sourceforge.net/p/mobac/git/ci/main/tree/directories.ini.template) into the same directory where Mobile\_Atlas\_Creator.jar has been installed into and change it's name to directories.ini. For more details read the comments in this configuration file.

### Moving the tile store directory

Usually the tile store directory where Mobile Atlas Creator saves all downloaded images in is automatically determined.  
In case you want to select a different directory perform the following steps:

1.  Make sure you have closed MOBAC
2.  Open settings.xml in an text editor
3.  Inside of the tag <settings> search for the tag <directories>. Inside this tag create a new tag named <tileStoreDirectory>
4.  Set the path name to the value of this tag (Windows users should replace backslashes with a slashes)
5.  (optional) Copy or move the content of the old tile store directory into the new tile store directory.

#### Example

```
<settings>
    <directories>
        <tileStoreDirectory>E:/tiles</tileStoreDirectory>
    </directories>
    ...
</settings>
```

This specified the windows directory `E:\tiles` as new tile store directory. The previously used tile store will not be used anymore.  
Deleting the <tileStoreDirectory> tag restores the old behavior (automatically tile store directory selection).

### Starting external tools from within MOBAC

External tools like scripts or other executable programs can be started from the **Tools** menu from within MOBAC. This menu is only visible if external programs have been configured. The advantage of starting a program from within MOBAC is that certain information about selected map source, selected region ... can be transmitted as parameters to the executed program.  
This allows you for example to create your own maps (render tiles) with external tools like [OSMFILTER](https://wiki.openstreetmap.org/wiki/Osmfilter), [OSMCONVERT](https://wiki.openstreetmap.org/wiki/Osmconvert), [MAPERITIVE](http://maperitive.net)...

For defining an external program, create an xml file for each external program to be called. The content of the xml file has to be like one of the following examples. The file-name of the xml is not relevant - it only has to end with .xml

#### test.xml

Demonstrates how to execute an Windows batch file.

```
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ExternalTool>
 <name>Name shown in tools menu</name>
 <command>cmd.exe /c start .\\tools\\mybatch.cmd</command>
 <parameters>MIN\_LON MIN\_LAT MAX\_LON MAX\_LAT MIN\_ZOOM MAX\_ZOOM MAPSOURCE\_NAME MAPSOURCE\_DISPLAYNAME NAME\_EDITBOX </parameters>
 <debug>true</debug>
</ExternalTool>
```

[Annotated sample file download](https://sourceforge.net/p/mobac/git/ci/main/tree/tools/test.xml)

#### test-exe.xml

Demonstrates how to execute an regular windows program.

```
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ExternalTool>
 <name>Name shown in tools menu</name>
 <command>C:\\sample-path\\program.exe</command>
 <parameters>MIN\_LON MIN\_LAT MAX\_LON MAX\_LAT MIN\_ZOOM MAX\_ZOOM MAPSOURCE\_NAME MAPSOURCE\_DISPLAYNAME NAME\_EDITBOX </parameters>
 <debug>true</debug>
</ExternalTool>
```

The most interesting section is the content of `<parameters>`. It contains a space separated list of parameters the specified command will be executed with. The following values can be used:

*   `MAX_LAT` - Maximum latitude (upper border) off the selected area
*   `MIN_LAT` - Minimum latitude (lower border) off the selected area
*   `MAX_LON` - Maximum longitude (right border) off the selected area
*   `MIN_LON` - Minimum longitude (left border) off the selected area
*   `MIN_ZOOM` - minimum zoom of selected zoom levels check-boxes
*   `MAX_ZOOM` - maximum zoom of selected zoom levels check-boxes
*   `MAPSOURCE_NAME` - Currently select map source internal name (used in atlas profile xml file)
*   `MAPSOURCE_DISPLAYNAME` - Currently select map source display name (as shown in the GUI)
*   `NAME_EDITBOX` - content of the edit box "Name" in the side panel "Atlas Content"

## Custom map sources

### Simple custom map sources

Custom map sources which uses a similar URL scheme as Google/OpenStreetMap can be added by saving for each custom map source the definition in form of an xml file in the mapsources directory.

The following section shows is an example how the xml file has to be formatted. It defines an additional map source named "Custom OSM Mapnik" which shows map tiles identical to the predefined map source "OpenStreetMap Mapnik".

**Note:**The name specified in there has to be unique among all available map sources within MOBAC. The list of all available map sources can be obtained via command **Show all map source names** in the menu **Debug**.

```
<?xml version="1.0" encoding="UTF-8"?>
<customMapSource>
	<name>Custom OSM Mapnik</name>
	<minZoom>0</minZoom>
	<maxZoom>18</maxZoom>
	<tileType>png</tileType>
	<tileUpdate>None</tileUpdate>
	<url>https://tile.openstreetmap.org/{$z}/{$x}/{$y}.png</url>
	<backgroundColor>#000000</backgroundColor>
</customMapSource>
```

The most important part of this definition is the url. It is a template containing specific placeholders which are encapsulated by curly brace:

*   `{$z}` for the zoom level - number range: [_minZoom_ .. _maxZoom_]
*   `{$x}` for the x tile coordinate - number range: [0..2_zoom level_ ]
*   `{$y}` for the y tile coordinate - number range: [0..2_zoom level_ ]

Note: If the url contains the ampersand character `&` you have to encode it as `&amp;`. Otherwise it not be valid XML and therefore can not bo loaded by MOBAC.

Example file for download: [Example custom map source.xml](https://sourceforge.net/p/mobac/git/ci/main/tree/mapsources/Example%20custom%20map%20source.xml)

### Custom WMS map sources

Similar a WMS map source can be defined. Currently only 1.1.1 and 1.3.0 as version are supported.

Tag coordinateunit is used to separate EPSG:4326, EPSG:4171 (unit in degree by default) from EPSG:3857, EPSG:900913 and EPSG:3785 (unit in meter)

EPSG:4326, EPSG:4171 (unit in degree by default)

```
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<customWmsMapSource>
    <name>TerraServer WMS</name>
    <minZoom>12</minZoom>
    <maxZoom>18</maxZoom>
    <tileType>JPG</tileType>
    <version>1.1.1</version>
    <layers>DRG</layers>
    <url>http://terraserver-usa.com/ogcmap6.ashx?</url>
    <coordinatesystem>EPSG:4326</coordinatesystem>
    <!-- optional, by default "degree" -->
    <coordinateunit>degree</coordinateunit>
    <aditionalparameters>&amp;EXCEPTIONS=BLANK&amp;Styles=</aditionalparameters>
    <backgroundColor>#000000</backgroundColor>
</customWmsMapSource>
```

Example file (with comments) for download: [Example custom WMS map source.xml](https://sourceforge.net/p/mobac/git/ci/main/tree/mapsources/Example%20custom%20WMS%20map%20source.xml)

EPSG:3857, EPSG:900913 and EPSG:3785 (unit in meter)

```
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<customWmsMapSource>
    <name>Modele, XML WMS 1.3.0-meters(France)</name>
    <minZoom>6</minZoom>
    <maxZoom>18</maxZoom>
    <tileType>PNG</tileType>
    <version>1.3.0</version>
    <layers>BassinDCE\_FXX,SsBassinDCEAdmin\_FXX</layers>
    <url>https://services.sandre.eaufrance.fr/geo/mdo?</url>
    <coordinatesystem>EPSG:3857</coordinatesystem>
    <!-- required -->
    <coordinateunit>meter</coordinateunit>
</customWmsMapSource>
```

Example file (with comments) for download: [Example custom WMS map source in meter.xml](https://sourceforge.net/p/mobac/git/ci/main/tree/mapsources/Example%20custom%20WMS%20map%20source%20in%20meter.xml)

### Custom multi-layer map sources

The same way as [custom map sources](#CustomMapSource) map sources which consist of two or more layers can be defined as well. Note that all except the background map source layer (first in the list) must have transparent parts - otherwise layers in the list before will not be visible.

```
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<customMultiLayerMapSource>
    <name>Custom OSM Mapnik with Hills (Ger)</name>
    <tileType>PNG</tileType>
    <backgroundColor>#000000</backgroundColor>
    <layers>
        <customMapSource>
            <name>Custom OSM Mapnik</name>
            <minZoom>0</minZoom>
            <maxZoom>18</maxZoom>
            <tileType>PNG</tileType>
            <tileUpdate>None</tileUpdate>
            <url>https://tile.openstreetmap.org/{$z}/{$x}/{$y}.png</url>
        </customMapSource>
        <customMapSource>
            <name>Custom transparent hillshade</name>
            <minZoom>0</minZoom>
            <maxZoom>18</maxZoom>
            <tileType>PNG</tileType>
            <tileUpdate>None</tileUpdate>
            <url>https://www.wanderreitkarte.de/hills/{$z}/{$x}/{$y}.png</url>
        </customMapSource>
    </layers>
</customMultiLayerMapSource>
```

Example file for download: [Example custom multi-layer map source.xml](https://sourceforge.net/p/mobac/git/ci/main/tree/mapsources/Multi-layer/Example%20custom%20multi-layer%20map%20source.xml)

### Custom BeanShell map sources

BeanShell map sources as the can be developed using [MapEvaluator](https://mobac.sourceforge.io/wiki/index.php/MapEvaluator) can be used by MOBAC. To do so place the saved BeanShell code file (file extension .bsh) in the mapsources directory. It will be loaded on next start-up of MOBAC.

It is recommended to add a line defining the map source name ([must be unique](#MapSourceNameUnique)).

```
name = "Your map source name here";
```

Example file for download: [Example beanshell map source.bsh](https://sourceforge.net/p/mobac/git/ci/main/tree/mapsources/Example%20beanshell%20map%20source.bsh)

### Custom Map Pack

Developing a custom map pack requires at least basic Java skills. Therefore the description on how to develop a custom map pack is only part of the source code release of MOBAC. You can get the source code of MOBAC by using the latest src release available in the [files section at Sourceforge](https://sourceforge.net/projects/mobac/files/Mobile%20Atlas%20Creator/) or you can get it directly from the [Subversion code repository](https://sourceforge.net/p/mobac/code/HEAD/tree/) using the latest trunk version.

### MOBAC integrated rendering of tiles based on OpenStreetmap (mapsforge) vector data

Since version 2.0 MOBAC includes the [mapsforge](https://github.com/mapsforge/mapsforge) rendering engine. This allows MOBAC to render bitmap tiles on-the-fly using Mapsforge vector data files. Those vector data files can be [downloaded](https://download.mapsforge.org/maps/) pre-generated for a large number of regions world-wide. Alternatively you can convert OpenStreetMap data to mapsforge format on your own. The map rendering of the vector data can be configured using [xml base render themes](https://github.com/mapsforge/mapsforge/blob/master/docs/Rendertheme.md).

For using mapsforge vector data within MOBAC the following steps are required:

1.  [Download](https://download.mapsforge.org/maps/) or create a mapsforge vector data file and save it on your compurter
2.  Create a custom XML map source file in the mapsources subdirectory similar to the example file.
    
```
<?xml version="1.0" encoding="UTF-8"?>
<mapsforge>
    <!-- name of the map - as shown in map source list -->
    <name>Custom Mapsforge</name>
    
    <!-- optional -->
    <minZoom>0</minZoom> 
    
    <!-- optional -->
    <maxZoom>20</maxZoom> 
        
    <!-- absolute or relative file name -->
    <mapFile>mapsforge-test.map</mapFile>
    
    <!-- optional default OSMARENDERER Theme -->
    <!-- <xmlRenderTheme>mytheme.xml</xmlRenderTheme> -->
    
    <transparent>false</transparent>
    
    <!-- text size scale factor -->
    <textScale>1.0</textScale>
    
</mapsforge>
```
    
3.  Start or Restart MOBAC

### Custom atlas as map source / locally generated tiles

Existing atlases or locally rendered tiles can be directly integrated into MOBAC as custom map source without having to set-up a local web-server.  
At the moment the formats used by OSMTracker, AndNav, Maverick and OSMAND are supported. For adding such an atlas as map source download the [Example custom tile files source.xml](https://sourceforge.net/p/mobac/git/ci/main/tree/mapsources/Example%20custom%20tile%20files%20source.xml) file, adapt the `<sourceFolder>` entry and place it in the mapsources directory.

Tiles can also be packed into one or more zip files and directly used by MOBAC. For details please see the [Example custom tile zip source.xml](https://sourceforge.net/p/mobac/git/ci/main/tree/mapsources/Example%20custom%20tile%20zip%20source.xml).  

SQLite based atlas formats (RMaps, MBTiles, BigPlanetTracks, NaviComputer or OSMAND) can also be used directly by MOBAC. For details please see the [Example custom tile SQLite source.xml](https://sourceforge.net/p/mobac/git/ci/main/tree/mapsources/Example%20custom%20tile%20SQLite%20source.xml)