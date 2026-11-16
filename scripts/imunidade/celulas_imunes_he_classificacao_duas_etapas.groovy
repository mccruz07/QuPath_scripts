setImageType('BRIGHTFIELD_H_E')
setColorDeconvolutionStains('{"Name" : "H&E default", "Stain 1" : "Hematoxylin", "Values 1" : "0.6511078257574492 0.7011930431234068 0.29049426072255424", "Stain 2" : "Eosin", "Values 2" : "0.21589893562087106 0.8011960501132093 0.5580972485873467", "Background" : " 255 255 255"}');
createAnnotationsFromPixelClassifier("tissue", 170.0, 0.0)
selectObjects(getAnnotationObjects().findAll {it.getPathClass() == getPathClass("Positive")})
runPlugin('qupath.imagej.detect.cells.WatershedCellDetection', '{"detectionImageBrightfield":"Hematoxylin OD","requestedPixelSizeMicrons":0.5,"backgroundRadiusMicrons":8.0,"backgroundByReconstruction":true,"medianRadiusMicrons":0.0,"sigmaMicrons":1.5,"minAreaMicrons":10.0,"maxAreaMicrons":400.0,"threshold":0.2,"maxBackground":2.0,"watershedPostProcess":true,"cellExpansionMicrons":2.0,"includeNuclei":true,"smoothBoundaries":true,"makeMeasurements":true}')
addShapeMeasurements("AREA", "LENGTH", "CIRCULARITY", "SOLIDITY", "MAX_DIAMETER", "MIN_DIAMETER", "NUCLEUS_CELL_RATIO")
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons":2.0,"region":"ROI","tileSizeMicrons":25.0,"colorOD":true,"colorStain1":true,"colorStain2":true,"colorStain3":true,"colorRed":false,"colorGreen":false,"colorBlue":false,"colorHue":false,"colorSaturation":false,"colorBrightness":false,"doMean":true,"doStdDev":false,"doMinMax":false,"doMedian":false,"doHaralick":false,"haralickDistance":1,"haralickBins":32}')
// Primeiro classificador
runObjectClassifier("redondas")

// Seleciona apenas as células com a classe "Immune cells"
def immuneCells = getDetectionObjects().findAll { it.getPathClass() == getPathClass("Immune cells") }
selectObjects(immuneCells)

// Roda o segundo classificador apenas nas células selecionadas
runObjectClassifier("roxas")

// Cria a hierarquia "Immune cells: <subclasse>" usando a string composta
def parentClassName = "Immune cells"
immuneCells.each { cell ->
    def subClass = cell.getPathClass()
    if (subClass != null && subClass.getName() != parentClassName) {
        cell.setPathClass(getPathClass("${parentClassName}: ${subClass.getName()}"))
    }
}

// Atualiza a visualização e a tabela na interface
fireHierarchyUpdate()