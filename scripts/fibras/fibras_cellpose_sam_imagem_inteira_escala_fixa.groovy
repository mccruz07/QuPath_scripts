import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.common.GeneralTools
import java.io.FileWriter

// =====================================================
// CONFIGURAÇÕES
// =====================================================

// Escala real
// 225 pixels = 500 nm = 0.5 µm
double micronPerPixel = 0.5 / 239.0

// Filtros morfológicos
double minCircularity = 0.55
double maxAspectRatio = 2.5

// Cellpose
String modelName = "cyto3"
double cellDiameter = 0

// =====================================================
// PASTA DE SAÍDA
// =====================================================

def project = getProject()

String projectDir =
        project.getPath().getParent().toString()

String outputDir =
        buildFilePath(projectDir, "Fiber_Analysis")

mkdirs(outputDir)

// =====================================================
// CSV COMPILADO
// =====================================================

String summaryFile =
        buildFilePath(
                outputDir,
                "All_Images_Summary.csv"
        )

if (!new File(summaryFile).exists()) {

    new File(summaryFile).text =
            "ImageName,NumberOfFibers,MeanFiberArea_um2,MeanCircularity\n"
}

// =====================================================
// IMAGEM ATUAL
// =====================================================

def imageData = getCurrentImageData()
def server = imageData.getServer()

String imageName =
        GeneralTools.stripExtension(
                server.getMetadata().getName()
        )

println("================================")
println("Processando: " + imageName)

// =====================================================
// LIMPAR OBJETOS
// =====================================================

clearAllObjects()

// =====================================================
// ANNOTATION IMAGEM INTEIRA
// =====================================================

def roi = ROIs.createRectangleROI(
        0,
        0,
        server.getWidth(),
        server.getHeight(),
        null
)

def annotation =
        PathObjects.createAnnotationObject(roi)

addObject(annotation)

// =====================================================
// CELLPOSE SAM
// =====================================================

def cellpose = Cellpose2D.builder(modelName)
        .useCellposeSAM()
        .diameter(cellDiameter)
        .measureShape()
        .simplify(1.0)
        .build()

cellpose.detectObjects(
        imageData,
        getAnnotationObjects()
)

def detections = getDetectionObjects()

println("Objetos detectados: " + detections.size())

// =====================================================
// FILTER OUT OBJETOS ALONGADOS
// =====================================================

def keptObjects = []

detections.each { obj ->

    def ml = obj.getMeasurementList()

    double circularity =
            ml.get("Circularity") ?: 0

    double majorAxis =
            ml.get("Max diameter") ?:
            ml.get("Length") ?: 0

    double minorAxis =
            ml.get("Min diameter") ?:
            ml.get("Width") ?: 1

    double aspectRatio =
            majorAxis /
            Math.max(minorAxis, 1e-6)

    boolean keep =
            circularity >= minCircularity &&
            aspectRatio <= maxAspectRatio

    if (keep)
        keptObjects << obj
}

// substituir detecções
clearDetections()
addObjects(keptObjects)

println("Objetos mantidos: " + keptObjects.size())

// =====================================================
// EXPORTAR CSV INDIVIDUAL
// =====================================================

String csvPath =
        buildFilePath(
                outputDir,
                imageName + "_fibers.csv"
        )

def writer =
        new FileWriter(csvPath)

writer.write(
        "ImageName,FiberID,NumberOfFibers,Area_um2,Circularity\n"
)

double totalArea = 0
double totalCircularity = 0

int fiberCount =
        keptObjects.size()

int id = 1

keptObjects.each { obj ->

    def ml = obj.getMeasurementList()

    // ------------------------------------------
    // Área original em pixels²
    // ------------------------------------------

    double areaPixels =
            ml.get("Area px^2") ?:
            ml.get("Area") ?: 0

    // ------------------------------------------
    // Converter para µm²
    // ------------------------------------------

    double areaUm2 =
            areaPixels *
            Math.pow(micronPerPixel, 2)

    double circularity =
            ml.get("Circularity") ?: 0

    totalArea += areaUm2
    totalCircularity += circularity

    writer.write(
            "${imageName}," +
            "${id}," +
            "${fiberCount}," +
            "${areaUm2}," +
            "${circularity}\n"
    )

    id++
}

writer.close()

// =====================================================
// CSV COMPILADO
// =====================================================

double meanArea =
        fiberCount > 0 ?
        totalArea / fiberCount : 0

double meanCircularity =
        fiberCount > 0 ?
        totalCircularity / fiberCount : 0

def summaryWriter =
        new FileWriter(summaryFile, true)

summaryWriter.write(
        "${imageName}," +
        "${fiberCount}," +
        "${meanArea}," +
        "${meanCircularity}\n"
)

summaryWriter.close()

println("Concluído: " + imageName)
println("CSV salvo em:")
println(csvPath)
println("================================")