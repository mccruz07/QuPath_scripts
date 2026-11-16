createAnnotationsFromPixelClassifier("raissa_tissue", 0.0, 0.0)
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane
//setup some classes that will be used later and selecting the main annotation that the random rectangles will be in
def annoList = getAnnotationObjects()
def anno = annoList.get(0)
def tissue = anno.getROI()
def tissueClass = getPathClass('Tissue')
def roiClass = getPathClass('ROI')
// I needed to analyze 20% of the area from randomly selected ROIs, this part is to calculate how many 2048x2048 rectangles will cover 20% of the annotation
def tissue_area = tissue.getArea() //this number is in pixels^2
double twenty_percent = tissue_area*0.2
double ROI_area = 2048*2048
double num_ROIs = Math.round(twenty_percent/ROI_area)

anno.each{it.setPathClass(tissueClass)} //assigned a classification to the main annotation so it is not included in future steps that are not shown in this code snippet
println num_ROIs
def random_ROIs = [] //empty list to store in the random rectangles

//basic for loop that creates the number of random rectangles calculated above
for (i in 1..num_ROIs) {
    randomROI = RoiTools.createRandomRectangle(tissue, 2048, 2048)
    randomAnnot = PathObjects.createAnnotationObject(randomROI)
    random_ROIs.add(randomAnnot)
    tissue = RoiTools.difference(tissue, randomROI) // this step is essentially punching holes into the original ROI and then is updating the tissue annotation being used to place the next ROI at the top of the loop. It keeps the newly added ROIs from overlapping eachother.
    //println tissue.getArea() //used this line when testing to verify that the main annotation was actually decreasing in size
}
// Add in the new ROIs and give them a class for easier selection should you want to do something with them
addObjects(random_ROIs)
def ROIs = getAnnotationObjects().findAll{it.getPathClass() == null}
ROIs.each{it.setPathClass(roiClass)}

def imageName = GeneralTools.getNameWithoutExtension(getCurrentImageData().getServer().getMetadata().getName())
//Make sure the location you want to save the files to exists - requires a Project
def pathOutput = buildFilePath(PROJECT_BASE_DIR, 'image_export', imageName)
mkdirs(pathOutput)

unclassifiedAnnotations = getAnnotationObjects()

unclassifiedAnnotations.eachWithIndex{unclassifiedAnnotations,x->
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