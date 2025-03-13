//QP 0.2.3
//from https://forum.image.sc/t/qupath-scripting-combining-two-annotations/48152/2
//For 0.1.2 see below
def tissue = getAnnotationObjects().find {it.getPathClass() == getPathClass("Positive")}
def vessel = getAnnotationObjects().find {it.getPathClass() == getPathClass("Negative")}
def plane = tissue.getROI().getImagePlane()
if (plane != vessel.getROI().getImagePlane()) {
    println 'Annotations are on different planes!'
    return    
}
// Convert to geometries & compute distance
// Note: see https://locationtech.github.io/jts/javadoc/org/locationtech/jts/geom/Geometry.html#distance-org.locationtech.jts.geom.Geometry-
def g1 = tissue.getROI().getGeometry()
def g2 = vessel.getROI().getGeometry()

def difference = g1.difference(g2)
if (difference.isEmpty())
    println "No intersection between areas"
else {
    def roi = GeometryTools.geometryToROI(difference, plane)
    def annotation = PathObjects.createAnnotationObject(roi, getPathClass('Difference'))
    addObject(annotation)
    selectObjects(annotation)
    println "Annotated created for subtraction"
}
//remove original objects
removeObject(tissue, true)
removeObject(vessel, true)





// https://groups.google.com/forum/#!topic/qupath-users/WlxDfgjqrCU
//0.1.2

import qupath.lib.roi.*
import qupath.lib.objects.*

classToSubtract = "Endothel"
    
def topLevel = getObjects{return it.getLevel()==1 && it.isAnnotation()}

for (parent in topLevel){

    def total = []
    def polygons = []
    subtractions = parent.getChildObjects().findAll{it.isAnnotation() && it.getPathClass() == getPathClass(classToSubtract)}
    for (subtractyBit in subtractions){
        if (subtractyBit instanceof AreaROI){
           subtractionROIs = PathROIToolsAwt.splitAreaToPolygons(subtractyBit.getROI())
           total.addAll(subtractionROIs[1])
        } else {total.addAll(subtractyBit.getROI())}              
                
    }     
    if (parent instanceof AreaROI){
        polygons = PathROIToolsAwt.splitAreaToPolygons(parent.getROI())
        total.addAll(polygons[0])
    } else { polygons[1] = parent.getROI()}

            
    def newPolygons = polygons[1].collect {
    updated = it
    for (hole in total)
         updated = PathROIToolsAwt.combineROIs(updated, hole, PathROIToolsAwt.CombineOp.SUBTRACT)
         return updated
    }
                // Remove original annotation, add new ones
    annotations = newPolygons.collect {new PathAnnotationObject(updated, parent.getPathClass())}


    addObjects(annotations)

    removeObjects(subtractions, true)
    removeObject(parent, true)
}

