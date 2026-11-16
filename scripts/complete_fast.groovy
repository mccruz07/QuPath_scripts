/*
 * =====================================================================
 * FAST CORTEX + MICROGLIA IBA + GR
 * QuPath 0.7
 * =====================================================================
 *
 * Mesma lógica e mesmas medidas da versão NORMAL.
 *
 * Principal otimização:
 *
 *   JTS STRtree para busca espacial GR x IBA.
 *
 * Em vez de cada GR testar todas as microglias,
 * testa somente IBA cujos envelopes estão próximos.
 *
 * =====================================================================
 */

import qupath.lib.roi.GeometryTools
import qupath.imagej.tools.IJTools
import ij.process.ByteProcessor
import org.locationtech.jts.index.strtree.STRtree


// =====================================================================
// CONFIGURAÇÕES
// =====================================================================

String CORTEX_NAME = "Cortex"
String CORTEX_CLASS = "Cortex"

String IBA_CLASSIFIER = "IBA"
String GR_CLASSIFIER = "GR_1"

double IBA_MIN_AREA = 65.0
double IBA_MAX_AREA = 2000.0

double GR_MIN_AREA = 45.0
double GR_MAX_AREA = 1500.0

double MIN_HOLE_AREA = 0.0

double MIN_GR_OVERLAP_FRACTION = 0.20

double SOMA_OPENING_RADIUS_UM = 1.5

int SKELETON_PADDING = 3

boolean REMOVE_PREVIOUS_OBJECTS = true


// =====================================================================
// PREPARAÇÃO
// =====================================================================

setImageType('FLUORESCENCE')

def imageData =
    getCurrentImageData()

def hierarchy =
    imageData.getHierarchy()

def server =
    imageData.getServer()

def cal =
    server.getPixelCalibration()

def projectEntry =
    getProjectEntry()


if (projectEntry == null) {
    throw new IllegalStateException(
        "A imagem precisa estar dentro de um projeto QuPath."
    )
}


if (!cal.hasPixelSizeMicrons()) {
    throw new IllegalStateException(
        "A imagem não possui pixel size em micrômetros."
    )
}


String imageName =
    projectEntry.getImageName()


double pixelWidth =
    cal.getPixelWidthMicrons()

double pixelHeight =
    cal.getPixelHeightMicrons()

double meanPixelSize =
    (pixelWidth + pixelHeight) / 2.0

double pixelAreaUm2 =
    pixelWidth * pixelHeight

double diagonalLength =
    Math.sqrt(
        pixelWidth * pixelWidth +
        pixelHeight * pixelHeight
    )


// =====================================================================
// CORTEX
// =====================================================================

def cortexCandidates =
    hierarchy.getAnnotationObjects().findAll { obj ->

        obj.getName() == CORTEX_NAME ||
        obj.getPathClass()?.toString() == CORTEX_CLASS
    }


if (cortexCandidates.size() != 1) {
    throw new IllegalStateException(
        "Precisa existir exatamente uma annotation Cortex."
    )
}


def cortex =
    cortexCandidates[0]

def cortexROI =
    cortex.getROI()


if (
    cortexROI == null ||
    !cortexROI.isArea()
) {
    throw new IllegalStateException(
        "A annotation Cortex não possui uma ROI válida."
    )
}


def cortexGeometry =
    GeometryTools.roiToGeometry(
        cortexROI
    )


double cortexAreaUm2 =
    cortexGeometry.getArea() *
    pixelAreaUm2


double cortexAreaMm2 =
    cortexAreaUm2 /
    1_000_000.0


println ""
println "========================================================"
println "FAST CORTEX + IBA + GR"
println "========================================================"
println "Image: ${imageName}"
println "Cortex area: ${cortexAreaMm2} mm2"
println "IBA min/max: ${IBA_MIN_AREA} / ${IBA_MAX_AREA} um2"
println "GR min/max: ${GR_MIN_AREA} / ${GR_MAX_AREA} um2"
println "========================================================"


// =====================================================================
// REMOVER ANÁLISE ANTERIOR
// =====================================================================

