import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.common.GeneralTools
import qupath.lib.analysis.features.ObjectMeasurements

// ======================================================
// LIMPAR OBJETOS DA IMAGEM ATUAL
// ======================================================

clearAllObjects()

// ======================================================
// PARÂMETROS DE FILTRAGEM DOS ADIPÓCITOS
// ======================================================

double minArea = 400.0
double maxArea = 8000.0

double minCircularidade = 0.35

// ------------------------------------------------------
// FILTRO DE COR
// ------------------------------------------------------

// Saturação máxima permitida.
// 0.0 = completamente branco/cinza
// valores maiores = mais colorido
double maxMeanSaturation = 0.20

// Brilho médio mínimo.
// 0.0 = preto
// 1.0 = branco
double minMeanBrightness = 0.60

// ======================================================
// PARÂMETROS DO PIXEL CLASSIFIER
// ======================================================

double classifierMinArea = 1000.0
double classifierMinHoleArea = 0.0

// ======================================================
// 1. NOME DA IMAGEM
// ======================================================

def imageName = GeneralTools.getNameWithoutExtension(
        getProjectEntry().getImageName()
)

println ""
println "======================================================"
println "PROCESSANDO IMAGEM"
println "======================================================"
println "Imagem: ${imageName}"

// ======================================================
// 2. ENCONTRAR O TECIDO ADIPOSO VIA PIXEL CLASSIFIER
// ======================================================

println ""
println "Criando anotação com Pixel Classifier 'adipo'..."

createAnnotationsFromPixelClassifier(
        "adipo",
        classifierMinArea,
        classifierMinHoleArea
)

// ======================================================
// 3. LOCALIZAR AS ANOTAÇÕES POSITIVE
// ======================================================

def positiveClass = getPathClass("Positive")

def parentROIs = getAnnotationObjects().findAll {
    it.getPathClass() == positiveClass
}

if (parentROIs.isEmpty()) {

    println ""
    println "Nenhuma anotação 'Positive' foi encontrada!"
    return
}

println "Anotações Positive encontradas: ${parentROIs.size()}"

// Selecionar as regiões Positive
selectObjects(parentROIs)

// ======================================================
// 4. REMOVER OBJETOS FILHOS ANTERIORES
// ======================================================

parentROIs.each { parent ->

    def children = parent.getChildObjects().toList()

    if (!children.isEmpty()) {
        removeObjects(children, true)
    }
}

// ======================================================
// 5. ÁREA TOTAL DO TECIDO
// ======================================================

double areaTotalTecido = parentROIs.sum {
    it.getROI().getArea()
} ?: 0.0

println String.format(
        "Área total do tecido Positive: %.2f µm²",
        areaTotalTecido
)

// ======================================================
// 6. CONFIGURAR CELLPOSE-SAM
// ======================================================

println ""
println "Configurando Cellpose-SAM..."

def cellposeSAM = Cellpose2D.builder("cpsam")
        .pixelSize(1.0)
        .channels("Red")
        .useCellposeSAM()
        .useGPU(true)
        .createAnnotations()
        .classify("Adipo")
        .build()

// ======================================================
// 7. EXECUTAR CELLPOSE-SAM
// ======================================================

def imageData = getCurrentImageData()

println ""
println "Executando Cellpose-SAM..."

parentROIs.each { parent ->

    cellposeSAM.detectObjects(
            imageData,
            [parent]
    )
}

// ======================================================
// 8. OBTER TODOS OS ADIPÓCITOS
// ======================================================

def adipoClass = getPathClass("Adipo")

def allAdipocytes = getAnnotationObjects().findAll {
    it.getPathClass() == adipoClass
}

int numAdipocytesTotal = allAdipocytes.size()

println ""
println "Adipócitos detectados pelo Cellpose: ${numAdipocytesTotal}"

// ======================================================
// 9. MEDIDAS DE FORMA
// ======================================================

if (!allAdipocytes.isEmpty()) {

    selectObjects(allAdipocytes)

    addShapeMeasurements()
}

// ======================================================
// 10. MEDIDAS DE INTENSIDADE / COR
// ======================================================

