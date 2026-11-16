import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.common.GeneralTools

clearAllObjects()
// ======================================================
// PARÂMETROS DE FILTRAGEM
// ======================================================
double minArea = 1000.0        // Área mínima da fibra (em µm² ou px)
double maxArea = 4000.0      // Área máxima da fibra
double minCircularidade = 0.45 // Mínimo de circularidade (0.0 a 1.0)

// 1. Encontrar o tecido via Pixel Classifier
createAnnotationsFromPixelClassifier("muscle", 2000.0, 0.0)

def parentROIs = getAnnotationObjects().findAll { 
    it.getPathClass() == getPathClass("Positive") 
}

if (parentROIs.isEmpty()) {
    println "Nenhuma anotação 'Positive' foi encontrada na imagem!"
    return
}

// Limpa detecções/anotações filhas anteriores
parentROIs.each { parent ->
    parent.getChildObjects().toList().each { removeObject(it, true) }
}

// 2. Configurar e rodar o Cellpose SAM (sem o método .diameter)
def cellposeSAM = Cellpose2D.builder('cp_sam')
        .pixelSize(1.0)
        .channels("Red")
        .useCellposeSAM()
        .useGPU(false)
        .createAnnotations()
        .classify("Fiber")
        .build()

def imageData = getCurrentImageData()
parentROIs.each { cellposeSAM.detectObjects(imageData, [it]) }

// 3. Obter todas as fibras detectadas inicialmente
def allFibers = getAnnotationObjects().findAll { 
    it.getPathClass() == getPathClass("Fiber") 
}

int numFibrasTotais = allFibers.size()

// 4. Calcular medições de forma
selectObjects(allFibers)
addShapeMeasurements()

// 5. Filtrar fibras por área e circularidade
def fibersToRemove = allFibers.findAll { fiber ->
    def area = fiber.getMeasurementList().get("Area µm^2") ?: fiber.getMeasurementList().get("Area px") ?: 0.0
    def circ = fiber.getMeasurementList().get("Circularity") ?: fiber.getMeasurementList().get("Shape: Circularity") ?: 0.0
    
    return (area < minArea || area > maxArea || circ < minCircularidade)
}

removeObjects(fibersToRemove, true)

// 6. Lista de fibras após o filtro
def filteredFibers = getAnnotationObjects().findAll { 
    it.getPathClass() == getPathClass("Fiber") 
}

int numFibrasFiltradas = filteredFibers.size()

// 7. Extrair áreas das fibras filtradas para estatísticas
def fiberAreas = filteredFibers.collect { fiber ->
    fiber.getMeasurementList().get("Area µm^2") ?: fiber.getMeasurementList().get("Area px") ?: 0.0
}

double areaTotalTecido = parentROIs.sum { it.getROI().getArea() } ?: 0.0
double areaTotalFibrasFiltradas = fiberAreas.sum() ?: 0.0
double areaMediaFibrasFiltradas = numFibrasFiltradas > 0 ? (areaTotalFibrasFiltradas / numFibrasFiltradas) : 0.0

// Cálculo do Desvio Padrão Amostral da Área das fibras filtradas
double desvioPadraoArea = 0.0
if (numFibrasFiltradas > 1) {
    double somaQuadradosDiferenca = fiberAreas.sum { area -> Math.pow(area - areaMediaFibrasFiltradas, 2) }
    desvioPadraoArea = Math.sqrt(somaQuadradosDiferenca / (numFibrasFiltradas - 1))
}

// 8. Grava as métricas consolidadas na anotação do tecido (Positive)
def mainTissueAnnotation = parentROIs.first()
mainTissueAnnotation.getMeasurementList().put("Area_Total_Tecido", areaTotalTecido)
mainTissueAnnotation.getMeasurementList().put("Num_Fibras_Totais", numFibrasTotais)
mainTissueAnnotation.getMeasurementList().put("Num_Fibras_Filtradas", numFibrasFiltradas)
mainTissueAnnotation.getMeasurementList().put("Area_Media_Fibras_Filtradas", areaMediaFibrasFiltradas)
mainTissueAnnotation.getMeasurementList().put("Desvio_Padrao_Area_Fibras_Filtradas", desvioPadraoArea)

fireHierarchyUpdate()


// ======================================================
// 9. EXPORTAÇÃO AUTOMÁTICA EM UM ÚNICO CSV CONSOLIDADO
// ======================================================
def imageName = GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName())
def csvFile = new File(buildFilePath(PROJECT_BASE_DIR, "consolidado_medidas_fibras.csv"))

// Cria o cabeçalho se o arquivo ainda não existir
if (!csvFile.exists()) {
    csvFile.text = "Imagem,Area_Total_Tecido,Num_Fibras_Totais,Num_Fibras_Filtradas,Area_Media_Fibras_Filtradas,Desvio_Padrao_Area_Fibras_Filtradas\n"
}

// Concatena a linha da imagem atual no final do arquivo
csvFile.append("${imageName},${areaTotalTecido},${numFibrasTotais},${numFibrasFiltradas},${areaMediaFibrasFiltradas},${desvioPadraoArea}\n")

println "Processamento concluído e dados exportados para: ${csvFile.getAbsolutePath()}"