if (REMOVE_PREVIOUS_OBJECTS) {

    def previous =
        hierarchy.getAnnotationObjects().findAll { obj ->

            if (obj == cortex)
                return false

            def cls =
                obj.getPathClass()?.toString()

            if (!(cls in ["Microglia", "GR"]))
                return false

            def r =
                obj.getROI()

            if (r == null)
                return false

            return cortexROI.contains(
                r.getCentroidX(),
                r.getCentroidY()
            )
        }


    if (!previous.isEmpty()) {

        println ""
        println "Removing previous objects: ${previous.size()}"

        hierarchy.removeObjects(
            previous,
            true
        )
    }
}


// =====================================================================
// SELECIONAR CORTEX
// =====================================================================

def selectCortex = {

    hierarchy
        .getSelectionModel()
        .clearSelection()

    hierarchy
        .getSelectionModel()
        .setSelectedObject(
            cortex
        )
}


// =====================================================================
// IBA
// =====================================================================

println ""
println "Detecting IBA..."


def beforeIBA =
    hierarchy.getAnnotationObjects() as Set


selectCortex()


createAnnotationsFromPixelClassifier(
    IBA_CLASSIFIER,
    IBA_MIN_AREA,
    MIN_HOLE_AREA,
    "SPLIT"
)


def afterIBA =
    hierarchy.getAnnotationObjects() as Set


def detectedIBAData = []


afterIBA.each { obj ->

    if (
        beforeIBA.contains(obj) ||
        obj == cortex
    ) {
        return
    }


    def roi =
        obj.getROI()


    if (roi == null)
        return


    if (
        !cortexROI.contains(
            roi.getCentroidX(),
            roi.getCentroidY()
        )
    ) {
        return
    }


    def geom =
        GeometryTools.roiToGeometry(
            roi
        )


    if (
        geom == null ||
        geom.isEmpty()
    ) {
        return
    }


    double areaUm2 =
        geom.getArea() *
        pixelAreaUm2


    detectedIBAData << [

        object:
            obj,

        roi:
            roi,

        geometry:
            geom,

        areaUm2:
            areaUm2
    ]
}


int ibaDetectedBeforeMaxFilter =
    detectedIBAData.size()


// =====================================================================
// FILTRO IBA
// =====================================================================

def ibaDataList = []
def excludedLargeIBA = []


detectedIBAData.each { data ->

    if (data.areaUm2 > IBA_MAX_AREA) {

        excludedLargeIBA << data.object

    } else {

        data.associatedGR = []

        ibaDataList << data
    }
}


if (!excludedLargeIBA.isEmpty()) {

    hierarchy.removeObjects(
        excludedLargeIBA,
        true
    )
}


int excludedLargeIBACount =
    excludedLargeIBA.size()


ibaDataList.eachWithIndex { data, i ->

    data.index =
        i

    data.object.setPathClass(
        getPathClass(
            "Microglia"
        )
    )

    data.object.setName(
        "IBA_${i + 1}"
    )
}


println "IBA detected before max filter: ${ibaDetectedBeforeMaxFilter}"
println "IBA excluded > ${IBA_MAX_AREA}: ${excludedLargeIBACount}"
println "Final microglia: ${ibaDataList.size()}"


// =====================================================================
// STRTREE
// =====================================================================

println ""
println "Building IBA spatial index..."


def ibaSpatialIndex =
    new STRtree()


ibaDataList.each { data ->

    ibaSpatialIndex.insert(
        data.geometry.getEnvelopeInternal(),
        data
    )
}


ibaSpatialIndex.build()


println "Spatial index ready."


// =====================================================================
// GR
// =====================================================================

println ""
println "Detecting GR..."


def beforeGR =
    hierarchy.getAnnotationObjects() as Set


selectCortex()


createAnnotationsFromPixelClassifier(
    GR_CLASSIFIER,
    GR_MIN_AREA,
    MIN_HOLE_AREA,
    "SPLIT"
)


def afterGR =
    hierarchy.getAnnotationObjects() as Set


def detectedGRData = []


afterGR.each { obj ->

    if (
        beforeGR.contains(obj) ||
        obj == cortex
    ) {
        return
    }


    def roi =
        obj.getROI()


    if (roi == null)
        return


    if (
        !cortexROI.contains(
            roi.getCentroidX(),
            roi.getCentroidY()
        )
    ) {
        return
    }


    def geom =
        GeometryTools.roiToGeometry(
            roi
        )


    if (
        geom == null ||
        geom.isEmpty()
    ) {
        return
    }


    double areaUm2 =
        geom.getArea() *
        pixelAreaUm2


    detectedGRData << [

        object:
            obj,

        geometry:
            geom,

        areaUm2:
            areaUm2
    ]
}