// Vamos medir a média dos canais RGB dentro de cada objeto.
// O QuPath disponibiliza medições de intensidade por canal
// através de ObjectMeasurements.
def server = imageData.getServer()

def requestedIntensityMeasurements = [
        ObjectMeasurements.Measurements.MEAN
]

def compartments = []

println ""
println "Calculando intensidade média RGB..."

allAdipocytes.each { adipocyte ->

    ObjectMeasurements.addIntensityMeasurements(
            server,
            adipocyte,
            1.0,
            requestedIntensityMeasurements,
            compartments
    )
}

// ======================================================
// 11. FUNÇÃO PARA LOCALIZAR UMA MEDIDA RGB
// ======================================================

def getChannelMean = { measurementList, channelName ->

    def keys = measurementList.keySet()

    def matchingKey = keys.find { key ->

        def keyLower = key.toLowerCase()
        def channelLower = channelName.toLowerCase()

        return keyLower.contains(channelLower) &&
               keyLower.contains("mean")
    }

    if (matchingKey != null) {
        return measurementList.get(matchingKey)
    }

    return Double.NaN
}

// ======================================================
// 12. FUNÇÃO PARA CALCULAR SATURAÇÃO MÉDIA
// ======================================================

// A saturação é calculada a partir das médias RGB:
//
// saturation = (max(R,G,B) - min(R,G,B)) / max(R,G,B)
//
// Branco -> R,G,B semelhantes -> saturação baixa
// Rosa   -> R,G,B diferentes  -> saturação maior

def calculateSaturation = { red, green, blue ->

    if (Double.isNaN(red) ||
        Double.isNaN(green) ||
        Double.isNaN(blue)) {

        return Double.NaN
    }

    double maxRGB = Math.max(
            red,
            Math.max(green, blue)
    )

    double minRGB = Math.min(
            red,
            Math.min(green, blue)
    )

    if (maxRGB <= 0.0) {
        return 0.0
    }

    return (maxRGB - minRGB) / maxRGB
}

// ======================================================
// 13. FUNÇÃO PARA CALCULAR BRILHO MÉDIO
// ======================================================

def calculateBrightness = { red, green, blue ->

    if (Double.isNaN(red) ||
        Double.isNaN(green) ||
        Double.isNaN(blue)) {

        return Double.NaN
    }

    double brightness =
            (red + green + blue) / 3.0

    // QuPath pode fornecer valores RGB em 0-255
    // ou normalizados em 0-1.
    if (brightness > 1.5) {
        brightness = brightness / 255.0
    }

    return brightness
}

// ======================================================
// 14. FILTRAR POR ÁREA, CIRCULARIDADE E COR
// ======================================================

def adipocytesToRemove = []

int rejectedArea = 0
int rejectedCircularity = 0
int rejectedColor = 0
int rejectedMissingColor = 0

