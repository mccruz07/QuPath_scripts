clearAllObjects()
fireHierarchyUpdate()

setImageType('BRIGHTFIELD_H_E')
setColorDeconvolutionStains('{"Name" : "H&E default", "Stain 1" : "Hematoxylin", "Values 1" : "0.6511078257574492 0.7011930431234068 0.29049426072255424", "Stain 2" : "Eosin", "Values 2" : "0.21589893562087106 0.8011960501132093 0.5580972485873467", "Background" : " 255 255 255"}');
createAnnotationsFromPixelClassifier("coracao", 1000.0, 0.0)
selectObjects(getAnnotationObjects().findAll {it.getPathClass() == getPathClass("Positive")})
createAnnotationsFromPixelClassifier("inter_fibra", 250.0, 0.0)
selectObjects(getAnnotationObjects().findAll {it.getPathClass() == getPathClass("fibers")})
runPlugin('qupath.imagej.detect.cells.WatershedCellDetection', '{"detectionImageBrightfield":"Hematoxylin OD","requestedPixelSizeMicrons":0.5,"backgroundRadiusMicrons":8.0,"backgroundByReconstruction":true,"medianRadiusMicrons":0.0,"sigmaMicrons":1.5,"minAreaMicrons":10.0,"maxAreaMicrons":400.0,"threshold":0.2,"maxBackground":2.0,"watershedPostProcess":true,"cellExpansionMicrons":2.0,"includeNuclei":true,"smoothBoundaries":true,"makeMeasurements":true}')
selectCells();
addShapeMeasurements("AREA", "LENGTH", "CIRCULARITY", "SOLIDITY", "MAX_DIAMETER", "MIN_DIAMETER", "NUCLEUS_CELL_RATIO")
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons":2.0,"region":"ROI","tileSizeMicrons":25.0,"colorOD":true,"colorStain1":true,"colorStain2":true,"colorStain3":true,"colorRed":false,"colorGreen":false,"colorBlue":false,"colorHue":false,"colorSaturation":false,"colorBrightness":false,"doMean":true,"doStdDev":false,"doMinMax":false,"doMedian":false,"doHaralick":false,"haralickDistance":1,"haralickBins":32}')
selectCells();
runObjectClassifier("redondas2")

fireHierarchyUpdate()