int grDetectedBeforeMaxFilter =
    detectedGRData.size()


// =====================================================================
// FILTRO GR
// =====================================================================

def grDataList = []
def excludedLargeGR = []


detectedGRData.each { data ->

    if (data.areaUm2 > GR_MAX_AREA) {

        excludedLargeGR << data.object

    } else {

        grDataList << data
    }
}


if (!excludedLargeGR.isEmpty()) {

    hierarchy.removeObjects(
        excludedLargeGR,
        true
    )
}


int excludedLargeGRCount =
    excludedLargeGR.size()


grDataList.eachWithIndex { data, i ->

    data.object.setPathClass(
        getPathClass(
            "GR"
        )
    )

    data.object.setName(
        "GR_${i + 1}"
    )
}


println "GR detected before max filter: ${grDetectedBeforeMaxFilter}"
println "GR excluded > ${GR_MAX_AREA}: ${excludedLargeGRCount}"
println "Final GR: ${grDataList.size()}"


// =====================================================================
// GR -> MELHOR IBA VIA STRTREE
// =====================================================================

println ""
println "Associating GR using spatial index..."


grDataList.each { grData ->

    def grGeom =
        grData.geometry


    double grAreaPx2 =
        grGeom.getArea()


    int bestIBAIndex =
        -1


    double bestFraction =
        0.0


    double bestIntersectionPx2 =
        0.0


    def candidates =
        ibaSpatialIndex.query(
            grGeom.getEnvelopeInternal()
        )


    candidates.each { ibaData ->

        double intersectionPx2 =
            GeometryTools.intersectionArea(
                grGeom,
                ibaData.geometry
            )


        if (intersectionPx2 <= 0)
            return


        double fraction =
            grAreaPx2 > 0 ?
            intersectionPx2 / grAreaPx2 :
            0.0


        if (fraction > bestFraction) {

            bestFraction =
                fraction

            bestIntersectionPx2 =
                intersectionPx2

            bestIBAIndex =
                ibaData.index
        }
    }


    boolean associated =
        bestIBAIndex >= 0 &&
        bestFraction >=
        MIN_GR_OVERLAP_FRACTION


    grData.associated =
        associated


    grData.bestFraction =
        bestFraction


    grData.bestIBAIndex =
        bestIBAIndex


    if (associated) {

        ibaDataList[
            bestIBAIndex
        ].associatedGR.add(
            [
                fraction:
                    bestFraction,

                intersectionPx2:
                    bestIntersectionPx2
            ]
        )
    }
}


// =====================================================================
// MORFOLOGIA
// =====================================================================

println ""
println "Calculating microglia morphology..."


def results = []

int failedSoma = 0
int failedSkeleton = 0


