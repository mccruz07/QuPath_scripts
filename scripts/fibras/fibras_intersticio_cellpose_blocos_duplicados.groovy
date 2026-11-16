import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.common.GeneralTools

// ======================================================
// CONFIGURAÇÕES
// ======================================================
double minCircularidade = 0.6
double maxAspectRatio = 2.5
double radialThreshold = 0.7

def outputDir = buildFilePath(PROJECT_BASE_DIR, "fiber_results")
mkdirs(outputDir)

// =========================
// ETAPA 0 — ROI & LIMPEZA
// =========================
createAnnotationsFromPixelClassifier("muscle", 0.0, 0.0)

def negativeObjects = getAnnotationObjects().findAll {
    it.getPathClass() == getPathClass("Negative")
}
removeObjects(negativeObjects, true)

def parentROIs = getAnnotationObjects().findAll {
    it.getPathClass() == getPathClass("Positive")
}

if (parentROIs.isEmpty()) {
    print "❌ Nenhuma anotação 'muscle' encontrada!"
    return
}

// limpar filhos antigos
parentROIs.each {
    it.getChildObjects().collect().each { child -> removeObject(child, true) }
}

// =========================
// ETAPA 1 — DETECÇÃO DE FIBRAS
// =========================
def fiberCellpose = Cellpose2D.builder('cp_sam')
        .pixelSize(1)
        .channels("Red")
        .diameter(40)
        .createAnnotations()
        .classify("Fiber")
        .build()

def imageData = getCurrentImageData()

parentROIs.each {
    fiberCellpose.detectObjects(imageData, [it])
}

import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs

// ======================================================
// CONFIGURAÇÕES
// ======================================================
double minCircularidade = 0.6
double maxAspectRatio = 2.5
double radialThreshold = 0.7

def outputDir = buildFilePath(PROJECT_BASE_DIR, "fiber_results")
mkdirs(outputDir)

// =========================
// ETAPA 0 — ROI & LIMPEZA
// =========================
createAnnotationsFromPixelClassifier("muscle", 0.0, 0.0)

def negativeObjects = getAnnotationObjects().findAll {
    it.getPathClass() == getPathClass("Negative")
}
removeObjects(negativeObjects, true)

def parentROIs = getAnnotationObjects().findAll {
    it.getPathClass() == getPathClass("Positive")
}

if (parentROIs.isEmpty()) {
    print "Nenhuma anotação 'muscle' encontrada!"
    return
}

// limpar filhos antigos
parentROIs.each {
    it.getChildObjects().collect().each { child ->
        removeObject(child, true)
    }
}

// =========================
// ETAPA 1 — DETECÇÃO DE FIBRAS
// =========================
def fiberCellpose = Cellpose2D.builder('cyto3')
        .pixelSize(1)
        .channels("Red")
        .diameter(40)
        .createAnnotations()
        .classify("Fiber")
        .build()

def imageData = getCurrentImageData()

parentROIs.each {
    fiberCellpose.detectObjects(imageData, [it])
}

// =========================
// ETAPA 1.5 — CRIAR INTER (VERSÃO RÁPIDA)
// =========================

// criar anotação baseada em classificação de pixels
createAnnotationsFromPixelClassifier(
        "fiber_mask",
        0.0,
        0.0
)

def fiberMask = getAnnotationObjects().findAll {
    it.getPathClass() == getPathClass("fiber_mask")
}

def interAnnotations = []

parentROIs.each { parent ->

    def parentGeom = parent.getROI().getGeometry()

    fiberMask.each { mask ->

        def maskGeom = mask.getROI().getGeometry()

        if (parentGeom.intersects(maskGeom)) {
            parentGeom = parentGeom.difference(maskGeom)
        }
    }

    if (!parentGeom.isEmpty()) {

        def interROI = ROIs.createROI(
                parentGeom,
                parent.getROI().getImagePlane()
        )

        def inter = PathObjects.createAnnotationObject(
                interROI,
                getPathClass("inter")
        )

        interAnnotations << inter
    }
}

addObjects(interAnnotations)

println "Inter criado: ${interAnnotations.size()}"

// =========================
// ETAPA 2 — FILTRO DE FIBRAS
// =========================

def allFibers = getAnnotationObjects().findAll {
    it.getPathClass() == getPathClass("Fiber")
}

println "Total fibras antes do filtro: ${allFibers.size()}"

selectObjects(allFibers)
addShapeMeasurements()

def fibersToRemove = []

allFibers.each { fiber ->

    def ml = fiber.getMeasurementList()

    double circ =
            ml.get("Circularity")
            ?: ml.get("Shape: Circularity")
            ?: -1

    double aspect =
            ml.get("Aspect ratio")
            ?: ml.get("Shape: Aspect ratio")
            ?: -1

    if (circ < minCircularidade || aspect > maxAspectRatio) {
        fibersToRemove << fiber
    }
}

println "Para remover: ${fibersToRemove.size()}"

removeObjects(fibersToRemove, true)

println "Remoção concluída"

// =========================
// LISTA FINAL
// =========================

def fibers = getAnnotationObjects().findAll {
    it.getPathClass() == getPathClass("Fiber")
}

println "Fibras finais: ${fibers.size()}"

// =========================
// ETAPA 2 — FILTRO DE FIBRAS
// =========================

def allFibers = getAnnotationObjects().findAll {
    it.getPathClass() == getPathClass("Fiber")
}

println "Total fibras antes do filtro: ${allFibers.size()}"

// garantir que as medidas existem
selectObjects(allFibers)
addShapeMeasurements()

def fibersToRemove = []

allFibers.each { fiber ->

    def ml = fiber.getMeasurementList()

    double circ =
            ml.get("Circularity")
            ?: ml.get("Shape: Circularity")
            ?: -1

    double aspect =
            ml.get("Aspect ratio")
            ?: ml.get("Shape: Aspect ratio")
            ?: -1

    println "Circ: ${circ} | Aspect: ${aspect}"

    if (circ < minCircularidade || aspect > maxAspectRatio) {
        fibersToRemove << fiber
    }
}

println "Para remover: ${fibersToRemove.size()}"

removeObjects(fibersToRemove, true)

println "Remoção concluída"

// =========================
// LISTA FINAL DE FIBRAS
// =========================

def fibers = getAnnotationObjects().findAll {
    it.getPathClass() == getPathClass("Fiber")
}

println "Fibras finais: ${fibers.size()}"