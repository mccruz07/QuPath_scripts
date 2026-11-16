/*
 * =====================================================================
 * CORTEX + MICROGLIA IBA + GR
 * QuPath 0.7
 * =====================================================================
 *
 * IBA:
 *   min area = 65 um2
 *   max area = 2000 um2
 *
 * GR:
 *   min area = 45 um2
 *
 * Associação GR -> IBA:
 *
 *      Area(GR intersect IBA)
 *      ---------------------- >= 0.20
 *             Area(GR)
 *
 * Cada GR é associado à IBA com maior overlap.
 *
 * =====================================================================
 */

import qupath.lib.roi.GeometryTools
import qupath.imagej.tools.IJTools
import ij.process.ByteProcessor


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
double MIN_HOLE_AREA = 0.0

double MIN_GR_OVERLAP_FRACTION = 0.20

double SOMA_OPENING_RADIUS_UM = 1.5

int SKELETON_PADDING = 3

boolean REMOVE_PREVIOUS_OBJECTS = true


// =====================================================================
// PREPARAÇÃO
// =====================================================================

setImageType('FLUORESCENCE')

def imageData = getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def server = imageData.getServer()
def cal = server.getPixelCalibration()
def projectEntry = getProjectEntry()

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


// =====================================================================
// CORTEX
// =====================================================================

def cortexCandidates =
    hierarchy
        .getAnnotationObjects()
        .findAll { obj ->

            obj.getName() == CORTEX_NAME ||

            obj.getPathClass()?.toString() == CORTEX_CLASS
        }

if (cortexCandidates.isEmpty()) {
    throw new IllegalStateException(
        "Nenhuma annotation Cortex encontrada."
    )
}

if (cortexCandidates.size() > 1) {
    throw new IllegalStateException(
        "Mais de uma annotation Cortex encontrada."
    )
}

def cortex =
    cortexCandidates[0]

def cortexROI =
    cortex.getROI()

def cortexGeometry =
    GeometryTools.roiToGeometry(
        cortexROI
    )

double cortexAreaUm2 =
    cortexGeometry.getArea() *
    pixelWidth *
    pixelHeight

double cortexAreaMm2 =
    cortexAreaUm2 /
    1_000_000.0


println ""
println "========================================================"
println "CORTEX + IBA + GR"
println "========================================================"
println "Image: ${imageName}"
println "Cortex area: ${cortexAreaMm2} mm2"
println "IBA min area: ${IBA_MIN_AREA} um2"
println "IBA max area: ${IBA_MAX_AREA} um2"
println "GR threshold: ${MIN_GR_OVERLAP_FRACTION * 100.0}%"
println "========================================================"


// =====================================================================
// REMOVER OBJETOS ANTERIORES
// =====================================================================

