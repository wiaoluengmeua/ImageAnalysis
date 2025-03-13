//v2.0 - updated for 0.2.0m5
//This version strips out the user interface and most options, and replaces them with variables at the beginning of the script.
//I recommend using the UI version to figure out your settings, and this version to run as part of a workflow.
//Possibly place whole image annotation with another type
clearAllObjects()
createSelectAllObject(true);
def sigma = 1
def downsample = 1
def lowerThreshold = 0.3


//calculate bit depth for initially suggested upper threhsold, replace the value with the Math.pow line or maxPixel variable
//maxPixel = Math.pow((double) 2,(double)server.getPixelType().getBitsPerPixel())-1
def upperThreshold = 65535

double [] weights = [0,0,0,0,1,0]
def smallestAnnotations = 300
def fillHolesSmallerThan = 300

//option to remove artifacts that are way too large to be what you are interested in.
def removeLargerThan = 999999999999999999999999999

import qupath.lib.gui.QuPathGUI
import qupath.imagej.plugins.ImageJMacroRunner
import qupath.lib.plugins.parameters.ParameterList
import qupath.lib.roi.*
import qupath.lib.objects.*


def imageData = getCurrentImageData()
def server = imageData.getServer()
def metadata = getCurrentImageData().getServer().getOriginalMetadata()
def pixelSize = metadata.pixelCalibration.pixelWidth.value


//Place all of the final weights into an array that can be read into ImageJ
//Normalize weights so that sum =1
def sum = weights.sum()
if (sum<=0){
   print "Please use positive weights"
   return;
}
for (i=0; i<weights.size(); i++){
        weights[i] = weights[i]/sum
}
    
    //[1,2,3,4] format can't be read into ImageJ arrays (or at least I didn't see an easy way), it needs to be converted to 1,2,3,4
def weightList =weights.join(", ")
listOfNames = []
def channels = []
if (!imageData.getServer().isRGB()) {

    channels = getQuPath().getViewer().getImageDisplay().availableChannels()

    for (channel in channels) {
        //This bit necessary as of 0.2.0m4/m5 in order to target the channel images correctly.
        currentChannelName = channel.getName().toString()
        channelTag = currentChannelName.lastIndexOf(" (C")
        position = currentChannelName.size()-channelTag
        currentChannelName = currentChannelName.substring(0, currentChannelName.length()-position)

        listOfNames << currentChannelName
        //print(channel.toString())
    }
    
    
}
listOfNames = '"'+listOfNames.join('","')+'"'




    //Get rid of everything already in the image.  Not totally necessary, but useful when I am spamming various values.
    def annotations = getAnnotationObjects()

    def params = new ImageJMacroRunner(getQuPath()).getParameterList()

    // Change the value of a parameter, using the JSON to identify the key
    params.getParameters().get('downsampleFactor').setValue(downsample)
    params.getParameters().get('sendROI').setValue(false)
    params.getParameters().get('sendOverlay').setValue(false)
    params.getParameters().get('getOverlay').setValue(false)
    if (!getQuPath().getClass().getPackage()?.getImplementationVersion()){
        params.getParameters().get('getOverlayAs').setValue('Annotations')
    }
    params.getParameters().get('getROI').setValue(true)
    params.getParameters().get('clearObjects').setValue(false)

    // Get the macro text and other required variables
    def macro ='weights=newArray('+weightList+');names=newArray('+listOfNames+');'+
    'run("Stack to Images");'+
    'for (i=0; i<'+channels.size()+';i++){selectWindow(names[i]);'+
    'run("Multiply...", "value="+weights[i]);}'+
    'run("Images to Stack");'+
    'run("Z Project...", "projection=[Sum Slices]");'+
    'run("Gaussian Blur...", "sigma='+sigma+'");'+
    'setThreshold('+lowerThreshold+', '+upperThreshold+');run("Convert to Mask");'+
    'run("Create Selection");run("Colors...", "foreground=white background=black selection=white");'+
    'run("Properties...", "channels=1 slices=1 frames=1 unit=um pixel_width='+pixelSize+' pixel_height='+pixelSize+' voxel_depth=1");'

    def macroRGB = 'weights=newArray('+weightList+');'+
    'original = getImageID();run("Duplicate...", " ");'+
    'run("Make Composite");run("Stack to Images");'+
    'selectWindow("Red");rename("Red X3t4Y6lEt");run("Multiply...", "value="+weights[0]);'+
    'selectWindow("Green");rename("Green X3t4Y6lEt");run("Multiply...", "value="+weights[1]);'+
    'selectWindow("Blue");rename("Blue X3t4Y6lEt");run("Multiply...", "value="+weights[2]);'+
    'run("Images to Stack", "name=Stack title=[X3t4Y6lEt] use");'+
    'run("Z Project...", "projection=[Sum Slices]");'+
    'run("Gaussian Blur...", "sigma='+sigma+'");'+
    'setThreshold('+lowerThreshold+', '+upperThreshold+');run("Convert to Mask");'+
    'run("Create Selection");run("Colors...", "foreground=white background=black selection=cyan");'+
    'run("Properties...", "channels=1 slices=1 frames=1 unit=um pixel_width='+pixelSize+' pixel_height='+pixelSize+' voxel_depth=1");'+
    'selectImage(original);run("Restore Selection");'


    for (annotation in annotations) {
        //Check if we need to use the RGB version
        if (imageData.getServer().isRGB()) {
            ImageJMacroRunner.runMacro(params, imageData, null, annotation, macroRGB)
        } else{ ImageJMacroRunner.runMacro(params, imageData, null, annotation, macro)}
    }

    //remove whole image annotation and lock the new annotation
    removeObjects(annotations,true)
