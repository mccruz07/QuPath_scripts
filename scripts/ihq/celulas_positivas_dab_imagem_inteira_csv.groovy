import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
setImageType('BRIGHTFIELD_H_DAB')
// =====================================================
// CONFIGURAÇÕES
// =====================================================

// ALTERE AQUI
double thresholdPositive1 = 0.21

double thresholdPositive2 = 0.40
double thresholdPositive3 = 0.60

String csvName = String.format(
        "PositiveCellDetection_Threshold_%.2f.csv",
        thresholdPositive1
).replace(",", ".")

// =====================================================
// PROJETO E CAMINHO DE SAÍDA
// =====================================================

def project = getProject()

if (project == null) {
    print "Abra um projeto do QuPath!"
    return
}

// Pasta do projeto (CORRIGIDO)
def projectFile = project.getPath().toFile()
def projectDir = projectFile.getParentFile()

def outputFile = new File(projectDir, csvName)

// Criar cabeçalho se arquivo não existir
if (!outputFile.exists()) {
    outputFile << "Image,Num Detections,Num Negative,Num Positive,Positive %\n"
}

// =====================================================
// LIMPAR OBJETOS
// =====================================================

clearAllObjects()

// =====================================================
// ANOTAÇÃO IMAGEM INTEIRA
// =====================================================

def imageData = getCurrentImageData()
def server = imageData.getServer()

double width = server.getWidth()
double height = server.getHeight()

def roi = ROIs.createRectangleROI(
        0,
        0,
        width,
        height,
        null
)

def annotation =
        PathObjects.createAnnotationObject(roi)

addObject(annotation)
selectObjects(annotation)

// =====================================================
// POSITIVE CELL DETECTION
// =====================================================

String params = """
{
  "detectionImageBrightfield":"Hematoxylin OD",
  "backgroundByReconstruction":true,
  "backgroundRadius":0.0,
  "medianRadius":2.0,
  "sigma":6.0,
  "minArea":150.0,
  "maxArea":2500.0,
  "threshold":0.06,
  "maxBackground":2.0,
  "watershedPostProcess":true,
  "excludeDAB":false,
  "cellExpansion":20.0,
  "includeNuclei":true,
  "smoothBoundaries":true,
  "makeMeasurements":true,
  "thresholdCompartment":"Cytoplasm: DAB OD mean",
  "thresholdPositive1":${thresholdPositive1},
  "thresholdPositive2":${thresholdPositive2},
  "thresholdPositive3":${thresholdPositive3},
  "singleThreshold":true
}
"""

runPlugin(
        'qupath.imagej.detect.cells.PositiveCellDetection',
        params
)

// =====================================================
// COLETAR RESULTADOS
// =====================================================

def detections = getDetectionObjects()

int numDetections = detections.size()
int numNegative = 0
int numPositive = 0

detections.each { det ->

    def pathClass = det.getPathClass()

    if (pathClass != null) {

        String className = pathClass.toString()

        if (className.contains("Negative")) {
            numNegative++
        }
        else if (className.contains("Positive")) {
            numPositive++
        }
    }
}

double positivePercent = 0

if (numDetections > 0) {
    positivePercent =
            (numPositive / numDetections) * 100
}

// =====================================================
// SALVAR CSV
// =====================================================

String imageName = getProjectEntry().getImageName()

String line = [
        imageName,
        numDetections,
        numNegative,
        numPositive,
        String.format("%.2f", positivePercent)
].join(",")

outputFile << line + "\n"

// =====================================================
// LOG
// =====================================================

print "\n====================================="
print "Imagem: ${imageName}"
print "ThresholdPositive1 = ${thresholdPositive1}"
print "CSV salvo:"
print outputFile.getAbsolutePath()