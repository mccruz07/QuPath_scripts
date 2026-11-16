import qupath.lib.objects.PathAnnotationObject
import qupath.lib.roi.RectangleROI
import qupath.lib.scripting.QP

// PARAMETERS - edit these to match your needs
double tileAreaMicrons2     = 62680.4666   // area of one tile in um2
int    tilesWide            = 3            // number of tiles across (width)
int    tilesHigh            = 6            // number of tiles down (height)
int    nSamples             = 10           // number of random rectangles to place
long   seed                 = 42           // fixed seed for reproducibility
String tissueAnnotationName = "Tissue"     // set to null to use full image

// SETUP
def imageData = QP.getCurrentImageData()
def server    = imageData.getServer()
def hierarchy = imageData.getHierarchy()
def cal       = server.getPixelCalibration()

double pxW = cal.getPixelWidthMicrons()
double pxH = cal.getPixelHeightMicrons()

if (pxW <= 0 || pxH <= 0) {
    print "ERROR: Pixel calibration not set. Go to Image > Set pixel size and re-run."
    return
}

// CALCULATE TILE SIDE LENGTH IN um THEN CONVERT TO PIXELS
double tileSideMicrons = Math.sqrt(tileAreaMicrons2)
double rectWidthMicrons  = tilesWide * tileSideMicrons
double rectHeightMicrons = tilesHigh * tileSideMicrons

int rectWidthPx  = (int) Math.round(rectWidthMicrons / pxW)
int rectHeightPx = (int) Math.round(rectHeightMicrons / pxH)

double actualAreaUm = rectWidthPx * pxW * rectHeightPx * pxH

print "Pixel calibration  : " + pxW + " um/px (W) x " + pxH + " um/px (H)"
print "Tile side          : " + tileSideMicrons + " um"
print "Rectangle (um)     : " + rectWidthMicrons + " um wide x " + rectHeightMicrons + " um tall"
print "Rectangle (px)     : " + rectWidthPx + " x " + rectHeightPx + " px"
print "Tile grid          : " + tilesWide + " wide x " + tilesHigh + " tall = " + (tilesWide * tilesHigh) + " tiles"
print "Total area (target): " + (tileAreaMicrons2 * tilesWide * tilesHigh) + " um2"
print "Total area (actual): " + actualAreaUm + " um2"

// DEFINE SAMPLING BOUNDS
def tissueAnnotation = null
def useFullImage     = true
int boundX = 0
int boundY = 0
int boundW = server.getWidth()
int boundH = server.getHeight()
def tissueGeometry = null

if (tissueAnnotationName != null) {
    tissueAnnotation = hierarchy.getAnnotationObjects().find {
        it.getName() == tissueAnnotationName
    }
    if (tissueAnnotation == null) {
        print "WARNING: No annotation named '" + tissueAnnotationName + "' found - sampling from full image instead."
    } else {
        def env        = tissueAnnotation.getROI().getGeometry().getEnvelopeInternal()
        boundX         = (int) env.getMinX()
        boundY         = (int) env.getMinY()
        boundW         = (int) env.getWidth()
        boundH         = (int) env.getHeight()
        tissueGeometry = tissueAnnotation.getROI().getGeometry()
        useFullImage   = false
        print "Sampling constrained to annotation: " + tissueAnnotationName
    }
}

if (useFullImage) {
    print "Sampling from full image bounds: " + boundW + " x " + boundH + " px"
}

if (rectWidthPx >= boundW || rectHeightPx >= boundH) {
    print "ERROR: Rectangle (" + rectWidthPx + " x " + rectHeightPx + " px) is larger than the sampling area (" + boundW + " x " + boundH + " px)."
    print "Check your pixel calibration or reduce the tile count."
    return
}

// PLACE RANDOM RECTANGLES
def rng         = new Random(seed)
def annotations = []
int attempts    = 0
int maxAttempts = nSamples * 100

while (annotations.size() < nSamples && attempts < maxAttempts) {
    attempts++

    int x = boundX + rng.nextInt(boundW - rectWidthPx)
    int y = boundY + rng.nextInt(boundH - rectHeightPx)

    def roi = new RectangleROI(x, y, rectWidthPx, rectHeightPx)

    if (!useFullImage && tissueGeometry != null) {
        if (!tissueGeometry.contains(roi.getGeometry())) {
            continue
        }
    }

    def annotation = new PathAnnotationObject(roi)
    annotation.setName("Sample_" + (annotations.size() + 1))
    annotations.add(annotation)
}

// ADD TO IMAGE AND REPORT
hierarchy.addObjects(annotations)
hierarchy.fireHierarchyChangedEvent(null)

if (annotations.size() < nSamples) {
    print "WARNING: Only placed " + annotations.size() + " / " + nSamples + " rectangles after " + attempts + " attempts."
    print "Try reducing nSamples or relaxing the tissue constraint."
} else {
    print "Done - placed " + annotations.size() + " rectangles (" + tilesWide + "x" + tilesHigh + " tiles each)."
}