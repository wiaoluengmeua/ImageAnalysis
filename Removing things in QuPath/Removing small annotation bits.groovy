import qupath.lib.roi.*
import qupath.lib.objects.*

//Choose the area threshold below which the annotations will be deleted
def ANNOTATION_AREA_MICRONS = 5
//PART 1
//This section splits ALL annotations into contiguous areas, so that any pieces off on their own can be deleted.
//Use only Part 2 if you already have individual annotations.
annotationList = getAnnotationObjects()
for (selected in annotationList){
if (!(selected.getROI() instanceof AreaROI)) {
    print 'Selected object does not have an AreaROI!'
    return
}

// Try to do split, and ensure holes are taken into consideration
def polygons = PathROIToolsAwt.splitAreaToPolygons(selected.getROI())
def newPolygons = polygons[1].collect {
    updated = it
    for (hole in polygons[0])
        updated = PathROIToolsAwt.combineROIs(updated, hole, PathROIToolsAwt.CombineOp.SUBTRACT)
    return updated
}

// Remove original annotation, add new ones
annotations = newPolygons.collect {new PathAnnotationObject(it)}
resetSelection()
removeObject(selected, true)
addObjects(annotations)
}

//PART2
//This section 

def server = getCurrentImageData().getServer()
double pixelWidth = server.getPixelWidthMicrons()
double pixelHeight = server.getPixelHeightMicrons()
def smallAnnotations = getAnnotationObjects().findAll {it.getROI().getScaledArea(pixelWidth, pixelHeight) < ANNOTATION_AREA_MICRONS}
removeObjects(smallAnnotations, true)
fireHierarchyUpdate()


//PART3
//Merge annotations back into a single annotation if this is desired for data analysis.

//selectAnnotations()
//mergeSelectedAnnotations()