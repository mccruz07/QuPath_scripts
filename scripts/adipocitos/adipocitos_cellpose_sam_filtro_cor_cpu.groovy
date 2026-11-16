import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.common.GeneralTools
import qupath.lib.analysis.features.ObjectMeasurements

// ======================================================
// ETAPA 0 — LIMPEZA INICIAL
// ======================================================
clearAllObjects()

// ======================================================
// PARÂMETROS DE FILTRAGEM
// ======================================================
double minArea = 400.0          // Área mínima do adipócito (em µm² ou px)
double maxArea = 8000.0         // Área máxima do adipócito
double minCircularidade = 0.35   // Mínimo de circularidade (0.0 a 1.0)

double maxMeanSaturation = 0.20 // Saturação máxima permitida (0.0 = cinza/branco)
double minMeanBrightness = 0.60 // Brilho mínimo (1.0 = branco)

// 1. Encontrar o tecido via Pixel Classifier
createAnnotationsFromPixelClassifier("adipo", 1000.0, 0.0)

def parentROIs = getAnnotationObjects().findAll { 
    it.getPathClass() == getPathClass("Positive") 
}

if (parentROIs.isEmpty()) {
    println "Nenhuma anotação 'Positive' foi encontrada na imagem!"
    return
}

// 2. Configurar e rodar o Cellpose SAM
def cellposeSAM = Cellpose2D.builder("cpsam")
        .pixelSize(1.0)
        .channels("Red")
        .useCellposeSAM()
        .useGPU(false)
        .createAnnotations()
        .classify("Adipo")
        .build()

def imageData = getCurrentImageData()
parentROIs.each { cellposeSAM.detectObjects(imageData, [it]) }

// 3. Obter todos os adipócitos detectados inicialmente
def allAdipocytes = getAnnotationObjects().findAll { 
    it.getPathClass() == getPathClass("Adipo") 
}

int numAdipocytesTotal = allAdipocytes.size()

// 4. Medições de Forma e Intensidade RGB
if (!allAdipocytes.isEmpty()) {
    selectObjects(allAdipocytes)
    addShapeMeasurements()
    
    def server = imageData.getServer()
    def requestedMeasurements = [ObjectMeasurements.Measurements.MEAN]
    allAdipocytes.each { 
        ObjectMeasurements.addIntensityMeasurements(server, it, 1.0, requestedMeasurements, []) 
    }
}

// Helper para cálculo de cor
def getChannel = { list, channel ->
    def key = list.keySet().find { it.toLowerCase().contains(channel.toLowerCase()) && it.toLowerCase().contains("mean") }
    return key ? list.get(key) : Double.NaN
}

// 5. Filtrar por Área, Circularidade e Cor
def adipocytesToRemove = []
int rejectedArea = 0, rejectedCircularity = 0, rejectedColor = 0, rejectedMissingColor = 0

allAdipocytes.each { adipocyte ->
    def m = adipocyte.getMeasurementList()
    def area = m.get("Area µm^2") ?: m.get("Area px") ?: adipocyte.getROI().getArea()
    def circ = m.get("Circularity") ?: m.get("Shape: Circularity") ?: 0.0
    
    def r = getChannel(m, "Red")
    def g = getChannel(m, "Green")
    def b = getChannel(m, "Blue")
    
    double sat = Double.NaN
    double brightness = Double.NaN
    
    if (!Double.isNaN(r) && !Double.isNaN(g) && !Double.isNaN(b)) {
        double maxRGB = Math.max(r, Math.max(g, b))
        double minRGB = Math.min(r, Math.min(g, b))
        sat = maxRGB > 0 ? (maxRGB - minRGB) / maxRGB : 0.0
        brightness = (r + g + b) / 3.0
        if (brightness > 1.5) brightness /= 255.0
    }

    boolean validArea = area >= minArea && area <= maxArea
    boolean validCirc = circ >= minCircularidade
    boolean validColor = !Double.isNaN(sat) && !Double.isNaN(brightness) && sat <= maxMeanSaturation && brightness >= minMeanBrightness

    if (!validArea) {
        adipocytesToRemove << adipocyte
        rejectedArea++
    } else if (!validCirc) {
        adipocytesToRemove << adipocyte
        rejectedCircularity++
    } else if (Double.isNaN(sat) || Double.isNaN(brightness)) {
        adipocytesToRemove << adipocyte
        rejectedMissingColor++
    } else if (!validColor) {
        adipocytesToRemove << adipocyte
        rejectedColor++
    }
}

