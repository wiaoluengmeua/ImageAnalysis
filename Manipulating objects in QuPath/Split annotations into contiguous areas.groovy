//from forums: https://groups.google.com/d/msg/qupath-users/DugpYQJq9Ic/J1qHWkZ4CgAJ
//0.1.2
import qupath.lib.roi.*
import qupath.lib.objects.*

//selects all annotation areas.  Change this if you want only a subset of your areas split into contiguous area components.
def areaAnnotations = getAnnotationObjects().findAll {it.getROI() instanceof AreaROI}

areaAnnotations.each { selected ->
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