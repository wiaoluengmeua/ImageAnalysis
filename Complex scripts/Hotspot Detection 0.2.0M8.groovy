// Script to find high density areas of classified cells in QuPath v0.2.0-m8. Version 4.
// Expected input: Classified cells, normally within some kind of annotation that does not have the same class as the cells.
// Expected output: No change to initial cells or classifications, but the addition of classified annotations around hotspots
// Downstream: Add further measurements to annotations based on the density and percentages of classified cells?
// Script by Mike Nelson, 1/15/2020.

double smooth = 7.0

import ij.plugin.filter.EDM
import ij.plugin.filter.RankFilters
import ij.process.Blitter
import ij.process.ByteProcessor
import ij.process.FloatProcessor
import qupath.imagej.processing.SimpleThresholding
import qupath.lib.objects.classes.PathClass
import qupath.lib.objects.classes.PathClassFactory
import qupath.lib.plugins.parameters.ParameterList
import qupath.imagej.processing.RoiLabeling;
import ij.gui.Wand;
import java.awt.Color
import ij.measure.Calibration;
import ij.IJ
import qupath.imagej.gui.IJExtension
import qupath.lib.regions.RegionRequest
import ij.ImagePlus;
import qupath.lib.regions.ImagePlane
import qupath.lib.objects.PathObjects
import ij.process.ImageProcessor;
import qupath.lib.objects.PathCellObject
import qupath.lib.roi.ShapeSimplifier

//Default values for dialog box, or values for running the script across a project.
int pixelDensity = 3
double radiusMicrons = 20.0
int minCells = 30
boolean smoothing = true
boolean showHeatMap = false
//Collect some information from the user to use in the hotspot detection

///////////////////////////////////////////////////////////////////
def params = new ParameterList()
        
        .addIntParameter("minCells", "Minimum cell count", minCells, "cells", "Minimum number of cells in hotspot")
        //.addDoubleParameter("pixelSizeMicrons", "Pixel size", pixelSizeMicrons, GeneralTools.micrometerSymbol(), "Choose downsampling-can break script on large images if not large enough")
        .addIntParameter("density", "Density", pixelDensity, "Changes with the other variables, requires testing", "Integer values: lower pixel size requires lower density")
        .addDoubleParameter("radiusMicrons", "Distance between cells", radiusMicrons, GeneralTools.micrometerSymbol(), "Usually roughly the distance between positive cell centroids")
        .addBooleanParameter("smoothing", "Smoothing?           ", smoothing, "Do you want smoothing")
        .addBooleanParameter("heatmap", "Show Heatmap?           ", showHeatMap, "Open a new window showing the heatmap. If ImageJ is already open, you can use that to look at pixel values")
if (!Dialogs.showParameterDialog("Parameters. WARNING, can be slow on large images with many clusters", params))
    return


radiusMicrons = params.getDoubleParameterValue("radiusMicrons")
minCells = params.getIntParameterValue("minCells")
pixelDensity = params.getIntParameterValue("density")
smoothing = params.getBooleanParameterValue("smoothing")
showHeatMap = params.getBooleanParameterValue("heatmap")

///////////////////////////////////////////////////////////////////

//Comment out the entire section above and put the values you want in manually if you want to run the script "For Project"


int z = 0
int t = 0
def plane = ImagePlane.getPlane(z, t)
def imageData = getCurrentImageData()
def server = imageData.getServer()
def cells = getCellObjects()

pixelCount = server.getWidth()*server.getHeight()
downsample = Math.ceil(pixelCount/(double)500000000)
pixelSizeMicrons = downsample*server.getPixelCalibration().getAveragedPixelSizeMicrons()

int w = Math.ceil(server.getWidth() / downsample)
int h = Math.ceil(server.getHeight() / downsample)
int nPixels = w * h
double radius = radiusMicrons / pixelSizeMicrons
//println("downsample " + downsample)
//println("radius " +radius)
//Unsure about this part. Maybe it shouldnt start at 0,0 but should get the upper left pixel using the imageserver?
Calibration calIJ = new Calibration();
calIJ.xOrigin = 0/downsample;
calIJ.yOrigin = 0/downsample;

