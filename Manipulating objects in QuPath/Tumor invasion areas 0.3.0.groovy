/**
0.2.0? 0.3.0 yes
 * Script to help with annotating tumor regions, chopping increasing chunks into the tumor.
 * SEE THREAD HERE FOR DESCRIPTION ON USE: https://forum.image.sc/t/reduce-annotations/24305/12?u=research_associate
 * Here, each of the margin regions is approximately 100 microns in width.
Recommended BEFORE running the script if your tumor is on the tissue border:
    1. Create a tissue annotation for boundaries (class tissue, eliminate whitespace)
    2. Create a tumor annotation
    3. Run the script, it should only take into account tumor areas inside of the tissue annotation.
 * @author Pete Bankhead
 * @mangled by Svidro because reasons
 * mix and match with @Mike_Nelson version 
 */

//-----
// Some things you might want to change

// How much to expand each region
double expandMarginMicrons = 20.0
// How many times you want to chop into your annotation. Edit color script around line 115 if you go over 5
int howManyTimes = 4
// Define the colors
// Inner layers are given scripted colors, but gretaer than 6 or 7 layers may require adjustments
def colorOuterMargin = getColorRGB(0, 200, 0)
PrecisionModel PM = new PrecisionModel(PrecisionModel.FIXED)
// Choose whether to lock the annotations or not (it's generally a good idea to avoid accidentally moving them)
def lockAnnotations = true


// Extract the main info we need
def imageData = getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def server = imageData.getServer()
// We need the pixel size
def cal = server.getPixelCalibration()
if (!cal.hasPixelSizeMicrons()) {
  print 'We need the pixel size information here!'
  return
}



//-----
//Setup - Merge all Tumor objects into one, they can be split later. Get Geometries for each object
selectObjectsByClassification("Tumor")
mergeSelectedAnnotations()
double expandPixels = expandMarginMicrons / cal.getAveragedPixelSizeMicrons()
initialTumorObject = getAnnotationObjects().find{it.getPathClass() == getPathClass("Tumor")}
def tumorGeom = getAnnotationObjects().find{it.getPathClass() == getPathClass("Tumor")}.getROI().getGeometry()
def plane = ImagePlane.getDefaultPlane()
def tissueGeom = getAnnotationObjects().find{it.getPathClass() == getPathClass("Tissue")}.getROI().getGeometry()

//Clean up the Tumor geometry
cleanTumorGeom = tissueGeom.intersection(tumorGeom)
tumorROIClean = GeometryTools.geometryToROI(cleanTumorGeom, plane)


cleanTumor = PathObjects.createAnnotationObject(tumorROIClean, getPathClass("Tumor"))
cleanTumor.setName("CleanTumor")



// Get the outer margin area
def geomOuter = cleanTumorGeom.buffer(expandPixels)
geomOuter = geomOuter.difference(cleanTumorGeom)
geomOuter = geomOuter.intersection(tissueGeom)
def roiOuter = GeometryTools.geometryToROI(geomOuter, plane)
def annotationOuter = PathObjects.createAnnotationObject(roiOuter)
annotationOuter.setName("Outer margin")
annotationOuter.setColorRGB(colorOuterMargin)

innerAnnotations = []
innerAnnotations << cleanTumor
innerAnnotations << annotationOuter
//innerAnnotations << selected

for (i=0; i<howManyTimes;i++){


    currentArea = innerAnnotations[innerAnnotations.size()-1].getROI().getGeometry()

    areaExpansion = currentArea.buffer(expandPixels)
    
    areaExpansion = areaExpansion.intersection(cleanTumorGeom)
    areaExpansion = areaExpansion.buffer(0)
    //println(areaExpansion)
    areaExpansion = areaExpansion.intersection(tissueGeom)
    //println(areaExpansion)
    //remove outer areas previously defined as other innerAnnotations
    if(i>=1){
        for (k=1; k<=i;k++){
            remove = innerAnnotations[innerAnnotations.size()-k].getROI().getGeometry()
            areaExpansion = areaExpansion.difference(remove)
            
        }
    }
    areaExpansion= GeometryPrecisionReducer.reduce(areaExpansion, PM)
    roiExpansion = GeometryTools.geometryToROI(areaExpansion, plane)
    j = i+1
    annotationExpansion = PathObjects.createAnnotationObject(roiExpansion)
    int nameValue = j*expandMarginMicrons
    annotationExpansion.setName("Inner margin "+nameValue+" microns")
    annotationExpansion.setColorRGB(getColorRGB(20*i, 40*i, 200-30*i))
    innerAnnotations << annotationExpansion
    //println("innerannotations size "+innerAnnotations.size())
}
//add one last inner annotation that contains the rest of the tumor
core = cleanTumorGeom

for (i=1; i<=howManyTimes;i++){
    core = core.difference(innerAnnotations[i].getROI().getGeometry())
}
coreROI = GeometryTools.geometryToROI(core, plane)

coreAnno = PathObjects.createAnnotationObject(coreROI)
coreAnno.setName("Remaining Tumor")
print "core geom: " +coreAnno.getClass()
innerAnnotations << coreAnno

print innerAnnotations

// Add the annotations
//hierarchy.removeObject(selected, true)
getAnnotationObjects().each {it.setLocked(lockAnnotations)}

addObjects(innerAnnotations)
removeObject(initialTumorObject, true)

println("Done! Wheeeee!")


import org.locationtech.jts.geom.Geometry
import qupath.lib.common.GeneralTools
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjects
import qupath.lib.roi.GeometryTools
import qupath.lib.roi.ROIs

import java.awt.Rectangle
import java.awt.geom.Area
import org.locationtech.jts.precision.GeometryPrecisionReducer
import org.locationtech.jts.geom.PrecisionModel
