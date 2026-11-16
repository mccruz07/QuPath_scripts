import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.roi.GeometryTools
import qupath.lib.common.GeneralTools

// ======================================================
// CONFIGURAÇÕES
// ======================================================
setImageType('BRIGHTFIELD_H_E')

double minCircularidade = 0.3
double radialThreshold = 0.7
double margemSegurancaInter = -1.5

int totalCentral = 0
int totalPeripheral = 0
int totalInterNuclei = 0
double areaTotalPositive = 0
double areaTotalInter = 0
double totalFiberArea = 0

def outputDir = buildFilePath(PROJECT_BASE_DIR, "fiber_results")
mkdirs(outputDir)

// ======================================================
// CORES DAS CLASSES
// ======================================================
getPathClass("Cell_central").setColor(0, 255, 0)
getPathClass("Cell_peripheral").setColor(255, 0, 0)
getPathClass("Cell_inter").setColor(0, 0, 255)

// ======================================================
// ETAPA 0 — ROI & LIMPEZA
// ======================================================
createAnnotationsFromPixelClassifier("muscle", 0.0, 0.0)

def parentROIs = getAnnotationObjects().findAll { 
    it.getPathClass() == getPathClass("Positive") 
}

if (parentROIs.isEmpty()) {
    print "Nenhuma anotação 'Positive' encontrada!"
    return
}

parentROIs.each { 
    areaTotalPositive += it.getROI().getArea()
    it.getChildObjects().toList().each { removeObject(it, true) } 
}

// ======================================================
// ETAPA 1 — DETECÇÃO DE FIBRAS
// ======================================================
def fiberCellpose = Cellpose2D.builder('cp_sam')
        .pixelSize(1)
        .channels("Red")
        .diameter(45)
        .useCellposeSAM()
        .createAnnotations()
        .classify("Fiber")
        .build()

def imageData = getCurrentImageData()
parentROIs.each { fiberCellpose.detectObjects(imageData, [it]) }

// ======================================================
// ETAPA 1.5 — INTER
// ======================================================
def interAnnotations = []

parentROIs.each { parent ->
    def fibersInParent = parent.getChildObjects().findAll { 
        it.getPathClass() == getPathClass("Fiber") 
    }

    if (fibersInParent.isEmpty()) return

    def fiberGeoms = fibersInParent.collect { it.getROI().getGeometry() }
    def unionGeom = GeometryTools.union(fiberGeoms)
    def interGeom = parent.getROI().getGeometry().difference(unionGeom)

    if (interGeom != null && !interGeom.isEmpty()) {
        def refinedGeom = interGeom.buffer(margemSegurancaInter)

        if (refinedGeom != null && !refinedGeom.isEmpty()) {
            def interROI = GeometryTools.geometryToROI(refinedGeom, parent.getROI().getImagePlane())
            def inter = PathObjects.createAnnotationObject(interROI, getPathClass("inter"))
            interAnnotations << inter
            areaTotalInter += interROI.getArea()
        }
    }
}

addObjects(interAnnotations)

// ======================================================
// ETAPA 2 — FILTRO DE FIBRAS
// ======================================================
def fibers = getAnnotationObjects().findAll { 
    it.getPathClass() == getPathClass("Fiber") 
}

selectObjects(fibers)
addShapeMeasurements()

def fibersToRemove = fibers.findAll { 
    def circ = it.getMeasurementList().get("Circularity") ?: 
               it.getMeasurementList().get("Shape: Circularity") ?: 1.0
    return circ < minCircularidade 
}

removeObjects(fibersToRemove, true)

fibers = getAnnotationObjects().findAll { 
    it.getPathClass() == getPathClass("Fiber") 
}

fibers.each { totalFiberArea += it.getROI().getArea() }

double avgFiberArea = fibers.size() > 0 ? totalFiberArea / fibers.size() : 0

// ======================================================
// ETAPA 3 — DETECÇÃO DE CÉLULAS
// ======================================================
selectObjects(parentROIs)