//Find all classes
Set classSet = []
for (object in getCellObjects()) {
    classSet << object.getPathClass()
}
//convert classes into a list, which is ordered

/*************************************************
CLASS LIST MIGHT BE MODIFIABLE FOR MULTIPLEXING
*****************************************************/
List classList = []
classList.addAll(classSet.findAll{ 
    //If you only want one class, use it == getPathClass("MyClassHere") instead
    it != getPathClass("Negative") 
})

removeObjects(getAnnotationObjects().findAll{(classList.contains(it.getPathClass()))},true)

print("Class list: "+ classList)
println("This part may be QUITE SLOW, with no apparent sign that it is working. Please wait for the 'Done' message.")

// Create centroid map
/*****************************
Create an array of floatprocessors per class

***************************/
def fpList = []
for (aClass in classList){
    fpArray = new FloatProcessor(w,h) 
    fpList << fpArray
}
def fpNegative = new FloatProcessor(w, h)

//////////////////////// Update valid mask
//Checking for areas to ignore (outside of annotations, near borders
ByteProcessor bpValid
def annotations = getAnnotationObjects()
if (annotations) {
//making an image instead of a byteprocessor
    def imgMask = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY)
        //Not sure
    def g2d = imgMask.createGraphics()
        //scale the image down by the downsample
    g2d.scale(1.0/downsample, 1.0/downsample)
        //Whole image starts black, 0s, fill annotation with white, 255?
    g2d.setColor(Color.WHITE)
    for (annotation in annotations) {
        def shape = annotation.getROI().getShape()
        g2d.fill(shape)
    }
    g2d.dispose()
        //ok, I think at this point we have a large white block defining the annotation are
    bpValid = new ByteProcessor(imgMask)
    bpValid = SimpleThresholding.thresholdAbove(new EDM().makeFloatEDM(bpValid, 0, true), radius/4 as float)
        //Ok, now we have a distance transform from the edge of the annotation object...
    //Ahah! One that is thresholded so that we don't look for hotspots near the edge of something. Not sure if I will want this behavior or not
    
}
//clear out the original annotations to make it easier to cycle through all new annotations
removeObjects(annotations,true)


///////////////////////////////////////////
//Create cell count map
for (cell in cells) {
    def roi = PathObjectTools.getROI(cell, true)
    if (roi.isEmpty())
        continue
    def pathClass = cell.getPathClass()
    
    //Ignore unclassified cells
    if (pathClass == null )
        continue
    
    int x = (roi.getCentroidX() / downsample) as int
    int y = (roi.getCentroidY() / downsample) as int
    //find the pixel of the current roi center, and validate the position against the mask that checks for being too close to the border or outside of annotations
    int check = w*y+x
    
    //This is where the fpList[] starts to get information from individual classes
    //add 1 pixel value to the fpList equivalent to the class of the cell
    //After this, each fpList object should be an array that shows COUNTS for cells within an area determined by downsampling
    
    //Make sure we are writing to the correct position in fpList
    for (i=0; i<classList.size(); i++){
        if (pathClass == classList[i]){
            if (bpValid.getf(check) != 0f){
                fpList[i].setf(x, y, fpList[i].getf(x, y) + 1f as float)
            }
        }
    }

    if (PathClassTools.isNegativeClass(pathClass) && bpValid.getf(check) != 0f)
        fpNegative.setf(x, y, fpNegative.getf(x, y) + 1f as float)

}
//At this point we have cycled through all of the cells and built N heatmaps, though they are downsampled

////////////////////////////////////////////////////////////
// In this section we create a mean filter to run across our downsampled density map, using the radius given by the user.
// This, along with the downsample, will fill in the spaces between cells
def rf = new RankFilters()