if (REMOVE_PREVIOUS_OBJECTS) {

    def previous =
        hierarchy
            .getAnnotationObjects()
            .findAll { obj ->

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
// DETECTAR IBA
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

def detectedIBA =
    afterIBA.findAll { obj ->

        !beforeIBA.contains(obj) &&
        obj != cortex
    }


// manter objetos com centro dentro do Cortex
detectedIBA =
    detectedIBA.findAll { obj ->

        def r =
            obj.getROI()

        r != null &&
        cortexROI.contains(
            r.getCentroidX(),
            r.getCentroidY()
        )
    }


int ibaDetectedBeforeMaxFilter =
    detectedIBA.size()


// =====================================================================
// FILTRO DE ÁREA MÁXIMA
// =====================================================================

def excludedLargeIBA = []

def microglia = []


detectedIBA.each { obj ->

    def geom =
        GeometryTools.roiToGeometry(
            obj.getROI()
        )

    if (
        geom == null ||
        geom.isEmpty()
    ) {
        return
    }

    double objectAreaUm2 =
        geom.getArea() *
        pixelWidth *
        pixelHeight

    if (objectAreaUm2 > IBA_MAX_AREA) {

        excludedLargeIBA << obj

    } else {

        microglia << obj
    }
}


if (!excludedLargeIBA.isEmpty()) {

    hierarchy.removeObjects(
        excludedLargeIBA,
        true
    )
}


int excludedLargeCount =
    excludedLargeIBA.size()


microglia.eachWithIndex { obj, i ->

    obj.setPathClass(
        getPathClass(
            "Microglia"
        )
    )

    obj.setName(
        "IBA_${i + 1}"
    )
}


println "IBA detected before max filter:"
println "  ${ibaDetectedBeforeMaxFilter}"

println "IBA excluded > ${IBA_MAX_AREA} um2:"
println "  ${excludedLargeCount}"

println "Final microglia:"
println "  ${microglia.size()}"


// =====================================================================
// DETECTAR GR
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

def grObjects =
    afterGR.findAll { obj ->

        !beforeGR.contains(obj) &&
        obj != cortex
    }


grObjects =
    grObjects.findAll { obj ->

        def r =
            obj.getROI()

        r != null &&
        cortexROI.contains(
            r.getCentroidX(),
            r.getCentroidY()
        )
    }


grObjects.eachWithIndex { obj, i ->

    obj.setPathClass(
        getPathClass(
            "GR"
        )
    )

    obj.setName(
        "GR_${i + 1}"
    )
}


println "GR objects:"
println "  ${grObjects.size()}"


// =====================================================================
// PRECALCULAR DADOS DAS MICROGLIAS
// =====================================================================

def ibaDataList = []

microglia.eachWithIndex { obj, i ->

    ibaDataList << [

        index:
            i,

        object:
            obj,

        geometry:
            GeometryTools.roiToGeometry(
                obj.getROI()
            ),

        associatedGR:
            []
    ]
}


// =====================================================================
// ASSOCIAR CADA GR À MELHOR IBA
// =====================================================================

println ""
println "Associating GR with best IBA overlap..."

def grDataList = []


grObjects.eachWithIndex { gr, grIndex ->

    def grGeom =
        GeometryTools.roiToGeometry(
            gr.getROI()
        )

    if (
        grGeom == null ||
        grGeom.isEmpty()
    ) {

        grDataList << [

            object:
                gr,

            associated:
                false,

            bestFraction:
                0.0,

            bestIBAIndex:
                -1
        ]

        return
    }


    double grAreaPx2 =
        grGeom.getArea()

    int bestIBAIndex =
        -1

    double bestFraction =
        0.0

    double bestIntersectionPx2 =
        0.0


    ibaDataList.each { ibaData ->

        def ibaGeom =
            ibaData.geometry

        if (
            ibaGeom == null ||
            ibaGeom.isEmpty()
        ) {
            return
        }


        if (
            !grGeom
                .getEnvelopeInternal()
                .intersects(
                    ibaGeom.getEnvelopeInternal()
                )
        ) {
            return
        }


        double intersectionPx2 =
            GeometryTools.intersectionArea(
                grGeom,
                ibaGeom
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
        (
            bestIBAIndex >= 0 &&
            bestFraction >= MIN_GR_OVERLAP_FRACTION
        )


    if (associated) {

        ibaDataList[bestIBAIndex]
            .associatedGR
            .add(
                [
                    object:
                        gr,

                    fraction:
                        bestFraction,

                    intersectionPx2:
                        bestIntersectionPx2
                ]
            )
    }


    grDataList << [

        object:
            gr,

        associated:
            associated,

        bestFraction:
            bestFraction,

        bestIBAIndex:
            bestIBAIndex
    ]
}


// =====================================================================
// MORFOLOGIA IBA
// =====================================================================

def results = []

int failedSoma = 0
int failedSkeleton = 0


ibaDataList.eachWithIndex { ibaData, index ->

    def cell =
        ibaData.object

    def roi =
        cell.getROI()

    def geometry =
        ibaData.geometry


    if (
        roi == null ||
        geometry == null ||
        geometry.isEmpty()
    ) {
        return
    }


    // -----------------------------------------------------------------
    // AREA
    // -----------------------------------------------------------------

    double areaUm2 =
        geometry.getArea() *
        pixelWidth *
        pixelHeight


    // -----------------------------------------------------------------
    // PERIMETER
    // -----------------------------------------------------------------

    double perimeterUm =
        geometry.getLength() *
        meanPixelSize


    // -----------------------------------------------------------------
    // CIRCULARITY
    // -----------------------------------------------------------------

    double circularity =
        Double.NaN

    if (perimeterUm > 0) {

        circularity =
            4.0 *
            Math.PI *
            areaUm2 /
            (perimeterUm * perimeterUm)
    }


    // -----------------------------------------------------------------
    // CONVEX HULL
    // -----------------------------------------------------------------

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
            pixelWidth *
            pixelHeight

        convexHullPerimeterUm =
            convexHull.getLength() *
            meanPixelSize
    }


    // -----------------------------------------------------------------
    // SOLIDITY
    // -----------------------------------------------------------------

    double solidity =
        Double.NaN

    if (convexHullAreaUm2 > 0) {

        solidity =
            areaUm2 /
            convexHullAreaUm2
    }


    // -----------------------------------------------------------------
    // ROUGHNESS
    // -----------------------------------------------------------------

    double roughness =
        Double.NaN

    if (convexHullPerimeterUm > 0) {

        roughness =
            perimeterUm /
            convexHullPerimeterUm
    }


    // -----------------------------------------------------------------
    // SHAPE COMPLEXITY
    // -----------------------------------------------------------------

    double shapeComplexity =
        Double.NaN

    if (areaUm2 > 0) {

        shapeComplexity =
            (perimeterUm * perimeterUm) /
            (4.0 * Math.PI * areaUm2)
    }


    // =================================================================
    // SOMA
    // =================================================================

    double openingRadiusPx =
        SOMA_OPENING_RADIUS_UM /
        meanPixelSize

    double somaAreaUm2 =
        Double.NaN

    double somaFraction =
        Double.NaN


    try {

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
                        pixelWidth *
                        pixelHeight

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

            double diagonalLength =
                Math.sqrt(
                    pixelWidth * pixelWidth +
                    pixelHeight * pixelHeight
                )


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

                    if (bp.get(x, y) == 0)
                        continue


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


    // -----------------------------------------------------------------
    // SKELETON DENSITY
    // -----------------------------------------------------------------

    double skeletonDensity =
        Double.NaN

    if (
        !Double.isNaN(
            skeletonLengthUm
        ) &&
        areaUm2 > 0
    ) {

        skeletonDensity =
            skeletonLengthUm /
            areaUm2
    }


    // =================================================================
    // GR ASSOCIADO
    // =================================================================

    def associatedGR =
        ibaData.associatedGR

    int associatedGRCount =
        associatedGR.size()

    boolean grPositive =
        associatedGRCount > 0

    double maxGRFractionInsideIBA =
        0.0


    if (!associatedGR.isEmpty()) {

        maxGRFractionInsideIBA =
            associatedGR
                .collect {
                    it.fraction
                }
                .max()
    }


    // =================================================================
    // MEDIDAS
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
// COUNTS / DENSITIES / RATIOS
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


double ibaDensity =
    cortexAreaMm2 > 0 ?
    ibaTotal / cortexAreaMm2 :
    Double.NaN

double grDensity =
    cortexAreaMm2 > 0 ?
    grTotal / cortexAreaMm2 :
    Double.NaN

double ibaGrRatio =
    grTotal > 0 ?
    ibaTotal / (double)grTotal :
    Double.NaN

double ibaWithGRPct =
    ibaTotal > 0 ?
    100.0 * ibaWithGR / ibaTotal :
    Double.NaN

double ibaOnlyPct =
    ibaTotal > 0 ?
    100.0 * ibaOnly / ibaTotal :
    Double.NaN

double grWithIBAPct =
    grTotal > 0 ?
    100.0 * grWithIBA / grTotal :
    Double.NaN

double grOnlyPct =
    grTotal > 0 ?
    100.0 * grOnly / grTotal :
    Double.NaN


// =====================================================================
// MÉDIA
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
// CSV
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
    )
        return ""

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


String outputDir =
    makePathInProject(
        "Microglia_Results"
    )

File outputDirectory =
    new File(outputDir)

if (!outputDirectory.exists())
    outputDirectory.mkdirs()


File allCellsFile =
    new File(
        outputDirectory,
        "microglia_all_cells.csv"
    )

File summaryFile =
    new File(
        outputDirectory,
        "microglia_summary.csv"
    )


// =====================================================================
// ALL CELLS CSV
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
            csvEscape(imageName),
            r.id,
            numberToCSV(r.area),
            numberToCSV(r.perimeter),
            numberToCSV(r.circularity),
            numberToCSV(r.convexHullArea),
            numberToCSV(r.convexHullPerimeter),
            numberToCSV(r.solidity),
            numberToCSV(r.roughness),
            numberToCSV(r.shapeComplexity),
            numberToCSV(r.somaArea),
            numberToCSV(r.somaFraction),
            numberToCSV(r.skeletonLength),
            numberToCSV(r.skeletonDensity),
            r.grPositive ? 1 : 0,
            r.associatedGRCount,
            numberToCSV(
                r.grFractionInsideIBA
            )
        ]
    )
}


// =====================================================================
// SUMMARY CSV
// =====================================================================

def summaryHeader = [

    "Image",
    "ROI_Area_mm2",
    "IBA_Detected_Before_Max_Filter",
    "IBA_Excluded_Above_Max_Area",
    "Microglia_Count",
    "Microglia_Density_cells_mm2",
    "GR_Count",
    "GR_Density_cells_mm2",
    "IBA_GR_Ratio",
    "IBA_GR_Count",
    "IBA_GR_pct_of_IBA",
    "IBA_Only_Count",
    "IBA_Only_pct_of_IBA",
    "GR_with_IBA_Count",
    "GR_with_IBA_pct_of_GR",
    "GR_Only_Count",
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
        csvEscape(imageName),
        numberToCSV(cortexAreaMm2),
        ibaDetectedBeforeMaxFilter,
        excludedLargeCount,
        ibaTotal,
        numberToCSV(ibaDensity),
        grTotal,
        numberToCSV(grDensity),
        numberToCSV(ibaGrRatio),
        ibaWithGR,
        numberToCSV(ibaWithGRPct),
        ibaOnly,
        numberToCSV(ibaOnlyPct),
        grWithIBA,
        numberToCSV(grWithIBAPct),
        grOnly,
        numberToCSV(grOnlyPct),
        numberToCSV(meanOf("area")),
        numberToCSV(meanOf("perimeter")),
        numberToCSV(meanOf("circularity")),
        numberToCSV(meanOf("convexHullArea")),
        numberToCSV(meanOf("convexHullPerimeter")),
        numberToCSV(meanOf("solidity")),
        numberToCSV(meanOf("roughness")),
        numberToCSV(meanOf("shapeComplexity")),
        numberToCSV(meanOf("somaArea")),
        numberToCSV(meanOf("somaFraction")),
        numberToCSV(meanOf("skeletonLength")),
        numberToCSV(meanOf("skeletonDensity"))
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
println "ANALYSIS FINISHED"
println "========================================================"

println "Image:"
println "  ${imageName}"

println ""

println "IBA:"
println "  Detected before max filter = ${ibaDetectedBeforeMaxFilter}"
println "  Excluded > ${IBA_MAX_AREA} um2 = ${excludedLargeCount}"
println "  Final = ${ibaTotal}"
println "  Density = ${ibaDensity} cells/mm2"

println ""

println "GR:"
println "  Count = ${grTotal}"
println "  Density = ${grDensity} cells/mm2"

println ""

println "IBA + GR:"
println "  ${ibaWithGR} (${ibaWithGRPct}%)"

println ""

println "IBA only:"
println "  ${ibaOnly} (${ibaOnlyPct}%)"

println ""

println "GR with IBA:"
println "  ${grWithIBA} (${grWithIBAPct}%)"

println ""

println "GR only:"
println "  ${grOnly} (${grOnlyPct}%)"

println ""

println "Mean IBA area:"
println "  ${meanOf('area')} um2"

println ""

println "========================================================"