runPlugin('qupath.imagej.detect.cells.WatershedCellDetection', 
    '{"detectionImageBrightfield":"Hematoxylin OD",' +
    '"requestedPixelSizeMicrons":0.5,' +
    '"backgroundRadiusMicrons":8.0,' +
    '"medianRadiusMicrons":0.0,' +
    '"sigmaMicrons":1.5,' +
    '"minAreaMicrons":4.0,' +
    '"maxAreaMicrons":60.0,' +
    '"threshold":0.1,' +
    '"maxBackground":2.0,' +
    '"watershedPostProcess":true,' +
    '"cellExpansionMicrons":1.0,' +
    '"includeNuclei":true,' +
    '"smoothBoundaries":true,' +
    '"makeMeasurements":true}')

// ======================================================
// ETAPA 4 — CLASSIFICAÇÃO OTIMIZADA (MUITO MAIS RÁPIDA)
// ======================================================

def allDetections = getDetectionObjects()

// 🔹 Indexa células por centroid (evita recalcular)
def cellCentroids = [:]
allDetections.each { cell ->
    cellCentroids[cell] = cell.getROI().getGeometry().getCentroid()
}

// =========================
// 4A — PROCESSA FIBRAS
// =========================
fibers.each { fiber ->

    def fiberGeom = fiber.getROI().getGeometry()

    // pega só células dentro da fibra
    def cellsInside = allDetections.findAll { cell ->
        fiberGeom.contains(cellCentroids[cell])
    }

    cellsInside.each { cell ->

        // 🔥 reorganiza hierarquia corretamente
        removeObject(cell, false)
        fiber.addChildObject(cell)

        def roiF = fiber.getROI()

        double cx = roiF.getCentroidX()
        double cy = roiF.getCentroidY()
        double ox = cell.getROI().getCentroidX()
        double oy = cell.getROI().getCentroidY()

        double vx = ox - cx
        double vy = oy - cy
        double dist = Math.sqrt(vx*vx + vy*vy)

        if (dist > 0) {

            def line = ROIs.createLineROI(
                cx, cy,
                cx + (vx/dist)*5000,
                cy + (vy/dist)*5000,
                roiF.getImagePlane()
            )

            def intersect = fiberGeom.intersection(line.getGeometry())

            double maxD = 0

            intersect.getCoordinates().each { c ->
                double d = Math.sqrt(Math.pow(c.x - cx, 2) + Math.pow(c.y - cy, 2))
                if (d > maxD) maxD = d
            }

            if (maxD > 0) {
                double idx = dist / maxD

                if (idx < radialThreshold) {
                    cell.setPathClass(getPathClass("Cell_central"))
                    totalCentral++
                } else {
                    cell.setPathClass(getPathClass("Cell_peripheral"))
                    totalPeripheral++
                }
            }
        }

        addObject(cell)
    }
}

// =========================
// 4B — PROCESSA INTER
// =========================
allDetections.each { cell ->

    if (cell.getPathClass() != null) return

    def centroid = cellCentroids[cell]

    def insideInter = interAnnotations.any { inter ->
        inter.getROI().getGeometry().contains(centroid)
    }

    if (insideInter) {
        cell.setPathClass(getPathClass("Cell_inter"))
        totalInterNuclei++
        addObject(cell)
    }
}

// ======================================================
// ATUALIZAÇÃO VISUAL FORTE
// ======================================================
fireHierarchyUpdate()
resetSelection()
selectObjects(getDetectionObjects())

// ======================================================
// ETAPA 5 — EXPORTAÇÃO
// ======================================================
def imageName = GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName())
def fileName = imageName + "_resultados.csv"
def file = new File(outputDir, fileName)

def csvContent = new StringBuilder()

csvContent.append("Nome_Imagem,Area_Total_Positive,Num_Fibras,Area_Media_Fibras,Nucleos_Centrais,Nucleos_Perifericos,Area_Total_Inter,Nucleos_Inter\n")

csvContent.append("${imageName},${areaTotalPositive},${fibers.size()},${avgFiberArea},${totalCentral},${totalPeripheral},${areaTotalInter},${totalInterNuclei}\n")

file.text = csvContent.toString()

println "--- RESULTADO FINAL PARA: ${imageName} ---"
println "Centrais: ${totalCentral} | Periféricos: ${totalPeripheral} | Inter: ${totalInterNuclei}"
println "Arquivo salvo em: ${file.getAbsolutePath()}"