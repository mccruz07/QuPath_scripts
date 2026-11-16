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
// ETAPA 2 — FILTRO DE FIBRAS (CORRIGIDO)
// =========================
def allFibers = getAnnotationObjects().findAll {
    it.getPathClass() == getPathClass("Fiber")
}

println "Total fibras: ${allFibers.size()}"

// Garante que as medidas existem
selectObjects(allFibers)
addShapeMeasurements()

def fibersToRemove = []

allFibers.each { fiber ->

    def ml = fiber.getMeasurementList()

    // 🔥 PEGAR DO QUPath (com fallback seguro)
    double circ = ml.get("Circularity") 
                 ?: ml.get("Shape: Circularity") 
                 ?: -1

    double aspect = ml.get("Aspect ratio") 
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

// 🔥 CRIAR LISTA FINAL
def fibers = getAnnotationObjects().findAll {
    it.getPathClass() == getPathClass("Fiber")
}

println "Fibras finais: ${fibers.size()}"

// ======================================================
// ETAPA 3 — DAPI / PAX (ROBUSTO)
// ======================================================
createDetectionsFromPixelClassifier("nuclei_classification", 15.0, 0.0, "SPLIT")

def detections = getDetectionObjects().findAll {

    def cls = it.getPathClass()?.toString()?.toLowerCase()

    return cls != null && (
        cls.contains("dapi") ||
        cls.contains("pax")
    )
}

println "🔍 Detections DAPI/PAX encontradas: " + detections.size()

// ⚠️ NÃO remover nada ainda (debug primeiro)
// removeObjects(lixo, true)


// ======================================================
// ETAPA 4 — LINK OBJETO → FIBRA (ROBUSTO DE VERDADE)
// ======================================================

// 🔥 garantir lista atualizada de fibras
fibers = getAnnotationObjects().findAll {
    it.getPathClass() == getPathClass("Fiber")
}

println "🧵 Fibras disponíveis: " + fibers.size()

// limpar filhos antigos das fibras (IMPORTANTE)
fibers.each { f ->
    f.getChildObjects().toList().each { removeObject(it, true) }
}

int linkedCount = 0
int unlinkedCount = 0

detections.each { obj ->

    def roiObj = obj.getROI()
    def geomObj = roiObj.getGeometry()

    def parentFiber = fibers.find { f ->

        def geomFiber = f.getROI().getGeometry()

        // 🔥 INTERSEÇÃO REAL (muito mais robusto que centroid)
        return geomFiber.intersects(geomObj)
    }

    if (parentFiber != null) {
        parentFiber.addChildObject(obj)
        linkedCount++
    } else {
        unlinkedCount++
        // NÃO remover — importante para debug
        // removeObject(obj, true)
    }
}

println "🔗 Objetos vinculados: ${linkedCount}"
println "⚠️ Objetos NÃO vinculados: ${unlinkedCount}"

fireHierarchyUpdate()
// ======================================================
// ETAPA 5 — RADIAL INDEX FINAL (CENTRO → BORDA REAL)
// ======================================================
fibers.each { fiber ->

    def roiF = fiber.getROI()
    def geom = roiF.getGeometry()

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

        // normalizar direção
        vx /= distCenterObj
        vy /= distCenterObj

        // linha longa na direção do núcleo
        double scale = 10000
        def line = ROIs.createLineROI(
            cx, cy,
            cx + vx * scale,
            cy + vy * scale,
            roiF.getImagePlane()
        )

        def intersection = geom.intersection(line.getGeometry())

        if (intersection.isEmpty()) return

        double maxDist = 0

        intersection.getCoordinates().each { coord ->
            double dx = coord.x - cx
            double dy = coord.y - cy
            double d = Math.sqrt(dx*dx + dy*dy)
            if (d > maxDist) maxDist = d
        }

        if (maxDist == 0) return

        double radialIndex = distCenterObj / maxDist

        def ml = obj.getMeasurementList()
        ml.put("Radial_Index", radialIndex)

        def base = obj.getPathClass().toString().toLowerCase()

        if (radialIndex < radialThreshold)
            obj.setPathClass(getPathClass(base + "_central"))
        else
            obj.setPathClass(getPathClass(base + "_peripheral"))
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

        def cls = obj.getPathClass().toString().toLowerCase()

        if (cls.contains("dapi")) {
            dapi_total++
            if (cls.contains("central")) dapi_central++
            if (cls.contains("peripheral")) dapi_peripheral++
        }

        if (cls.contains("pax")) {
            pax_total++
            if (cls.contains("central")) pax_central++
            if (cls.contains("peripheral")) pax_peripheral++
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
println "🎯 PIPELINE FINALIZADO COM SUCESSO"