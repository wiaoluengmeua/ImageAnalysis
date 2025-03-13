//Another version of the split annotations script from : https://github.com/qupath/qupath/issues/99
//0.1.2
import static qupath.lib.roi.PathROIToolsAwt.splitAreaToPolygons
import qupath.lib.roi.AreaROI
import qupath.lib.objects.PathAnnotationObject

// Get all the annotations
def annotations = getAnnotationObjects()

// Prepare to add/remove annotations in batch
def toAdd = []
def toRemove = []

// Loop through the annotations, preparing to make changes
for (annotation in annotations) {
    def roi = annotation.getROI()
    // If we have an area, prepare to remove it - 
    // and add the separated polygons
    if (roi instanceof AreaROI) {
        toRemove << annotation
        for (p in splitAreaToPolygons(roi)[1]) {
            toAdd << new PathAnnotationObject(p, annotation.getPathClass())
        }
    }
}

// Perform the changes
removeObjects(toRemove, true)
addObjects(toAdd)