ibaDataList.eachWithIndex { data, index ->

    def cell =
        data.object

    def roi =
        data.roi

    def geometry =
        data.geometry


    double areaUm2 =
        data.areaUm2


    // =================================================================
    // PERIMETER
    // =================================================================

    double perimeterUm =
        geometry.getLength() *
        meanPixelSize


    // =================================================================
    // CIRCULARITY
    // =================================================================

    double circularity =
        perimeterUm > 0 ?
        4.0 *
        Math.PI *
        areaUm2 /
        (perimeterUm * perimeterUm) :
        Double.NaN


    // =================================================================
    // CONVEX HULL
    // =================================================================

    def convexHull =
        geometry.convexHull()


    double convexHullAreaUm2 =
        Double.NaN


    double convexHullPerimeterUm =
        Double.NaN


    if (
        convexHull != null &&
        !convexHull.isEmpty()
    ) {

        convexHullAreaUm2 =
            convexHull.getArea() *
            pixelAreaUm2


        convexHullPerimeterUm =
            convexHull.getLength() *
            meanPixelSize
    }


    // =================================================================
    // SOLIDITY
    // =================================================================

    double solidity =
        convexHullAreaUm2 > 0 ?
        areaUm2 /
        convexHullAreaUm2 :
        Double.NaN


    // =================================================================
    // ROUGHNESS
    // =================================================================

    double roughness =
        convexHullPerimeterUm > 0 ?
        perimeterUm /
        convexHullPerimeterUm :
        Double.NaN


    // =================================================================
    // SHAPE COMPLEXITY
    // =================================================================

    double shapeComplexity =
        areaUm2 > 0 ?
        (perimeterUm * perimeterUm) /
        (4.0 * Math.PI * areaUm2) :
        Double.NaN


    // =================================================================
    // SOMA
    // =================================================================

    double somaAreaUm2 =
        Double.NaN


    double somaFraction =
        Double.NaN


    try {

        double openingRadiusPx =
            SOMA_OPENING_RADIUS_UM /
            meanPixelSize


        def eroded =
            geometry.buffer(
                -openingRadiusPx
            )


        if (
            eroded != null &&
            !eroded.isEmpty()
        ) {

            def opened =
                eroded.buffer(
                    openingRadiusPx
                )


            if (
                opened != null &&
                !opened.isEmpty()
            ) {

                def somaGeometry =
                    GeometryTools.findLargestPolygon(
                        opened
                    )


                if (
                    somaGeometry != null &&
                    !somaGeometry.isEmpty()
                ) {

                    somaAreaUm2 =
                        somaGeometry.getArea() *
                        pixelAreaUm2


                    if (areaUm2 > 0) {

                        somaFraction =
                            somaAreaUm2 /
                            areaUm2
                    }

                } else {

                    failedSoma++
                }

            } else {

                failedSoma++
            }

        } else {

            failedSoma++
        }

    } catch (Exception e) {

        failedSoma++
    }


    // =================================================================
    // SKELETON
    // =================================================================

    double skeletonLengthUm =
        Double.NaN


    try {

        int x0 =
            Math.floor(
                roi.getBoundsX()
            ).intValue() -
            SKELETON_PADDING


        int y0 =
            Math.floor(
                roi.getBoundsY()
            ).intValue() -
            SKELETON_PADDING


        int width =
            Math.ceil(
                roi.getBoundsWidth()
            ).intValue() +
            2 * SKELETON_PADDING +
            2


        int height =
            Math.ceil(
                roi.getBoundsHeight()
            ).intValue() +
            2 * SKELETON_PADDING +
            2


        if (
            width >= 3 &&
            height >= 3
        ) {

            def bp =
                new ByteProcessor(
                    width,
                    height
                )


            bp.setValue(0)
            bp.fill()

            bp.setValue(255)


            def ijRoi =
                IJTools.convertToIJRoi(
                    roi,
                    x0,
                    y0,
                    1.0
                )


            bp.fill(
                ijRoi
            )


            bp.skeletonize()


            skeletonLengthUm =
                0.0


            for (
                int y = 1;
                y < height - 1;
                y++
            ) {

                for (
                    int x = 1;
                    x < width - 1;
                    x++
                ) {

                    if (
                        bp.get(
                            x,
                            y
                        ) == 0
                    ) {
                        continue
                    }


                    if (
                        bp.get(
                            x + 1,
                            y
                        ) > 0
                    ) {

                        skeletonLengthUm +=
                            pixelWidth
                    }


                    if (
                        bp.get(
                            x,
                            y + 1
                        ) > 0
                    ) {

                        skeletonLengthUm +=
                            pixelHeight
                    }


                    if (
                        bp.get(
                            x + 1,
                            y + 1
                        ) > 0
                    ) {

                        skeletonLengthUm +=
                            diagonalLength
                    }


                    if (
                        bp.get(
                            x - 1,
                            y + 1
                        ) > 0
                    ) {

                        skeletonLengthUm +=
                            diagonalLength
                    }
                }
            }

        } else {

            failedSkeleton++
        }

    } catch (Exception e) {

        failedSkeleton++
    }


    // =================================================================
    // SKELETON DENSITY
    // =================================================================

    double skeletonDensity =
        (
            !Double.isNaN(
                skeletonLengthUm
            ) &&
            areaUm2 > 0
        ) ?
        skeletonLengthUm /
        areaUm2 :
        Double.NaN


    // =================================================================
    // GR ASSOCIADO
    // =================================================================

    def associatedGR =
        data.associatedGR


    int associatedGRCount =
        associatedGR.size()


    boolean grPositive =
        associatedGRCount > 0


    double maxGRFractionInsideIBA =
        associatedGR.isEmpty() ?
        0.0 :
        associatedGR
            .collect {
                it.fraction
            }
            .max()


    // =================================================================
    // MICROGLIA MEASUREMENTS
    // =================================================================

    def m =
        cell.getMeasurements()


    m["Microglia: IBA area µm^2"] =
        areaUm2

    m["Microglia: Perimeter µm"] =
        perimeterUm

    m["Microglia: Circularity"] =
        circularity

    m["Microglia: Convex hull area µm^2"] =
        convexHullAreaUm2

    m["Microglia: Convex hull perimeter µm"] =
        convexHullPerimeterUm

    m["Microglia: Solidity"] =
        solidity

    m["Microglia: Roughness"] =
        roughness

    m["Microglia: Shape complexity"] =
        shapeComplexity

    m["Microglia: Estimated soma area µm^2"] =
        somaAreaUm2

    m["Microglia: Soma fraction"] =
        somaFraction

    m["Microglia: Skeleton length µm"] =
        skeletonLengthUm

    m["Microglia: Skeleton density µm^-1"] =
        skeletonDensity

    m["Microglia: GR positive"] =
        grPositive ? 1.0 : 0.0

    m["Microglia: Associated GR count"] =
        associatedGRCount

    m["Microglia: Max GR fraction inside IBA"] =
        maxGRFractionInsideIBA


    // =================================================================
    // RESULTS
    // =================================================================

    results << [

        id:
            index + 1,

        area:
            areaUm2,

        perimeter:
            perimeterUm,

        circularity:
            circularity,

        convexHullArea:
            convexHullAreaUm2,

        convexHullPerimeter:
            convexHullPerimeterUm,

        solidity:
            solidity,

        roughness:
            roughness,

        shapeComplexity:
            shapeComplexity,

        somaArea:
            somaAreaUm2,

        somaFraction:
            somaFraction,

        skeletonLength:
            skeletonLengthUm,

        skeletonDensity:
            skeletonDensity,

        grPositive:
            grPositive,

        associatedGRCount:
            associatedGRCount,

        grFractionInsideIBA:
            maxGRFractionInsideIBA
    ]
}


