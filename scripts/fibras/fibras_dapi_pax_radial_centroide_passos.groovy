import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs

// ======================================================
// CONFIGURAÇÕES DE FILTRO
// ======================================================
double minCircularidade = 0.6
double maxAspectRatio = 2.5
double radialThreshold = 0.7

// =========================
// ETAPA 0 — ROI & LIMPEZA
// =========================
// 1. Cria as anotações a partir do classificador de pixels
createAnnotationsFromPixelClassifier("muscle", 0.0, 0.0)

// 2. Remove objetos da classe "Negative" (Corrigido)
def negativeObjects = getAnnotationObjects().findAll { it.getPathClass() == getPathClass("Negative") }
removeObjects(negativeObjects, true)

// 3. Seleciona as ROIs de interesse
def parentROIs = getAnnotationObjects().findAll { it.getPathClass() == getPathClass("Positive") }
if (parentROIs.isEmpty()) {
    print "❌ Nenhuma anotação 'muscle' encontrada!"
    return
}

// Limpa filhos anteriores das ROIs selecionadas
parentROIs.each { it.getChildObjects().collect().each { child -> removeObject(child, true) } }

// =========================
// ETAPA 1 — DETECÇÃO DE FIBRAS
// =========================
def fiberCellpose = Cellpose2D.builder('cyto3')
        .pixelSize(1)
        .channels(1)
        .diameter(40) 
        .createAnnotations()
        .classify("Fiber")
        .build()

def imageData = getCurrentImageData()
parentROIs.each { fiberCellpose.detectObjects(imageData, [it]) }

// =========================
// ETAPA 2 — FILTRAR POLIGONAIS
// =========================
def allDetectedFibers = getAnnotationObjects().findAll { it.getPathClass() == getPathClass("Fiber") }
selectObjects(allDetectedFibers)
addShapeMeasurements()

// Usando um nome de variável diferente para evitar o erro de escopo
def fibersToRemove = [] 
def fibers = allDetectedFibers.findAll { fiber ->
    def ml = fiber.getMeasurementList()
    double circ = ml.get("Circularidade") ?: 1.0
    double aspect = ml.get("Shape: Aspect ratio") ?: 1.0
    
    if (circ < minCircularidade || aspect > maxAspectRatio) {
        fibersToRemove << fiber
        return false
    }
    return true
}

removeObjects(fibersToRemove, true)
println "Fibras Poligonais: ${fibers.size()} | Removidas (Longitudinais): ${fibersToRemove.size()}"

// ======================================================
// ETAPA 3 — DAPI / PAX (CORRIGIDO)
// ======================================================

// 1. Criar objetos a partir do pixel classifier
createDetectionsFromPixelClassifier("nuclei_classification", 0.0, 0.0, "SPLIT")

// 2. Coletar DAPI e PAX
def detections = getDetectionObjects().findAll {
    def cls = it.getPathClass()?.toString()
    cls == "dapi" || cls == "pax"
}

println "🔍 Detections encontradas: " + detections.size()

// 3. Vincular cada detecção à fibra correta
int linked = 0
int unlinked = 0

detections.each { det ->
    def cx = det.getROI().getCentroidX()
    def cy = det.getROI().getCentroidY()

    boolean foundParent = false

    fibers.each { fiber ->
        if (fiber.getROI().contains(cx, cy)) {
            fiber.addChildObject(det)
            foundParent = true
            linked++
        }
    }

    if (!foundParent) {
        unlinked++
    }
}

println "🔗 DAPI/PAX vinculados às fibras: $linked"
println "⚠️ DAPI/PAX sem fibra: $unlinked"

// 4. Remover lixo (SEM apagar fibras nem ROIs)
def lixo = getDetectionObjects().findAll {
    !(it in detections)
}

removeObjects(lixo, true)

// ======================================================
// ETAPA 4 — LINK OBJETO → FIBRA
// ======================================================
def linkedObjects = []

detections.each { obj ->

    def roi = obj.getROI()
    double cx = roi.getCentroidX()
    double cy = roi.getCentroidY()

    def parent = fibers.find { f ->
        f.getROI().contains(cx, cy)
    }

    if (parent != null) {
        addObjectBelowParent(parent, obj, true)
        linkedObjects << obj
    } else {
        removeObject(obj, true)
    }
}

println "✅ Objetos vinculados: ${linkedObjects.size()}"

fireHierarchyUpdate()

// ======================================================
// ETAPA 5 — RADIAL INDEX
// ======================================================
fibers.each { fiber ->

    def roiF = fiber.getROI()

    double cx = roiF.getCentroidX()
    double cy = roiF.getCentroidY()

    fiber.getChildObjects().each { obj ->

        def roiN = obj.getROI()

        double ox = roiN.getCentroidX()
        double oy = roiN.getCentroidY()

        double vx = ox - cx
        double vy = oy - cy

        double distCenterObj = Math.sqrt(vx*vx + vy*vy)

        if (distCenterObj == 0) return

        vx /= distCenterObj
        vy /= distCenterObj

        double step = 1.0
        double maxDist = 0
        double x = cx
        double y = cy

        while (roiF.contains(x, y)) {
            x += vx * step
            y += vy * step
            maxDist += step

            if (maxDist > 10000) break
        }

        double radialIndex = distCenterObj / maxDist

        def ml = obj.getMeasurementList()
        ml.put("Radial_Index_Real", radialIndex)

        def base = obj.getPathClass().toString()

        if (radialIndex < radialThreshold)
            obj.setPathClass(getPathClass(base + "_Central"))
        else
            obj.setPathClass(getPathClass(base + "_Peripheral"))
    }
}

fireHierarchyUpdate()

// ======================================================
// ETAPA 6 — ESTATÍSTICA POR FIBRA
// ======================================================
fibers.each { fiber ->

    int dapi_total = 0
    int pax_total = 0

    int dapi_central = 0
    int dapi_peripheral = 0

    int pax_central = 0
    int pax_peripheral = 0

    fiber.getChildObjects().each { obj ->

        def cls = obj.getPathClass().toString()

        if (cls.contains("DAPI")) {
            dapi_total++
            if (cls.contains("Central")) dapi_central++
            if (cls.contains("Peripheral")) dapi_peripheral++
        }

        if (cls.contains("PAX")) {
            pax_total++
            if (cls.contains("Central")) pax_central++
            if (cls.contains("Peripheral")) pax_peripheral++
        }
    }

    def ml = fiber.getMeasurementList()

    ml.put("DAPI_total", dapi_total)
    ml.put("PAX_total", pax_total)

    ml.put("DAPI_central", dapi_central)
    ml.put("DAPI_peripheral", dapi_peripheral)

    ml.put("PAX_central", pax_central)
    ml.put("PAX_peripheral", pax_peripheral)
}

fireHierarchyUpdate()

// ======================================================
// ETAPA 7 — EXPORT
// ======================================================
def imageName = GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName())
def outputFile = buildFilePath(outputDir, imageName + "_fibers.csv")

selectObjects(fibers)
saveAnnotationMeasurements(outputFile)

println "📁 CSV salvo: " + outputFile
println "🎯 PIPELINE SEM FILTRO FINALIZADO"