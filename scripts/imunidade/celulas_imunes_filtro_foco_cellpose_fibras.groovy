import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.roi.GeometryTools
import qupath.lib.common.GeneralTools

setImageType('BRIGHTFIELD_H_E')
setColorDeconvolutionStains('{"Name" : "H&E default", "Stain 1" : "Hematoxylin", "Values 1" : "0.6511078257574492 0.7011930431234068 0.29049426072255424", "Stain 2" : "Eosin", "Values 2" : "0.21589893562087106 0.8011960501132093 0.5580972485873467", "Background" : " 255 255 255"}');

// 1. Identificar o tecido geral
createAnnotationsFromPixelClassifier("tissue", 170.0, 0.0)
def tissueAnns = getAnnotationObjects().findAll { it.getPathClass() == getPathClass("Positive") }

if (tissueAnns.isEmpty()) {
    println "AVISO: Nenhum tecido encontrado. Interrompendo processamento."
    return
}

// =========================================================================
// 2. FILTRO DE DESFOQUE
// =========================================================================
selectObjects(tissueAnns)
runPlugin('qupath.lib.algorithms.TilerPlugin', '{"tileSizeMicrons": 150.0, "trimToROI": true, "makeAnnotations": false}')
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 1.0, "region": "ROI", "colorOD": true, "colorStain1": true, "doMean": true, "doStdDev": true}')

def tiles = getDetectionObjects()
if (tiles.isEmpty()) {
    println "AVISO: Nenhum tile foi gerado para análise de foco. Interrompendo."
    return
}

tiles.each { tile ->
    double stdDev = measurement(tile, "Hematoxylin OD: Std.Dev.")
    double meanOD = measurement(tile, "Hematoxylin OD: Mean")

    if (meanOD > 0.15 && stdDev < 0.05) {
        tile.setPathClass(getPathClass("Qualidade: Desfocado"))
    } else {
        tile.setPathClass(getPathClass("Qualidade: Focado"))
    }
}

def tilesDesfocados = getDetectionObjects().findAll { it.getPathClass() == getPathClass("Qualidade: Desfocado") }
double percentagemDesfocada = (tilesDesfocados.size() / (double) getDetectionObjects().size()) * 100.0

if (percentagemDesfocada > 40.0) {
    println "AVISO: Lâmina reprovada no controlo de qualidade (${percentagemDesfocada.round(1)}% desfocada). Análise interrompida."
    removeObjects(getDetectionObjects(), true)
    return
} else {
    println "Lâmina aprovada no foco: ${percentagemDesfocada.round(1)}% desfocada."
}

// Remove os tiles de teste para liberar a lâmina
removeObjects(getDetectionObjects(), true)
fireHierarchyUpdate()



// =========================================================================
// 4. DETEÇÃO E CLASSIFICAÇÃO NAS FIBRAS
// =========================================================================
selectObjects(getAnnotationObjects().findAll { it.getPathClass() == getPathClass("Positive") })
createAnnotationsFromPixelClassifier("inter_fibra", 250.0, 0.0)

selectObjects(getAnnotationObjects().findAll { it.getPathClass() == getPathClass("fibers") })
runPlugin('qupath.imagej.detect.cells.WatershedCellDetection', '{"detectionImageBrightfield":"Hematoxylin OD","requestedPixelSizeMicrons":0.5,"backgroundRadiusMicrons":8.0,"backgroundByReconstruction":true,"medianRadiusMicrons":0.0,"sigmaMicrons":1.5,"minAreaMicrons":10.0,"maxAreaMicrons":400.0,"threshold":0.2,"maxBackground":2.0,"watershedPostProcess":true,"cellExpansionMicrons":2.0,"includeNuclei":true,"smoothBoundaries":true,"makeMeasurements":true}')

selectCells()
addShapeMeasurements("AREA", "LENGTH", "CIRCULARITY", "SOLIDITY", "MAX_DIAMETER", "MIN_DIAMETER", "NUCLEUS_CELL_RATIO")

runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons":2.0,"region":"ROI","tileSizeMicrons":25.0,"colorOD":true,"colorStain1":true,"colorStain2":true,"colorStain3":true,"colorRed":false,"colorGreen":false,"colorBlue":false,"colorHue":false,"colorSaturation":false,"colorBrightness":false,"doMean":true,"doStdDev":false,"doMinMax":false,"doMedian":false,"doHaralick":false,"haralickDistance":1,"haralickBins":32}')

selectCells()
runObjectClassifier("redondas2")

fireHierarchyUpdate()
println "Processamento concluído com sucesso."



selectObjects(getAnnotationObjects().findAll { it.getPathClass() == getPathClass("Positive") })
// ======================================================
// ETAPA 1 — DETECÇÃO DE FIBRAS
// ======================================================
def fiberCellpose = Cellpose2D.builder('cpsam')
        .pixelSize(1)
        .channels("Red")
        .diameter()
        .useCellposeSAM()
        .createAnnotations()
        .classify("Fiber")
        .build()

def imageData = getCurrentImageData()
parentROIs.each { fiberCellpose.detectObjects(imageData, [it]) }

fireHierarchyUpdate()