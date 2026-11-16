import qupath.lib.scripting.QP
import qupath.lib.objects.PathAnnotationObject
import java.nio.file.Paths

// 1. Configurações e limpeza
setImageType('FLUORESCENCE')
removeAnnotations()

// 2. Classificador 1: Detecção do Tecido
createAnnotationsFromPixelClassifier("tissue", 500000.0, 5000.0, "SPLIT")

def tissueAnnotations = getAnnotationObjects().findAll { 
    it.getPathClass()?.getName()?.equalsIgnoreCase("tissue") 
}

if (tissueAnnotations.isEmpty()) {
    print "Aviso: Nenhuma anotação 'tissue' encontrada. Interrompendo."
    return
}

// 3. Classificador 2: Colágeno Total (Positive) dentro de tissue
tissueAnnotations.each { tissue ->
    setSelectedObject(tissue)
    createAnnotationsFromPixelClassifier("col_total", 5.0, 0.0)
}
resolveHierarchy()

def positiveAnnotations = getAnnotationObjects().findAll { 
    it.getPathClass()?.getName()?.equalsIgnoreCase("Positive") 
}

if (positiveAnnotations.isEmpty()) {
    print "Aviso: Nenhuma anotação 'Positive' encontrada. Interrompendo."
    return
}

// 4. Classificador 3: Mede 'red' e 'green' diretamente dentro de Positive sem vetorizar
positiveAnnotations.each { pos ->
    setSelectedObject(pos)
    addPixelClassifierMeasurements("col_type", "Col_Type")
}

// ==========================================
// 5. Cálculo e Extração das Métricas
// ==========================================
def server = getCurrentServer()
def cal = server.getPixelCalibration()
def hasCal = cal.hasPixelSizeMicrons()
def unit = hasCal ? "um^2" : "px^2"

def computeArea = { PathAnnotationObject obj ->
    def roi = obj.getROI()
    if (roi == null) return 0.0
    return hasCal ? roi.getScaledArea(cal.getPixelWidthMicrons(), cal.getPixelHeightMicrons()) : roi.getArea()
}

double totalAreaTissue = tissueAnnotations.collect { computeArea(it) }.sum() ?: 0.0
double totalAreaPositive = positiveAnnotations.collect { computeArea(it) }.sum() ?: 0.0

double totalAreaRed = 0.0
double totalAreaGreen = 0.0

// Acesso seguro ao MeasurementList usando .get(key)
positiveAnnotations.each { pos ->
    def ml = pos.getMeasurementList()
    ml.getMeasurementNames().each { name ->
        def val = ml.get(name) // Sintaxe canônica suportada pelo MeasurementList
        if (val == null || Double.isNaN(val)) return
        
        def lower = name.toLowerCase()
        if (lower.contains("col_type") && lower.contains("area")) {
            if (lower.contains("red")) {
                totalAreaRed += val
            } else if (lower.contains("green")) {
                totalAreaGreen += val
            }
        }
    }
}

// Percentuais e Razões
double pctColInTissue = (totalAreaTissue > 0) ? (totalAreaPositive / totalAreaTissue) * 100.0 : 0.0
double pctRedInPositive = (totalAreaPositive > 0) ? (totalAreaRed / totalAreaPositive) * 100.0 : 0.0
double pctGreenInPositive = (totalAreaPositive > 0) ? (totalAreaGreen / totalAreaPositive) * 100.0 : 0.0

double ratioRedGreen = (totalAreaGreen > 0) ? (totalAreaRed / totalAreaGreen) : Double.NaN
double ratioGreenRed = (totalAreaRed > 0) ? (totalAreaGreen / totalAreaRed) : Double.NaN

def imageName = server.getMetadata().getName()

// ==========================================
// 6. Exportação para CSV (Individual e Compilado)
// ==========================================
def project = QP.getProject()
def baseDir = project != null ? project.getPath().getParent().toString() : System.getProperty("user.home")
def resultsDir = Paths.get(baseDir, "collagen_results").toFile()
if (!resultsDir.exists()) resultsDir.mkdirs()

def compiledCsv = new File(resultsDir, "compiled_collagen_summary.csv")
def singleCsv = new File(resultsDir, "${imageName}_collagen.csv")

def header = [
    "Image_Name",
    "Unit",
    "Area_Tissue",
    "Area_Positive_ColTotal",
    "Pct_ColTotal_in_Tissue",
    "Area_Red",
    "Area_Green",
    "Pct_Red_in_Positive",
    "Pct_Green_in_Positive",
    "Ratio_Red_to_Green",
    "Ratio_Green_to_Red"
].join(",")

def row = [
    imageName,
    unit,
    String.format(Locale.US, "%.3f", totalAreaTissue),
    String.format(Locale.US, "%.3f", totalAreaPositive),
    String.format(Locale.US, "%.4f", pctColInTissue),
    String.format(Locale.US, "%.3f", totalAreaRed),
    String.format(Locale.US, "%.3f", totalAreaGreen),
    String.format(Locale.US, "%.4f", pctRedInPositive),
    String.format(Locale.US, "%.4f", pctGreenInPositive),
    Double.isNaN(ratioRedGreen) ? "NA" : String.format(Locale.US, "%.4f", ratioRedGreen),
    Double.isNaN(ratioGreenRed) ? "NA" : String.format(Locale.US, "%.4f", ratioGreenRed)
].join(",")

singleCsv.withWriter { writer ->
    writer.writeLine(header)
    writer.writeLine(row)
}

synchronized (compiledCsv) {
    boolean fileExists = compiledCsv.exists()
    compiledCsv.withWriterAppend { writer ->
        if (!fileExists) {
            writer.writeLine(header)
        }
        writer.writeLine(row)
    }
}

print "Concluído com sucesso para: ${imageName}"