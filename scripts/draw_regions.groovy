/*
 * ================================================================
 * COPY CORTEX ANNOTATION TO ALL PROJECT IMAGES
 * QuPath 0.7
 * ================================================================
 *
 * Uso:
 *
 * 1. Abra a imagem de referência.
 * 2. Desenhe a ROI cortical POR ÚLTIMO.
 * 3. Execute este script.
 *
 * O script:
 *
 *   - usa a última annotation encontrada como template;
 *   - nomeia essa annotation como "Cortex";
 *   - atribui a classe "Cortex";
 *   - copia a geometria para todas as outras imagens;
 *   - nomeia e classifica todas as cópias como "Cortex".
 *
 * ================================================================
 */

import qupath.lib.objects.PathObjects
import qupath.lib.roi.GeometryTools
import qupath.lib.regions.ImagePlane


// ================================================================
// CONFIGURAÇÕES
// ================================================================

String ROI_NAME  = "Cortex"
String ROI_CLASS = "Cortex"

/*
 * true:
 * se uma imagem já tiver uma annotation chamada Cortex,
 * ela não será duplicada.
 */
boolean SKIP_IF_EXISTS = true


// ================================================================
// PROJETO
// ================================================================

def project = getProject()

if (project == null) {

    throw new IllegalStateException(
        "Abra um projeto QuPath antes de executar este script."
    )
}


// ================================================================
// IMAGEM ATUAL
// ================================================================

def sourceImageData =
    getCurrentImageData()

def sourceHierarchy =
    sourceImageData.getHierarchy()

def sourceEntry =
    getProjectEntry()

def sourceServer =
    getCurrentServer()


if (sourceEntry == null) {

    throw new IllegalStateException(
        "A imagem atual precisa fazer parte do projeto."
    )
}


// ================================================================
// PEGAR ANNOTATIONS DA IMAGEM ATUAL
// ================================================================

def annotations =
    sourceHierarchy.getAnnotationObjects()


if (annotations == null || annotations.isEmpty()) {

    throw new IllegalStateException(
        "Nenhuma annotation encontrada na imagem atual.\n\n" +
        "Desenhe primeiro a ROI cortical."
    )
}


// ================================================================
// USAR A ÚLTIMA ANNOTATION COMO TEMPLATE
// ================================================================

def sourceObject =
    annotations.last()


def sourceROI =
    sourceObject.getROI()


if (
    sourceROI == null ||
    !sourceROI.isArea()
) {

    throw new IllegalStateException(
        "A última annotation encontrada não é uma ROI de área."
    )
}


// ================================================================
// NOMEAR E CLASSIFICAR A ROI ORIGINAL
// ================================================================

sourceObject.setName(
    ROI_NAME
)

sourceObject.setPathClass(
    getPathClass(
        ROI_CLASS
    )
)


// ================================================================
// GEOMETRIA ORIGINAL
// ================================================================

def sourceGeometry =
    GeometryTools.roiToGeometry(
        sourceROI
    )


if (
    sourceGeometry == null ||
    sourceGeometry.isEmpty()
) {

    throw new IllegalStateException(
        "Não foi possível obter a geometria da ROI."
    )
}


// ================================================================
// INFORMAÇÕES DA IMAGEM DE REFERÊNCIA
// ================================================================

String sourceImageName =
    sourceEntry.getImageName()


int sourceWidth =
    sourceServer.getWidth()


int sourceHeight =
    sourceServer.getHeight()


def cal =
    sourceServer.getPixelCalibration()


double areaPx2 =
    sourceGeometry.getArea()


double areaUm2 =
    Double.NaN


if (cal.hasPixelSizeMicrons()) {

    areaUm2 =

        areaPx2 *

        cal.getPixelWidthMicrons() *

        cal.getPixelHeightMicrons()
}


// ================================================================
// RELATÓRIO INICIAL
// ================================================================

println ""
println "================================================"
println "CORTEX ROI TEMPLATE"
println "================================================"

println "Reference image:"
println "  ${sourceImageName}"

println ""

println "Annotation name:"
println "  ${ROI_NAME}"

println ""

println "Annotation class:"
println "  ${ROI_CLASS}"

println ""

println "Area:"
println "  ${areaPx2} px^2"


if (!Double.isNaN(areaUm2)) {

    println "  ${areaUm2} um^2"

    println "  ${areaUm2 / 1_000_000.0} mm^2"
}


