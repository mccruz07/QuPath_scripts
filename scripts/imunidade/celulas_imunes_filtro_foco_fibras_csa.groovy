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

removeObjects(getDetectionObjects(), true)
fireHierarchyUpdate()


// =========================================================================
// 4. DETEÇÃO E CLASSIFICAÇÃO NAS FIBRAS (Células / Infiltrado)
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

// =========================================================================
// 5. SEGMENTAÇÃO DE FIBRAS COM CELLPOSE (CPSAM) E CÁLCULO DE CSA TRANSVERSAL
// =========================================================================
// Define as regiões de interesse para o Cellpose rodar dentro das fibras
def parentROIs = getAnnotationObjects().findAll { it.getPathClass() == getPathClass("Positive") }

if (parentROIs.isEmpty()) {
    println "AVISO: Nenhuma anotação de 'fibers' encontrada para rodar o Cellpose."
    return
}

// Configuração do modelo Cellpose SAM
// Nota: diameter(0) ativa a estimativa automática do Cellpose; ajuste para o diâmetro típico em px se preferir fixar
def fiberCellpose = Cellpose2D.builder('cpsam')
        .pixelSize(1.0)
        .channels("Red")

        .useCellposeSAM()
        .createAnnotations()
        .classify("Fiber")
        .useGPU(true) 
        .build()

def imageData = getCurrentImageData()
parentROIs.each { roi ->
    fiberCellpose.detectObjects(imageData, [roi])
}

// Calcula medidas geométricas (Área, Perímetro, Circularidade) para as novas anotações de fibra
selectObjects(getAnnotationObjects().findAll { it.getPathClass() == getPathClass("Fiber") })
addShapeMeasurements("AREA", "LENGTH", "CIRCULARITY", "SOLIDITY", "MAX_DIAMETER", "MIN_DIAMETER")

// Filtra as fibras pelo corte transversal correto usando Circularidade
// Fibras em corte transversal são circulares/poligonais (circularidade >= 0.60 e área mínima fisiológica)
def allFibers = getAnnotationObjects().findAll { it.getPathClass() == getPathClass("Fiber") }
def minCircularity = 0.45  // Ajuste entre 0.55 e 0.70 conforme sua histologia
def minAreaUm2 = 80.0      // Descarta fragmentos ou restos menores que 80 µm²
def maxAreaUm2 = 3000.0    // Descarta agrupamentos ou artefatos maiores

// Obtém a calibração do servidor atual
def cal = getCurrentServer().getPixelCalibration()

int transversalCount = 0
double totalCSA = 0.0

allFibers.each { fiber ->
    double circ = measurement(fiber, "Circularity")
    // Obtém a área calibrada em µm² de forma segura
    double area = fiber.getROI().getScaledArea(cal)
    
    if (circ >= minCircularity && area >= minAreaUm2 && area <= maxAreaUm2) {
        fiber.setPathClass(getPathClass("Fiber: Transversal (CSA OK)"))
        transversalCount++
        totalCSA += area
    } else {
        fiber.setPathClass(getPathClass("Fiber: Obliqua/Descartada"))
    }
}

fireHierarchyUpdate()

if (transversalCount > 0) {
    double meanCSA = totalCSA / transversalCount
    println "=== RESULTADOS CSA DE FIBRAS ==="
    println "Total de fibras transversais válidas: ${transversalCount}"
    println "Área de Secção Transversa Média (CSA): ${meanCSA.round(2)} µm²"
} else {
    println "Nenhuma fibra cumpriu o critério de alinhamento transversal (Circularity >= ${minCircularity})."
}