// =====================================================================
// COUNTS
// =====================================================================

int ibaTotal =
    results.size()


int grTotal =
    grDataList.size()


int ibaWithGR =
    results.count {
        it.grPositive
    }


int ibaOnly =
    ibaTotal -
    ibaWithGR


int grWithIBA =
    grDataList.count {
        it.associated
    }


int grOnly =
    grTotal -
    grWithIBA


// =====================================================================
// NORMALIZAÇÃO POR ÁREA
// =====================================================================

double ibaDensity =
    cortexAreaMm2 > 0 ?
    ibaTotal /
    cortexAreaMm2 :
    Double.NaN


double grDensity =
    cortexAreaMm2 > 0 ?
    grTotal /
    cortexAreaMm2 :
    Double.NaN


double ibaWithGRDensity =
    cortexAreaMm2 > 0 ?
    ibaWithGR /
    cortexAreaMm2 :
    Double.NaN


double ibaOnlyDensity =
    cortexAreaMm2 > 0 ?
    ibaOnly /
    cortexAreaMm2 :
    Double.NaN


double grWithIBADensity =
    cortexAreaMm2 > 0 ?
    grWithIBA /
    cortexAreaMm2 :
    Double.NaN


double grOnlyDensity =
    cortexAreaMm2 > 0 ?
    grOnly /
    cortexAreaMm2 :
    Double.NaN


// =====================================================================
// RATIO / PERCENTAGES
// =====================================================================

double ibaGrRatio =
    grTotal > 0 ?
    ibaTotal /
    (double)grTotal :
    Double.NaN


double ibaWithGRPct =
    ibaTotal > 0 ?
    100.0 *
    ibaWithGR /
    ibaTotal :
    Double.NaN


double ibaOnlyPct =
    ibaTotal > 0 ?
    100.0 *
    ibaOnly /
    ibaTotal :
    Double.NaN


