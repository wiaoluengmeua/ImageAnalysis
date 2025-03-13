/**
0.2.0
 * Script to help with annotating tumor regions, chopping increasing chunks into the tumor.
 * SEE THREAD HERE FOR DESCRIPTION ON USE: https://forum.image.sc/t/reduce-annotations/24305/12?u=research_associate
 * Here, each of the margin regions is approximately 100 microns in width.
Recommended BEFORE running the script if your tumor is on the tissue border:
    1. Do have a simple tissue detection made to limit the borders.
    2. Make an inverse area, delete the original tissue, and use CTRL+SHIFT BRUSH TOOL mostly to create the border of your tumor touching the simple tissue detection edge. You can use the wand tool for the interior of the tumor, but any little pixel that is missed by the wand tool defining the whitespace border of the tumor will result in an expansion. Brush tool them all away.
    3. Invert the inverted tissue detection to get the original tissue back, and then delete the inverted annotation. You now have a tumor annotation that is right up against the tissue border.
    4. Select the tumor and run the script
 * @author Pete Bankhead
 * @mangled by Svidro because reasons
 */

//-----
// Some things you might want to change

// How much to expand each region
double expandMarginMicrons = 20.0
// How many times you want to chop into your annotation. Edit color script around line 115 if you go over 5
int howManyTimes = 2
// Define the colors
// Inner layers are given scripted colors, but gretaer than 6 or 7 layers may require adjustments
def colorOuterMargin = getColorRGB(0, 200, 0)


import org.locationtech.jts.geom.Geometry
import qupath.lib.common.GeneralTools
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjects
import qupath.lib.roi.GeometryTools
import qupath.lib.roi.ROIs

import java.awt.Rectangle
import java.awt.geom.Area


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


// Choose whether to lock the annotations or not (it's generally a good idea to avoid accidentally moving them)
def lockAnnotations = true

//-----




if (!GeneralTools.almostTheSame(cal.getPixelWidthMicrons(), cal.getPixelHeightMicrons(), 0.0001)) {
    print 'Warning! The pixel width & height are different; the average of both will be used'
}

// Get annotation & detections
def annotations = getAnnotationObjects()
def selected = getSelectedObject()
if (selected == null || !selected.isAnnotation()) {
    print 'Please select an annotation object!'
    return
}

// We need one selected annotation as a starting point; if we have other annotations, they will constrain the output
annotations.remove(selected)

// If we have at most one other annotation, it represents the tissue
Geometry areaTissue
PathObject tissueAnnotation
// Calculate how much to expand
double expandPixels = expandMarginMicrons / cal.getAveragedPixelSizeMicrons()
def roiOriginal = selected.getROI()
def plane = roiOriginal.getImagePlane()
def areaTumor = roiOriginal.getGeometry()

if (annotations.isEmpty()) {
  areaTissue = ROIs.createRectangleROI(0, 0, server.getWidth(), server.getHeight(), plane).getGeometry()
} else if (annotations.size() == 1) {
  tissueAnnotation = annotations.get(0)
  areaTissue = tissueAnnotation.getROI().getGeometry()
} else {
  print 'Sorry, this script only support one selected annotation for the tumor region, and at most one other annotation to constrain the expansion'
  return
}
println("Working, give it some time")


// Get the outer margin area
def geomOuter = areaTumor.buffer(expandPixels)

geomOuter = geomOuter.difference(areaTumor)
geomOuter = geomOuter.intersection(areaTissue)
def roiOuter = GeometryTools.geometryToROI(geomOuter, plane)
def annotationOuter = PathObjects.createAnnotationObject(roiOuter)
annotationOuter.setName("Outer margin")
annotationOuter.setColorRGB(colorOuterMargin)

innerAnnotations = []
innerAnnotations << annotationOuter
//innerAnnotations << selected

for (i=0; i<howManyTimes;i++){


    //select the current expansion, which the first time is outside of the tumor, then expand it and intersect it
    currentArea = innerAnnotations[innerAnnotations.size()-1].getROI().getGeometry()
    println(currentArea)
    /*
    if (getQuPath().getBuildString().split()[1]<"0.2.0-m2"){
        areaExpansion = PathROIToolsAwt.shapeMorphology(currentArea, expandPixels)
    }else {areaExpansion = PathROIToolsAwt.getArea(PathROIToolsAwt.roiMorphology(innerAnnotations[innerAnnotations.size()-1].getROI(), expandPixels))}
    */
    areaExpansion = currentArea.buffer(expandPixels)
    
    areaExpansion = areaExpansion.intersection(areaTumor)
    //println(areaExpansion)
    areaExpansion = areaExpansion.intersection(areaTissue)
    //println(areaExpansion)
    //remove outer areas previously defined as other innerAnnotations
    if(i>=1){
        for (k=1; k<=i;k++){
            remove = innerAnnotations[innerAnnotations.size()-k].getROI().getGeometry()
            areaExpansion = areaExpansion.difference(remove)
            
        }
    }
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
core = areaTumor

for (i=1; i<=howManyTimes;i++){
    core = core.difference(innerAnnotations[i].getROI().getGeometry())
}
coreROI = GeometryTools.geometryToROI(core, plane)
coreAnno = PathObjects.createAnnotationObject(coreROI)
coreAnno.setName("Remaining Tumor")
innerAnnotations << coreAnno



// Add the annotations
hierarchy.getSelectionModel().clearSelection()
//hierarchy.removeObject(selected, true)
def annotationsToAdd = innerAnnotations;
annotationsToAdd.each {it.setLocked(lockAnnotations)}
if (tissueAnnotation == null) {
    hierarchy.addPathObjects(annotationsToAdd, false)
} else {
    tissueAnnotation.addPathObjects(annotationsToAdd)
    hierarchy.fireHierarchyChangedEvent(this, tissueAnnotation)
    if (lockAnnotations)
        tissueAnnotation.setLocked(true)
}
println("Done! Wheeeee!")