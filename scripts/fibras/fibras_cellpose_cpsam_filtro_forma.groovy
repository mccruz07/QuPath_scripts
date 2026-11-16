import qupath.ext.biop.cellpose.Cellpose2D

println "========== INICIANDO =========="

// ============================================================
// 1. Preparação da imagem
// ============================================================

setImageType('BRIGHTFIELD_OTHER')

setColorDeconvolutionStains(
'{"Name":"H-DAB default","Stain 1":"Hematoxylin","Values 1":"0.6511078257574492 0.7011930431234068 0.29049426072255424","Stain 2":"DAB","Values 2":"0.26916687204956063 0.5682411743268502 0.777593185920953","Background":"255 255 255"}'
)

// ============================================================
// 2. Criar anotação do Pixel Classifier
// ============================================================

createAnnotationsFromPixelClassifier("muscle", 0.0, 0.0)

def positive = getAnnotationObjects().findAll {
    it.getPathClass()?.toString() == "Positive"
}

if (positive.isEmpty()) {
    println "Nenhuma anotação Positive encontrada."
    return
}

selectObjects(positive)

println "Anotações positivas: ${positive.size()}"

// ============================================================
// 3. Cellpose
// ============================================================

def cellpose = Cellpose2D.builder("cpsam")
        .pixelSize(2)
        .channels("Red")
        .measureShape()
        .measureIntensity()
        .build()

cellpose.detectObjects(
        getCurrentImageData(),
        getSelectedObjects()
)

println "Cellpose concluído."

// ============================================================
// 4. Calcular Aspect Ratio = Max diameter / Min diameter
// ============================================================

getDetectionObjects().each { det ->

    def ml = det.getMeasurementList()

    double maxD = ml.get("Max diameter µm")
    double minD = ml.get("Min diameter µm")

    if (Double.isNaN(maxD) || Double.isNaN(minD) || minD <= 0)
        return

    ml.put("Aspect ratio", maxD / minD)
}

println "Aspect ratio calculado."

// ============================================================
// 5. Filtrar objetos
// ============================================================

double minCircularity = 0.30
double minArea = 50.0
double maxArea = 100000.0

def removeList = getDetectionObjects().findAll {

    def ml = it.getMeasurementList()

    double area = ml.get("Area µm^2")
    double circ = ml.get("Circularity")

    circ < minCircularity ||
    area < minArea ||
    area > maxArea
}

removeObjects(removeList, true)

println "Objetos removidos = ${removeList.size()}"

// ============================================================
// 6. Resumo
// ============================================================

println ""
println "Objetos restantes = ${getDetectionObjects().size()}"

println ""
println "Medidas disponíveis:"

if (!getDetectionObjects().isEmpty()) {

    getDetectionObjects().first()
            .getMeasurementList()
            .getMeasurementNames()
            .each {
                println it
            }
}

println ""
println "========== FIM =========="