double grWithIBAPct =
    grTotal > 0 ?
    100.0 *
    grWithIBA /
    grTotal :
    Double.NaN


double grOnlyPct =
    grTotal > 0 ?
    100.0 *
    grOnly /
    grTotal :
    Double.NaN


// =====================================================================
// MEAN
// =====================================================================

def meanOf = { String key ->

    def values =
        results
            .collect {
                it[key]
            }
            .findAll { value ->

                if (value == null)
                    return false

                double d =
                    value as double

                return (
                    !Double.isNaN(d) &&
                    !Double.isInfinite(d)
                )
            }


    if (values.isEmpty())
        return Double.NaN


    return values.sum() /
        values.size()
}


// =====================================================================
// MEASUREMENTS CORTEX
// =====================================================================

def cortexMeasurements =
    cortex.getMeasurements()


cortexMeasurements["Cortex: Area mm^2"] =
    cortexAreaMm2


cortexMeasurements["Cortex: IBA detected before max filter"] =
    ibaDetectedBeforeMaxFilter

cortexMeasurements["Cortex: IBA excluded above max area"] =
    excludedLargeIBACount

cortexMeasurements["Cortex: GR detected before max filter"] =
    grDetectedBeforeMaxFilter

cortexMeasurements["Cortex: GR excluded above max area"] =
    excludedLargeGRCount


cortexMeasurements["Cortex: Microglia count"] =
    ibaTotal

cortexMeasurements["Cortex: Microglia density cells/mm^2"] =
    ibaDensity


cortexMeasurements["Cortex: GR count"] =
    grTotal

cortexMeasurements["Cortex: GR density cells/mm^2"] =
    grDensity


cortexMeasurements["Cortex: IBA/GR ratio"] =
    ibaGrRatio


cortexMeasurements["Cortex: IBA+GR count"] =
    ibaWithGR

cortexMeasurements["Cortex: IBA+GR density cells/mm^2"] =
    ibaWithGRDensity

cortexMeasurements["Cortex: IBA+GR % of IBA"] =
    ibaWithGRPct


cortexMeasurements["Cortex: IBA only count"] =
    ibaOnly

cortexMeasurements["Cortex: IBA only density cells/mm^2"] =
    ibaOnlyDensity

cortexMeasurements["Cortex: IBA only % of IBA"] =
    ibaOnlyPct


cortexMeasurements["Cortex: GR with IBA count"] =
    grWithIBA

cortexMeasurements["Cortex: GR with IBA density cells/mm^2"] =
    grWithIBADensity

cortexMeasurements["Cortex: GR with IBA % of GR"] =
    grWithIBAPct


cortexMeasurements["Cortex: GR only count"] =
    grOnly

cortexMeasurements["Cortex: GR only density cells/mm^2"] =
    grOnlyDensity

cortexMeasurements["Cortex: GR only % of GR"] =
    grOnlyPct


cortexMeasurements["Cortex: Mean IBA area µm^2"] =
    meanOf("area")

cortexMeasurements["Cortex: Mean perimeter µm"] =
    meanOf("perimeter")

cortexMeasurements["Cortex: Mean circularity"] =
    meanOf("circularity")

cortexMeasurements["Cortex: Mean convex hull area µm^2"] =
    meanOf("convexHullArea")

cortexMeasurements["Cortex: Mean convex hull perimeter µm"] =
    meanOf("convexHullPerimeter")

cortexMeasurements["Cortex: Mean solidity"] =
    meanOf("solidity")

cortexMeasurements["Cortex: Mean roughness"] =
    meanOf("roughness")

cortexMeasurements["Cortex: Mean shape complexity"] =
    meanOf("shapeComplexity")

cortexMeasurements["Cortex: Mean estimated soma area µm^2"] =
    meanOf("somaArea")

cortexMeasurements["Cortex: Mean soma fraction"] =
    meanOf("somaFraction")

cortexMeasurements["Cortex: Mean skeleton length µm"] =
    meanOf("skeletonLength")

cortexMeasurements["Cortex: Mean skeleton density µm^-1"] =
    meanOf("skeletonDensity")


// =====================================================================
// CSV HELPERS
// =====================================================================