//Option to remove small sized annotation areas. Requires pixel size


//Clip button goes with the Remove Small button on the dialog, to remove objects below the text box amount in um^2
        def areaAnnotations = getAnnotationObjects().findAll {it.getROI() instanceof AreaROI}

        for (section in areaAnnotations){
            
            def polygons = RoiTools.splitAreaToPolygons(section.getROI())
            def newPolygons = polygons[1].collect {
                updated = it
                for (hole in polygons[0])
                    updated = RoiTools.combineROIs(updated, hole, RoiTools.CombineOp.SUBTRACT)
                return updated
            }
                    // Remove original annotation, add new ones
        annotations = newPolygons.collect {new PathAnnotationObject(it)}
        
        removeObject(section, true)
        addObjects(annotations)

            
        }




    //PART2

    def smallAnnotations = getAnnotationObjects().findAll {it.getROI().getScaledArea(pixelSize, pixelSize) < smallestAnnotations}
    println("small "+smallAnnotations)
    removeObjects(smallAnnotations, true)
    fireHierarchyUpdate()

    // Get selected objects
    // If you're willing to loop over all annotation objects, for example, then use getAnnotationObjects() instead
    def pathObjects = getAnnotationObjects()

    // Create a list of objects to remove, add their replacements
    def toRemove = []
    def toAdd = []
    for (pathObject in pathObjects) {
        def roi = pathObject.getROI()
        // AreaROIs are the only kind that might have holes
        if (roi instanceof AreaROI ) {
            // Extract exterior polygons
            def polygons = RoiTools.splitAreaToPolygons(roi)[1] as List
            // If we have multiple polygons, merge them
            def roiNew = polygons.remove(0)
            def roiNegative = RoiTools.splitAreaToPolygons(roi)[0] as List
            for (temp in polygons){
                roiNew = PRoiTools.combineROIs(temp, roiNew, RoiTools.CombineOp.ADD)
            }
            for (temp in roiNegative){  
                if (temp.getArea() > fillHolesSmallerThan/pixelSize/pixelSize){
                    roiNew = RoiTools.combineROIs(roiNew, temp, RoiTools.CombineOp.SUBTRACT)
                }
            }
            // Create a new annotation
            toAdd << new PathAnnotationObject(roiNew, pathObject.getPathClass())
            toRemove << pathObject
        }
    }

// Remove & add objects as required
def hierarchy = getCurrentHierarchy()
hierarchy.getSelectionModel().clearSelection()
hierarchy.removeObjects(toRemove, true)
hierarchy.addPathObjects(toAdd)

def largeAnnotations = getAnnotationObjects().findAll {it.getROI().getScaledArea(pixelSize, pixelSize) > removeLargerThan}
removeObjects(largeAnnotations, true)

getAnnotationObjects().each{it.setLocked(true)}


//uncomment to merge final results into single line in annotations table
//selectAnnotations()
//mergeSelectedAnnotations()
println("Annotation areas completed")