allAdipocytes.each { adipocyte ->

    def measurements = adipocyte.getMeasurementList()

    // --------------------------------------------------
    // ÁREA
    // --------------------------------------------------

    double area = 0.0

    if (measurements.containsKey("Area µm^2")) {

        area = measurements.get("Area µm^2")

    } else if (measurements.containsKey("Area px")) {

        area = measurements.get("Area px")

    } else {

        area = adipocyte.getROI().getArea()
    }

    // --------------------------------------------------
    // CIRCULARIDADE
    // --------------------------------------------------

    double circularity = 0.0

    if (measurements.containsKey("Circularity")) {

        circularity =
                measurements.get("Circularity")

    } else if (measurements.containsKey("Shape: Circularity")) {

        circularity =
                measurements.get("Shape: Circularity")
    }

    // --------------------------------------------------
    // RGB MÉDIO
    // --------------------------------------------------

    double redMean =
            getChannelMean(
                    measurements,
                    "Red"
            )

    double greenMean =
            getChannelMean(
                    measurements,
                    "Green"
            )

    double blueMean =
            getChannelMean(
                    measurements,
                    "Blue"
            )

    // --------------------------------------------------
    // SATURAÇÃO E BRILHO
    // --------------------------------------------------

    double meanSaturation =
            calculateSaturation(
                    redMean,
                    greenMean,
                    blueMean
            )

    double meanBrightness =
            calculateBrightness(
                    redMean,
                    greenMean,
                    blueMean
            )

    // Guardar as medições explicitamente
    measurements.put(
            "Adipo_Mean_Red",
            redMean
    )

    measurements.put(
            "Adipo_Mean_Green",
            greenMean
    )

    measurements.put(
            "Adipo_Mean_Blue",
            blueMean
    )

    measurements.put(
            "Adipo_Mean_Saturation",
            meanSaturation
    )

    measurements.put(
            "Adipo_Mean_Brightness",
            meanBrightness
    )

    // --------------------------------------------------
    // TESTE DE ÁREA
    // --------------------------------------------------

    boolean validArea =
            area >= minArea &&
            area <= maxArea

    // --------------------------------------------------
    // TESTE DE CIRCULARIDADE
    // --------------------------------------------------

    boolean validCircularity =
            circularity >= minCircularidade

    // --------------------------------------------------
    // TESTE DE COR
    // --------------------------------------------------

    boolean validColor = true

    if (Double.isNaN(meanSaturation) ||
        Double.isNaN(meanBrightness)) {

        validColor = false
        rejectedMissingColor++

    } else {

        if (meanSaturation > maxMeanSaturation) {
            validColor = false
        }

        if (meanBrightness < minMeanBrightness) {
            validColor = false
        }
    }

    // --------------------------------------------------
    // CLASSIFICAÇÃO DO MOTIVO DA REJEIÇÃO
    // --------------------------------------------------

    if (!validArea) {

        adipocytesToRemove << adipocyte
        rejectedArea++

    } else if (!validCircularity) {

        adipocytesToRemove << adipocyte
        rejectedCircularity++

    } else if (!validColor) {

        adipocytesToRemove << adipocyte
        rejectedColor++
    }
}

// ======================================================
// 15. REMOVER ADIPÓCITOS REJEITADOS
// ======================================================

if (!adipocytesToRemove.isEmpty()) {

    removeObjects(
            adipocytesToRemove,
            true
    )
}

// ======================================================
// 16. ADIPÓCITOS APROVADOS
// ======================================================

def filteredAdipocytes = getAnnotationObjects().findAll {
    it.getPathClass() == adipoClass
}

int numAdipocytesFiltered =
        filteredAdipocytes.size()

println ""
println "======================================================"
println "RESULTADOS DO FILTRO"
println "======================================================"

println "Adipócitos detectados: ${numAdipocytesTotal}"

println "Rejeitados por área: ${rejectedArea}"

println "Rejeitados por circularidade: ${rejectedCircularity}"

println "Rejeitados por cor: ${rejectedColor}"

println "Sem medida de cor: ${rejectedMissingColor}"

println "Adipócitos aprovados: ${numAdipocytesFiltered}"

// ======================================================
// 17. EXTRAIR ÁREAS DOS ADIPÓCITOS APROVADOS
// ======================================================

def adipocyteAreas = filteredAdipocytes.collect { adipocyte ->

    def measurements =
            adipocyte.getMeasurementList()

    if (measurements.containsKey("Area µm^2")) {

        return measurements.get("Area µm^2")

    } else if (measurements.containsKey("Area px")) {

        return measurements.get("Area px")

    } else {

        return adipocyte.getROI().getArea()
    }
}

// ======================================================
// 18. ESTATÍSTICAS DE ÁREA
// ======================================================

double areaTotalAdipocytes =
        adipocyteAreas.sum() ?: 0.0

double areaMediaAdipocytes =
        numAdipocytesFiltered > 0 ?
        areaTotalAdipocytes / numAdipocytesFiltered :
        0.0

double minAreaAdipocytes = 0.0
double maxAreaAdipocytes = 0.0

if (!adipocyteAreas.isEmpty()) {

    minAreaAdipocytes =
            adipocyteAreas.min()

    maxAreaAdipocytes =
            adipocyteAreas.max()
}

// ======================================================
// 19. DESVIO PADRÃO
// ======================================================

double desvioPadraoArea = 0.0

if (numAdipocytesFiltered > 1) {

    double somaQuadradosDiferenca =
            adipocyteAreas.sum { area ->

                Math.pow(
                        area - areaMediaAdipocytes,
                        2
                )
            }

    desvioPadraoArea =
            Math.sqrt(
                    somaQuadradosDiferenca /
                    (numAdipocytesFiltered - 1)
            )
}