def csvEscape = { value ->

    if (value == null)
        return ""

    String s =
        value.toString()


    if (
        s.contains(",") ||
        s.contains("\"") ||
        s.contains("\n") ||
        s.contains("\r")
    ) {

        s =
            "\"" +
            s.replace(
                "\"",
                "\"\""
            ) +
            "\""
    }


    return s
}


def numberToCSV = { value ->

    if (value == null)
        return ""


    double d =
        value as double


    if (
        Double.isNaN(d) ||
        Double.isInfinite(d)
    ) {
        return ""
    }


    return Double.toString(d)
}


def appendCSVLine = { File file, List values ->

    String line =
        values.join(",") +
        System.lineSeparator()


    file.append(
        line,
        "UTF-8"
    )
}


// =====================================================================
// OUTPUT
// =====================================================================

String outputDir =
    makePathInProject(
        "Microglia_Results_FAST"
    )


File outputDirectory =
    new File(
        outputDir
    )


if (!outputDirectory.exists()) {

    outputDirectory.mkdirs()
}


File allCellsFile =
    new File(
        outputDirectory,
        "microglia_all_cells_FAST.csv"
    )


File summaryFile =
    new File(
        outputDirectory,
        "microglia_summary_FAST.csv"
    )


// =====================================================================
// CSV INDIVIDUAL
// =====================================================================

def allCellsHeader = [

    "Image",
    "Microglia_ID",
    "IBA_Area_um2",
    "Perimeter_um",
    "Circularity",
    "Convex_Hull_Area_um2",
    "Convex_Hull_Perimeter_um",
    "Solidity",
    "Roughness",
    "Shape_Complexity",
    "Estimated_Soma_Area_um2",
    "Soma_Fraction",
    "Skeleton_Length_um",
    "Skeleton_Density_um-1",
    "GR_Positive",
    "Associated_GR_Count",
    "Max_GR_Fraction_Inside_IBA"
]


if (!allCellsFile.exists()) {

    appendCSVLine(
        allCellsFile,
        allCellsHeader
    )
}


results.each { r ->

    appendCSVLine(
        allCellsFile,
        [

            csvEscape(
                imageName
            ),

            r.id,

            numberToCSV(
                r.area
            ),

            numberToCSV(
                r.perimeter
            ),

            numberToCSV(
                r.circularity
            ),

            numberToCSV(
                r.convexHullArea
            ),

            numberToCSV(
                r.convexHullPerimeter
            ),

            numberToCSV(
                r.solidity
            ),

            numberToCSV(
                r.roughness
            ),

            numberToCSV(
                r.shapeComplexity
            ),

            numberToCSV(
                r.somaArea
            ),

            numberToCSV(
                r.somaFraction
            ),

            numberToCSV(
                r.skeletonLength
            ),

            numberToCSV(
                r.skeletonDensity
            ),

            r.grPositive ? 1 : 0,

            r.associatedGRCount,

            numberToCSV(
                r.grFractionInsideIBA
            )
        ]
    )
}


// =====================================================================
// CSV SUMMARY
// =====================================================================

def summaryHeader = [

    "Image",
    "ROI_Area_mm2",

    "IBA_Detected_Before_Max_Filter",
    "IBA_Excluded_Above_Max_Area",

    "Microglia_Count",
    "Microglia_Density_cells_mm2",

    "GR_Detected_Before_Max_Filter",
    "GR_Excluded_Above_Max_Area",

    "GR_Count",
    "GR_Density_cells_mm2",

    "IBA_GR_Ratio",

    "IBA_GR_Count",
    "IBA_GR_Density_cells_mm2",
    "IBA_GR_pct_of_IBA",

    "IBA_Only_Count",
    "IBA_Only_Density_cells_mm2",
    "IBA_Only_pct_of_IBA",

    "GR_with_IBA_Count",
    "GR_with_IBA_Density_cells_mm2",
    "GR_with_IBA_pct_of_GR",

    "GR_Only_Count",
    "GR_Only_Density_cells_mm2",
    "GR_Only_pct_of_GR",

    "Mean_IBA_Area_um2",
    "Mean_Perimeter_um",
    "Mean_Circularity",
    "Mean_Convex_Hull_Area_um2",
    "Mean_Convex_Hull_Perimeter_um",
    "Mean_Solidity",
    "Mean_Roughness",
    "Mean_Shape_Complexity",
    "Mean_Estimated_Soma_Area_um2",
    "Mean_Soma_Fraction",
    "Mean_Skeleton_Length_um",
    "Mean_Skeleton_Density_um-1"
]


