import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane

print 'Image_Process_started!'
createAnnotationsFromPixelClassifier("raissa_tissue", 0.0, 0.0)
int z = 0
int t = 0
def plane = ImagePlane.getPlane(z, t)
def roi = ROIs.createRectangleROI(35000, 20000, 1000, 1000, plane)
def annotation = PathObjects.createAnnotationObject(roi)
addObject(annotation)
selectAnnotations();
runPlugin('qupath.lib.algorithms.TilerPlugin', '{"tileSizeMicrons":320.0,"trimToROI":false,"makeAnnotations":true,"removeParentAnnotation":true}')


//Write out each region corresponding to an unclassified annotation
//Only use this if you have created SMALL unclassified annotations!!

def imageName = GeneralTools.getNameWithoutExtension(getCurrentImageData().getServer().getMetadata().getName())
//Make sure the location you want to save the files to exists - requires a Project
def pathOutput = buildFilePath(PROJECT_BASE_DIR, 'image_export', imageName)
mkdirs(pathOutput)

unclassifiedAnnotations = getAnnotationObjects()

unclassifiedAnnotations.eachWithIndex{anno,x->
    //Name of the file and the path to where it goes in the Project
    fileName = pathOutput+"//"+imageName+ "region "+x+".tif"
    //For each annotation, we get its outline
    def roi1 = anno.getROI()
    //For each outline, we request the pixels within the bounding box of the annotation
    def requestROI = RegionRequest.createInstance(getCurrentServer().getPath(), 1, roi1)
    //The 1 in the function above is the downsample, increase it for smaller images
    writeImageRegion(getCurrentServer(), requestROI, fileName)
}

print 'Done!'