// ======================================================
// 20. MEDIDAS NA ANOTAÇÃO POSITIVE
// ======================================================

def mainTissueAnnotation =
        parentROIs.first()

def tissueMeasurements =
        mainTissueAnnotation.getMeasurementList()

tissueMeasurements.put(
        "Area_Total_Tecido_um2",
        areaTotalTecido
)

tissueMeasurements.put(
        "Num_Adipocytes_Totais",
        numAdipocytesTotal
)

tissueMeasurements.put(
        "Num_Adipocytes_Filtrados",
        numAdipocytesFiltered
)

tissueMeasurements.put(
        "Adipocyte_Area_Total_um2",
        areaTotalAdipocytes
)

tissueMeasurements.put(
        "Adipocyte_Area_Media_um2",
        areaMediaAdipocytes
)

tissueMeasurements.put(
        "Adipocyte_Area_Min_um2",
        minAreaAdipocytes
)

tissueMeasurements.put(
        "Adipocyte_Area_Max_um2",
        maxAreaAdipocytes
)

tissueMeasurements.put(
        "Adipocyte_Area_SD_um2",
        desvioPadraoArea
)

tissueMeasurements.put(
        "Rejected_Area",
        rejectedArea
)

tissueMeasurements.put(
        "Rejected_Circularity",
        rejectedCircularity
)

tissueMeasurements.put(
        "Rejected_Color",
        rejectedColor
)

fireHierarchyUpdate()

// ======================================================
// 21. CSV CONSOLIDADO
// ======================================================

def outputDir = buildFilePath(
        PROJECT_BASE_DIR,
        "adipo_results"
)

mkdirs(outputDir)

def csvFile = new File(
        outputDir,
        "consolidado_adipocytes.csv"
)

// ======================================================
// CABEÇALHO
// ======================================================

if (!csvFile.exists()) {

    csvFile.text =
            "Imagem," +
            "Area_Total_Tecido_um2," +
            "Num_Adipocytes_Totais," +
            "Num_Adipocytes_Filtrados," +
            "Adipocyte_Area_Total_um2," +
            "Adipocyte_Area_Media_um2," +
            "Adipocyte_Area_Min_um2," +
            "Adipocyte_Area_Max_um2," +
            "Adipocyte_Area_SD_um2," +
            "Rejected_Area," +
            "Rejected_Circularity," +
            "Rejected_Color," +
            "Rejected_Missing_Color\n"
}

// ======================================================
// ESCREVER RESULTADO DA IMAGEM
// ======================================================

csvFile.append(

        "${imageName}," +
        "${areaTotalTecido}," +
        "${numAdipocytesTotal}," +
        "${numAdipocytesFiltered}," +
        "${areaTotalAdipocytes}," +
        "${areaMediaAdipocytes}," +
        "${minAreaAdipocytes}," +
        "${maxAreaAdipocytes}," +
        "${desvioPadraoArea}," +
        "${rejectedArea}," +
        "${rejectedCircularity}," +
        "${rejectedColor}," +
        "${rejectedMissingColor}\n"
)

// ======================================================
// FINAL
// ======================================================

resetSelection()
fireHierarchyUpdate()

println ""
println "======================================================"
println "PROCESSAMENTO CONCLUÍDO"
println "======================================================"

println "Imagem: ${imageName}"

println String.format(
        "Área total do tecido: %.2f µm²",
        areaTotalTecido
)

println "Adipócitos detectados: ${numAdipocytesTotal}"

println "Adipócitos aprovados: ${numAdipocytesFiltered}"

println String.format(
        "Área média dos adipócitos: %.2f µm²",
        areaMediaAdipocytes
)

println String.format(
        "Desvio padrão: %.2f µm²",
        desvioPadraoArea
)

println ""
println "Filtro de cor:"
println String.format(
        "Saturação máxima: %.3f",
        maxMeanSaturation
)

println String.format(
        "Brilho mínimo: %.3f",
        minMeanBrightness
)

println ""
println "CSV:"
println csvFile.getAbsolutePath()

println "======================================================"