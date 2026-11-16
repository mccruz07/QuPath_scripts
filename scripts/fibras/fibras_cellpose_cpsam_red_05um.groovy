selectAnnotations();
def pathModel = 'cpsam'
def cellpose = Cellpose2D.builder( pathModel )
        .pixelSize( 0.5 )                      // Resolution for detection in um
        .channels( 'Red' )	               // Select detection channel(s)
//        .tempDirectory( new File( '/tmp' ) )         // Temporary directory to export images to. defaults to 'cellpose-temp' inside the QuPath Project
//        .preprocess( ImageOps.Filters.median( 1 ) )  // List of preprocessing ImageOps to run on the images before exporting them
//        .normalizePercentilesGlobal( 0.1, 99.8, 10 ) // Convenience global percentile normalization. arguments are percentileMin, percentileMax, dowsample.
//        .tileSize( 1024 )                  // If your GPU can take it, make larger tiles to process fewer of them. Useful for Omnipose
//        .cellposeChannels( 1,2 )           // Overwrites the logic of this plugin with these two values. These will be sent directly to --chan and --chan2
//        .cellprobThreshold( 0.0 )          // Threshold for the mask detection, defaults to 0.0
//        .flowThreshold( 0.4 )              // Threshold for the flows, defaults to 0.4
//        .diameter( 15 )                    // Median object diameter. Set to 0.0 for the `bact_omni` model or for automatic computation
//        .useOmnipose()                     // Use omnipose instead
//        .useCellposeSAM()                  // Use cellposeSAM (i.e. cellpose 4.x.x) env instead of previous versions of cellpose <= v3.x.x
//        .addParameter( "cluster" )         // Any parameter from cellpose or omnipose not available in the builder.
//        .addParameter( "save_flows" )      // Any parameter from cellpose or omnipose not available in the builder.
//        .addParameter( "anisotropy", "3" ) // Any parameter from cellpose or omnipose not available in the builder.
//        .cellExpansion( 5.0 )              // Approximate cells based upon nucleus expansion
//        .cellConstrainScale( 1.5 )         // Constrain cell expansion using nucleus size
//        .classify( "My Detections" )       // PathClass to give newly created objects
//        .measureShape()                    // Add shape measurements
//        .measureIntensity()                // Add cell measurements (in all compartments)
//        .createAnnotations()               // Make annotations instead of detections. This ignores cellExpansion
//        .simplify( 0 )                     // Simplification 1.6 by default, set to 0 to get the cellpose masks as precisely as possible
//        .useGPU(false)                     // Force using CPU. Default useGPU(true)
        .build()

// Run detection for the selected objects
def imageData = getCurrentImageData()
def pathObjects = getSelectedObjects() // To process only selected annotations, useful while testing
// def pathObjects = getAnnotationObjects() // To process all annotations. For working in batch mode
if (pathObjects.isEmpty()) {
    Dialogs.showErrorMessage( "Cellpose", "Please select a parent object!" )
    return
}

cellpose.detectObjects( imageData, pathObjects )

// You could do some post-processing here, e.g. to remove objects that are too small, but it is usually better to
// do this in a separate script so you can see the results before deleting anything.

println 'Cellpose detection script done'

import qupath.ext.biop.cellpose.Cellpose2D

selectDetections();
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons":2.0,"region":"ROI","tileSizeMicrons":25.0,"colorOD":false,"colorStain1":false,"colorStain2":false,"colorStain3":false,"colorRed":false,"colorGreen":false,"colorBlue":false,"colorHue":false,"colorSaturation":false,"colorBrightness":false,"doMean":false,"doStdDev":false,"doMinMax":false,"doMedian":false,"doHaralick":false,"haralickDistance":1,"haralickBins":32}')

selectDetections();
addShapeMeasurements("AREA", "LENGTH", "CIRCULARITY", "SOLIDITY", "MAX_DIAMETER", "MIN_DIAMETER", "NUCLEUS_CELL_RATIO")