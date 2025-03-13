//Ideally checks one channel for presence above a certain background level, to help remove islets or areas of interest in areas of bad staining
//Could be modified to check for stain bubbles, edge staining artifacts, multiple channels, etc.
//RESETS ANY CLASSIFICATIONS ALREADY SET. Would require substantial revision to avoid reclassifying annotations.

//expansion distance in microns around the annotations that is checked for background, this is also a STRING
def expansion = "20.0"
def threshold = 5000
//channel variable is part of a String and needs to be exactly correct
def channel = "Channel 2"


import qupath.lib.roi.*
import qupath.lib.objects.*
   
def pixelSize = getCurrentImageData().getServer().getPixelHeightMicrons()
hierarchy = getCurrentHierarchy()
originals = getAnnotationObjects()
classToSubtract = "Original"
surroundingClass = "Surrounding"
areaClass = "Donut"

//set the class on all of the base objects, lots of objects will be created and this helps keep track.
originals.each{it.setPathClass(getPathClass(surroundingClass))}
selectAnnotations()
runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": '+expansion+',  "removeInterior": false,  "constrainToParent": true}');
originals.each{it.setPathClass(getPathClass(classToSubtract))}
surroundings = getAnnotationObjects().findAll{it.getPathClass() == getPathClass(surroundingClass)}
fireHierarchyUpdate()
   
for (parent in surroundings){
    //child object should be of the original annotations, now with classToSubtract
    child = parent.getChildObjects()
    updated = PathROIToolsAwt.combineROIs(parent.getROI(), child[0].getROI(), PathROIToolsAwt.CombineOp.SUBTRACT)
     // Remove original annotation, add new ones
    annotations = new PathAnnotationObject(updated, getPathClass(areaClass))

    addObject(annotations)
    selectAnnotations().findAll{it.getPathClass() == getPathClass(areaClass)}
  
    ///////////MAY NEED TO MANUALLY EDIT THIS LINE and "value" below A BIT BASED ON IMAGE///////////////////  
    runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": '+pixelSize+',  "region": "ROI",  "tileSizeMicrons": 25.0,  "channel1": false,  "channel2": true,  "channel3": false,  "channel4": false,  "doMean": true,  "doStdDev": true,  "doMinMax": false,  "doMedian": false,  "doHaralick": false,  "haralickMin": 0,  "haralickMax": 0,  "haralickDistance": 1,  "haralickBins": 32}');
    donut = getAnnotationObjects().findAll{it.getPathClass()==getPathClass(areaClass)}
    fireHierarchyUpdate()
    value = donut[0].getMeasurementList().getMeasurementValue("ROI: 0.32 " + qupath.lib.common.GeneralTools.micrometerSymbol() + " per pixel: "+channel+":  Mean")

 
    //occasionally the value is NaN for no reason I can figure out. I decided it was safer to keep the results any time
    //this happens for now, though if the preserved regions end up being problematic the && !value.isNaN should be removed.
    if ( value > threshold && !value.isNaN()){
        println("remove, value was "+value)
        removeObject(parent, false)
        removeObject(donut[0], true)
    } else {println("keep");
        removeObject(parent, true);
        removeObject(donut[0],true)
    }

}
    fireHierarchyUpdate()


