import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane

// ======================================================
// CONFIGURAÇÕES
// ======================================================
int roiSize = 200
int nROIs = 5

// ======================================================
// OBTER DIMENSÕES DA IMAGEM
// ======================================================
def imageData = getCurrentImageData()
def server = imageData.getServer()

double imgWidth = server.getWidth()
double imgHeight = server.getHeight()

// ======================================================
// REMOVER ANNOTATIONS ANTIGAS (opcional)
// ======================================================
removeObjects(getAnnotationObjects(), false)

// ======================================================
// CALCULAR POSIÇÕES
// Distribui 5 ROIs horizontalmente no centro
// ======================================================
double spacing = (imgWidth - (nROIs * roiSize)) / (nROIs + 1)
double y = (imgHeight - roiSize) / 2

for (int i = 0; i < nROIs; i++) {

    double x = spacing + i * (roiSize + spacing)

    // Garantir que não saia da imagem
    x = Math.max(0, Math.min(x, imgWidth - roiSize))
    y = Math.max(0, Math.min(y, imgHeight - roiSize))

    // Criar ROI
    def roi = ROIs.createRectangleROI(
        x,
        y,
        roiSize,
        roiSize,
        ImagePlane.getDefaultPlane()
    )

    // Criar annotation
    def annotation = PathObjects.createAnnotationObject(roi)

    // Nome opcional
    annotation.setName("ROI_${i+1}")

    addObject(annotation)
}

println "${nROIs} annotations de ${roiSize}x${roiSize}px criadas."