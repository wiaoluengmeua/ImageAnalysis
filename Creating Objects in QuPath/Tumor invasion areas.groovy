/**
 * Script to help with annotating tumor regions, chopping increasing chunks into the tumor.
 * SEE THREAD HERE FOR DESCRIPTION ON USE: 
 * Here, each of the margin regions is approximately 100 microns in width.
 * Should work with both 1.2 and 0.2.0m2 due to code from Thomas Kilvaer found here: https://petebankhead.github.io/qupath/scripts/2018/08/08/three-regions.html
 *
 * @author Pete Bankhead
 * @mangled by Svidro
 */


import qupath.lib.common.GeneralTools
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.objects.PathObject
import qupath.lib.roi.PathROIToolsAwt

import java.awt.Rectangle
import java.awt.geom.Area



//-----
// Some things you might want to change

// How much to expand each region
double expandMarginMicrons = 100.0
// How many times you want to chop into your annotation. Edit color script around line 115 if you go over 5
int howManyTimes = 4


// Define the colors
// Inner layers are given scripted colors, but gretaer than 6 or 7 layers may require adjustments
def colorOuterMargin = getColorRGB(0, 200, 0)


// Choose whether to lock the annotations or not (it's generally a good idea to avoid accidentally moving them)
def lockAnnotations = true

//-----

// Extract the main info we need
def imageData = getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def server = imageData.getServer()

// We need the pixel size
if (!server.hasPixelSizeMicrons()) {
    print 'We need the pixel size information here!'
    return
}
if (!GeneralTools.almostTheSame(server.getPixelWidthMicrons(), server.getPixelHeightMicrons(), 0.0001)) {
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
Area areaTissue
PathObject tissueAnnotation
if (annotations.isEmpty()) {
    areaTissue = new Area(new Rectangle(0, 0, server.getWidth(), server.getHeight()))
} else if (annotations.size() == 1) {
    tissueAnnotation = annotations.get(0)
    areaTissue = PathROIToolsAwt.getArea(tissueAnnotation.getROI())
} else {
    print 'Sorry, this script only support one selected annotation for the tumor region, and at most one other annotation to constrain the expansion'
    return
}
println("Working, give it some time")
// Calculate how much to expand
double expandPixels = expandMarginMicrons / server.getAveragedPixelSizeMicrons()
def roiOriginal = selected.getROI()
def areaTumor = PathROIToolsAwt.getArea(roiOriginal)

// Get the outer margin area
if (getQuPath().getBuildString().split()[1]<"0.2.0-m2"){
    def areaOuter = PathROIToolsAwt.shapeMorphology(areaTumor, expandPixels)
}else {areaOuter = PathROIToolsAwt.getArea(PathROIToolsAwt.roiMorphology(roiOriginal, expandPixels))}

areaOuter.subtract(areaTumor)
areaOuter.intersect(areaTissue)
def roiOuter = PathROIToolsAwt.getShapeROI(areaOuter, roiOriginal.getC(), roiOriginal.getZ(), roiOriginal.getT())
def annotationOuter = new PathAnnotationObject(roiOuter)
annotationOuter.setName("Outer margin")
annotationOuter.setColorRGB(colorOuterMargin)

innerAnnotations = []
innerAnnotations << annotationOuter

for (i=0; i<howManyTimes;i++){


    //select the current expansion, which the first time is outside of the tumor, then expand it and intersect it
    currentArea = PathROIToolsAwt.getArea(innerAnnotations[innerAnnotations.size()-1].getROI())
    if (getQuPath().getBuildString().split()[1]<"0.2.0-m2"){
        areaExpansion = PathROIToolsAwt.shapeMorphology(currentArea, expandPixels)
    }else {areaExpansion = PathROIToolsAwt.getArea(PathROIToolsAwt.roiMorphology(innerAnnotations[innerAnnotations.size()-1].getROI(), expandPixels))}
    areaExpansion.intersect(areaTumor)
    areaExpansion.intersect(areaTissue)
    if(i>=1){
        for (k=1; k<=i;k++){
            areaExpansion.subtract(PathROIToolsAwt.getArea(innerAnnotations[innerAnnotations.size()-k].getROI()))
        }
    }
    roiExpansion = PathROIToolsAwt.getShapeROI(areaExpansion, roiOriginal.getC(), roiOriginal.getZ(), roiOriginal.getT())
    j = i+1
    annotationExpansion = new PathAnnotationObject(roiExpansion)
    int nameValue = j*expandMarginMicrons
    annotationExpansion.setName("Inner margin "+nameValue+" microns")
    annotationExpansion.setColorRGB(getColorRGB(20*i, 40*i, 200-30*i))
    innerAnnotations << annotationExpansion

}


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