removeObjects(adipocytesToRemove, true)

// 6. Lista de adipócitos aprovados
def filteredAdipocytes = getAnnotationObjects().findAll { 
    it.getPathClass() == getPathClass("Adipo") 
}

int numAdipocytesFiltered = filteredAdipocytes.size()

// 7. Extrair áreas para estatísticas
def adipocyteAreas = filteredAdipocytes.collect { 
    it.getMeasurementList().get("Area µm^2") ?: it.getMeasurementList().get("Area px") ?: it.getROI().getArea() 
}

double areaTotalTecido = parentROIs.sum { it.getROI().getArea() } ?: 0.0
double areaTotalAdipocytes = adipocyteAreas.sum() ?: 0.0
double areaMediaAdipocytes = numAdipocytesFiltered > 0 ? (areaTotalAdipocytes / numAdipocytesFiltered) : 0.0
double minAreaAdipocytes = adipocyteAreas ? adipocyteAreas.min() : 0.0
double maxAreaAdipocytes = adipocyteAreas ? adipocyteAreas.max() : 0.0

// Cálculo do Desvio Padrão Amostral
double desvioPadraoArea = 0.0
if (numAdipocytesFiltered > 1) {
    double somaQuadrados = adipocyteAreas.sum { Math.pow(it - areaMediaAdipocytes, 2) }
    desvioPadraoArea = Math.sqrt(somaQuadrados / (numAdipocytesFiltered - 1))
}

// 8. Grava as métricas na anotação do tecido (Positive)
def mainTissueAnnotation = parentROIs.first()
mainTissueAnnotation.getMeasurementList().with {
    put("Area_Total_Tecido_um2", areaTotalTecido)
    put("Num_Adipocytes_Totais", numAdipocytesTotal)
    put("Num_Adipocytes_Filtrados", numAdipocytesFiltered)
    put("Adipocyte_Area_Total_um2", areaTotalAdipocytes)
    put("Adipocyte_Area_Media_um2", areaMediaAdipocytes)
    put("Adipocyte_Area_Min_um2", minAreaAdipocytes)
    put("Adipocyte_Area_Max_um2", maxAreaAdipocytes)
    put("Adipocyte_Area_SD_um2", desvioPadraoArea)
    put("Rejected_Area", rejectedArea)
    put("Rejected_Circularity", rejectedCircularity)
    put("Rejected_Color", rejectedColor)
}

fireHierarchyUpdate()

// ======================================================
// 9. EXPORTAÇÃO AUTOMÁTICA EM UM ÚNICO CSV CONSOLIDADO
// ======================================================
def imageName = GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName())
def outputDir = buildFilePath(PROJECT_BASE_DIR, "adipo_results")
mkdirs(outputDir)

def csvFile = new File(outputDir, "consolidado_adipocytes.csv")

if (!csvFile.exists()) {
    csvFile.text = "Imagem,Area_Total_Tecido_um2,Num_Adipocytes_Totais,Num_Adipocytes_Filtrados,Adipocyte_Area_Total_um2,Adipocyte_Area_Media_um2,Adipocyte_Area_Min_um2,Adipocyte_Area_Max_um2,Adipocyte_Area_SD_um2,Rejected_Area,Rejected_Circularity,Rejected_Color,Rejected_Missing_Color\n"
}

csvFile.append("${imageName},${areaTotalTecido},${numAdipocytesTotal},${numAdipocytesFiltered},${areaTotalAdipocytes},${areaMediaAdipocytes},${minAreaAdipocytes},${maxAreaAdipocytes},${desvioPadraoArea},${rejectedArea},${rejectedCircularity},${rejectedColor},${rejectedMissingColor}\n")

resetSelection()
fireHierarchyUpdate()
println "Processamento concluído e dados exportados para: ${csvFile.getAbsolutePath()}"