//Get an odd diameter so that there is a center
int dim = Math.ceil(radius * 2 + 5)
def fpTemp = new FloatProcessor(dim, dim)
//generate an empty square (0s) with R^2 as the center pixel value
fpTemp.setf(dim/2 as int, dim/2 as int, radius * radius as float)
//spread the radius squared across a circle using the euclidean distance, radius
rf.rank(fpTemp, radius, RankFilters.MEAN)
def pixels = fpTemp.getPixels() as float[]
//count the number of pixels within fpTemp that will actually be used by RankFilters.Mean when passing "radius"
double n = Arrays.stream(pixels).filter({f -> f > 0}).count()


// Compute sum of elements
//rankfilter is used to run a mean filter across the fpTemp area

/*######## NEED TO MAKE THIS ONLY USE INTERESTING CLASSES ###########*/
fpList.each{
    rf.rank(it, radius, RankFilters.MEAN)
    it.multiply(n)
}


//Here we take the mean-filtered density maps, apply the user's density threshold, 
for (l=0; l<fpList.size(); l++){
    //create a mask based on the user threshold
    hotspotMaskMap = SimpleThresholding.thresholdAbove(fpList[l], (float)pixelDensity)
    //not 100% sure how this line worked, but it was necessary for the getFilledPolygonROIs to function
    hotspotMaskMap.setThreshold(1, ImageProcessor.NO_THRESHOLD, ImageProcessor.NO_LUT_UPDATE)
    //use the mask to generate ROIs that surround 4 connected points (not diagonals)
    hotspotROIs = RoiLabeling.getFilledPolygonROIs(hotspotMaskMap, Wand.FOUR_CONNECTED);
    
    //print(hotspotROIs.size())

    allqupathROIs = []
    qupathROIs = []
    //convert the ImageJ ROIs to QuPath ROIs
    hotspotROIs.each{allqupathROIs << IJTools.convertToROI(it, calIJ, downsample, plane)}
    //Use the QuPath ROIs to generate annotation objects (possibly smoothed), out of the heatmap ROIs
    objects = []
    qupathROIs = allqupathROIs.findAll{it.getArea() > (radiusMicrons*radiusMicrons/(server.getPixelCalibration().getAveragedPixelSizeMicrons()*server.getPixelCalibration().getAveragedPixelSizeMicrons()))}
    smoothedROIs = []
    qupathROIs.each{smoothedROIs << ShapeSimplifier.simplifyShape(it, downsample*2)}
    
    //println("sizes "+ qupathROIs.size)
    smoothedROIs.each{objects << PathObjects.createAnnotationObject(it, classList[l]);}
 
    addObjects(objects)

}

resolveHierarchy()

//remove small hotspots
getAnnotationObjects().each{
    currentClass = it.getPathClass()
    if (classList.contains(it.getPathClass())){
        count = []
        qupath.lib.objects.PathObjectTools.getDescendantObjects(it,count, PathCellObject)
        count = count.findAll{cell -> cell.getPathClass() == currentClass}
        if (count.size < minCells){
            //print count.size
            removeObject(it,true)
        }
    }
}
Set hotSpotClassList = []
for (object in getAnnotationObjects()) {
    hotSpotClassList << object.getPathClass()
}
IJExtension.getImageJInstance()
if (showHeatMap){
    for (l=0; l<fpList.size(); l++){
        if (hotSpotClassList.contains(classList[l])){
            new ImagePlus(classList[l].toString()+" heatmap at "+ pixelSizeMicrons+ "um pixel size", fpList[l]).show()
        }
    }
}


if(smoothing){
    
    before = getAnnotationObjects()
    selectAnnotations()
    runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": '+smooth+',  "lineCap": "Round",  "removeInterior": false,  "constrainToParent": false}');
    removeObjects(before,true)
    expanded = getAnnotationObjects()
    selectAnnotations()
    runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": '+(-1*smooth)+',  "lineCap": "Round",  "removeInterior": false,  "constrainToParent": false}');
    removeObjects(expanded,true)
    resetSelection();
    
}
    

//return the original annotations
addObjects(annotations)
resolveHierarchy()
getAnnotationObjects().each{it.setLocked(true)}

println("Done")