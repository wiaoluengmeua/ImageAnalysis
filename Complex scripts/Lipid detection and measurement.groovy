//Not well commented, but the overall purpose of this script is to
//1. detect tissue in a brightfield image
//2. send the tissue to ImageJ 
//2.5 you may need to cut up your tissue if the image is too large using something like
//runPlugin('qupath.lib.algorithms.TilerPlugin', '{"tileSizePx": 10000.0,  "trimToROI": true,  "makeAnnotations": true,  "removeParentAnnotation": true}');
//at which point you will also need to place selectAnnotations() mergeSelectedAnnotations() before doing the calculations at the end
//3. Using an ImageJ macro (with all comments and newline characters removed) threshold and find all empty spots
//The values for this will depend greatly on image quality, background, and brightness.  You may also want to adjust size thresholds for the Analyze Particles... command
//4. Once the detection objects are returned, sum up their areas, and divide by the total parent annotation area to find a percentage lipid area
// Since all of the detections exist as objects, you could also perform other analyses of them by adding circularity measurements etc. (Calculate Features/Add Shape Features)

import qupath.imagej.plugins.ImageJMacroRunner
import qupath.lib.plugins.parameters.ParameterList

runPlugin('qupath.imagej.detect.tissue.SimpleTissueDetection2', '{"threshold": 161,  "requestedDownsample": 1.0,  "minAreaPixels": 1.0E7,  "maxHoleAreaPixels": 25500.0,  "darkBackground": false,  "smoothImage": true,  "medianCleanup": true,  "dilateBoundaries": false,  "smoothCoordinates": true,  "excludeOnBoundary": false,  "singleAnnotation": true}');

// Create a macro runner so we can check what the parameter list contains
def params = new ImageJMacroRunner(getQuPath()).getParameterList()
print ParameterList.getParameterListJSON(params, ' ')

// Change the value of a parameter, using the JSON to identify the key
params.getParameters().get('downsampleFactor').setValue(1.0 as double)
params.getParameters().get('getOverlay').setValue(true)
params.getParameters().get('clearObjects').setValue(true)
print ParameterList.getParameterListJSON(params, ' ')

// Get the macro text and other required variables
def macro = 'min=newArray(3);max=newArray(3);filter=newArray(3);a=getTitle();run("HSB Stack");run("Convert Stack to Images");selectWindow("Hue");rename("0");selectWindow("Saturation");rename("1");selectWindow("Brightness");rename("2");min[0]=9;max[0]=255;filter[0]="pass";min[1]=0;max[1]=45;filter[1]="pass";min[2]=136;max[2]=255;filter[2]="pass";for (i=0;i<3;i++){  selectWindow(""+i);  setThreshold(min[i], max[i]);  run("Convert to Mask");  if (filter[i]=="stop")  run("Invert");}imageCalculator("AND create", "0","1");imageCalculator("AND create", "Result of 0","2");for (i=0;i<3;i++){  selectWindow(""+i);  close();}selectWindow("Result of 0");close();selectWindow("Result of Result of 0");rename(a);run("Smooth");run("Smooth");run("Smooth");run("Smooth");run("Smooth");run("Smooth");run("Smooth");run("Make Binary");run("Despeckle");run("Despeckle");run("Analyze Particles...", "size=200-19500 show=[Overlay Masks] add");'
def imageData = getCurrentImageData()

//***********************************************************************************************
//YOU MAY NEED TO CREATE TILE ANNOTATIONS HERE DEPENDING ON THE SIZE OF YOUR IMAGE + REMOVE PARENT
//I recommend the largest tiles you can possibly get away with since they will disrupt adipocyte detection
//*******************************************************************************************

def annotations = getAnnotationObjects()

// Loop through the annotations and run the macro
for (annotation in annotations) {
    ImageJMacroRunner.runMacro(params, imageData, null, annotation, macro)
}

//selectAnnotations()+mergeSelectedObjects() here if needed.

selected = getAnnotationObjects()
removeObject(selected[0], true)
addObject(selected[0])
selected[0].setLocked(true)
selectObjects{p -> (p.getLevel()==1) && (p.isAnnotation() == false)};
clearSelectedObjects(false);


import qupath.lib.objects.PathDetectionObject

hierarchy = getCurrentHierarchy()

for (annotation in getAnnotationObjects()){
    //Block 1
    def tiles = hierarchy.getDescendantObjects(annotation,null, PathDetectionObject)
    double totalArea = 0
    for (def tile in tiles){
        totalArea += tile.getROI().getArea()
    }


    annotation.getMeasurementList().putMeasurement("Marked area px", totalArea)

    def annotationArea = annotation.getROI().getArea()
    annotation.getMeasurementList().putMeasurement("Marked area %", totalArea/annotationArea*100)

}

print 'Done!'