if (!summaryFile.exists()) {

    appendCSVLine(
        summaryFile,
        summaryHeader
    )
}


appendCSVLine(
    summaryFile,
    [

        csvEscape(
            imageName
        ),

        numberToCSV(
            cortexAreaMm2
        ),

        ibaDetectedBeforeMaxFilter,

        excludedLargeIBACount,

        ibaTotal,

        numberToCSV(
            ibaDensity
        ),

        grDetectedBeforeMaxFilter,

        excludedLargeGRCount,

        grTotal,

        numberToCSV(
            grDensity
        ),

        numberToCSV(
            ibaGrRatio
        ),

        ibaWithGR,

        numberToCSV(
            ibaWithGRDensity
        ),

        numberToCSV(
            ibaWithGRPct
        ),

        ibaOnly,

        numberToCSV(
            ibaOnlyDensity
        ),

        numberToCSV(
            ibaOnlyPct
        ),

        grWithIBA,

        numberToCSV(
            grWithIBADensity
        ),

        numberToCSV(
            grWithIBAPct
        ),

        grOnly,

        numberToCSV(
            grOnlyDensity
        ),

        numberToCSV(
            grOnlyPct
        ),

        numberToCSV(
            meanOf("area")
        ),

        numberToCSV(
            meanOf("perimeter")
        ),

        numberToCSV(
            meanOf("circularity")
        ),

        numberToCSV(
            meanOf("convexHullArea")
        ),

        numberToCSV(
            meanOf("convexHullPerimeter")
        ),

        numberToCSV(
            meanOf("solidity")
        ),

        numberToCSV(
            meanOf("roughness")
        ),

        numberToCSV(
            meanOf("shapeComplexity")
        ),

        numberToCSV(
            meanOf("somaArea")
        ),

        numberToCSV(
            meanOf("somaFraction")
        ),

        numberToCSV(
            meanOf("skeletonLength")
        ),

        numberToCSV(
            meanOf("skeletonDensity")
        )
    ]
)


// =====================================================================
// FINAL
// =====================================================================

hierarchy
    .getSelectionModel()
    .clearSelection()


fireHierarchyUpdate()


println ""
println "========================================================"
println "FAST ANALYSIS FINISHED"
println "========================================================"

println "Image:"
println "  ${imageName}"

println ""

println "Cortex area:"
println "  ${cortexAreaMm2} mm2"

println ""

println "IBA:"
println "  Raw = ${ibaDetectedBeforeMaxFilter}"
println "  Large excluded = ${excludedLargeIBACount}"
println "  Final = ${ibaTotal}"
println "  Density = ${ibaDensity} cells/mm2"

println ""

println "GR:"
println "  Raw = ${grDetectedBeforeMaxFilter}"
println "  Large excluded = ${excludedLargeGRCount}"
println "  Final = ${grTotal}"
println "  Density = ${grDensity} cells/mm2"

println ""

println "IBA + GR:"
println "  Count = ${ibaWithGR}"
println "  Density = ${ibaWithGRDensity} cells/mm2"
println "  % IBA = ${ibaWithGRPct}"

println ""

println "IBA only:"
println "  Count = ${ibaOnly}"
println "  Density = ${ibaOnlyDensity} cells/mm2"

println ""

println "GR with IBA:"
println "  Count = ${grWithIBA}"
println "  Density = ${grWithIBADensity} cells/mm2"
println "  % GR = ${grWithIBAPct}"

println ""

println "GR only:"
println "  Count = ${grOnly}"
println "  Density = ${grOnlyDensity} cells/mm2"

println ""

println "Mean IBA area:"
println "  ${meanOf('area')} um2"

println ""

println "Soma failures:"
println "  ${failedSoma}"

println ""

println "Skeleton failures:"
println "  ${failedSkeleton}"

println ""

println "Summary:"
println "  ${summaryFile.getAbsolutePath()}"

println "========================================================"