println ""

println "Image dimensions:"
println "  ${sourceWidth} x ${sourceHeight}"

println ""

println "Number of annotations in source image:"
println "  ${annotations.size()}"

println "================================================"
println ""


// ================================================================
// SALVAR IMAGEM ORIGINAL
// ================================================================

sourceEntry.saveImageData(
    sourceImageData
)


// ================================================================
// CONTADORES
// ================================================================

int copied = 0
int skipped = 0
int failed = 0


// ================================================================
// COPIAR PARA TODAS AS IMAGENS
// ================================================================

for (def entry : project.getImageList()) {


    String targetName =
        entry.getImageName()


    println ""
    println "------------------------------------------------"
    println "Processing:"
    println "  ${targetName}"


    // ============================================================
    // IMAGEM ORIGINAL
    // ============================================================

    if (entry == sourceEntry) {

        println "Reference image -> Cortex ROI retained."

        skipped++

        continue
    }


    try {


        // ========================================================
        // CARREGAR IMAGE DATA
        // ========================================================

        def targetImageData =
            entry.readImageData()


        def targetHierarchy =
            targetImageData.getHierarchy()


        def targetServer =
            targetImageData.getServer()


        // ========================================================
        // VERIFICAR SE CORTEX JÁ EXISTE
        // ========================================================

        if (SKIP_IF_EXISTS) {


            def existing =

                targetHierarchy
                    .getAnnotationObjects()
                    .find {

                        it.getName() == ROI_NAME ||

                        it.getPathClass()?.toString() == ROI_CLASS

                    }


            if (existing != null) {

                println "Cortex annotation already exists -> skipped."

                skipped++

                continue
            }
        }


        // ========================================================
        // DIMENSÕES
        // ========================================================

        int targetWidth =
            targetServer.getWidth()


        int targetHeight =
            targetServer.getHeight()


        println "Target dimensions:"
        println "  ${targetWidth} x ${targetHeight}"


        if (
            targetWidth != sourceWidth ||
            targetHeight != sourceHeight
        ) {

            println ""
            println "WARNING:"
            println "Target image dimensions differ from source."
            println "The Cortex ROI will use the same pixel coordinates."
            println ""
        }


        // ========================================================
        // COPIAR GEOMETRIA
        // ========================================================

        def copiedGeometry =
            sourceGeometry.copy()


        // ========================================================
        // GEOMETRY -> ROI
        // ========================================================

        def targetROI =

            GeometryTools.geometryToROI(

                copiedGeometry,

                ImagePlane.getDefaultPlane()

            )


        if (
            targetROI == null ||
            !targetROI.isArea()
        ) {

            throw new IllegalStateException(
                "Could not recreate Cortex ROI."
            )
        }


        // ========================================================
        // CRIAR NOVA ANNOTATION
        // ========================================================

        def newAnnotation =

            PathObjects.createAnnotationObject(

                targetROI,

                getPathClass(
                    ROI_CLASS
                )

            )


        // ========================================================
        // NOMEAR
        // ========================================================

        newAnnotation.setName(
            ROI_NAME
        )


        // ========================================================
        // GARANTIR CLASSE
        // ========================================================

        newAnnotation.setPathClass(
            getPathClass(
                ROI_CLASS
            )
        )


        // ========================================================
        // ADICIONAR À HIERARQUIA
        // ========================================================

        targetHierarchy.addObject(
            newAnnotation
        )


        targetHierarchy.resolveHierarchy()


        // ========================================================
        // SALVAR
        // ========================================================

        entry.saveImageData(
            targetImageData
        )


        copied++


        println "Cortex ROI copied successfully."


    } catch (Exception e) {


        failed++


        println ""
        println "ERROR in ${targetName}:"
        println "  ${e.getMessage()}"

        e.printStackTrace()

    }

}


// ================================================================
// ATUALIZAR INTERFACE
// ================================================================

fireHierarchyUpdate()


// ================================================================
// FINAL
// ================================================================

println ""
println "================================================"
println "FINISHED"
println "================================================"

println "Template image:"
println "  ${sourceImageName}"

println ""

println "ROI name:"
println "  Cortex"

println ""

println "ROI class:"
println "  Cortex"

println ""

println "Copied:"
println "  ${copied}"

println ""

println "Skipped:"
println "  ${skipped}"

println ""

println "Failed:"
println "  ${failed}"

println "================================================"