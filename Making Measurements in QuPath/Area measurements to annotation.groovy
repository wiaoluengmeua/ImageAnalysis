//Useful when using detection objects returned from ImageJ macros.  Note that areas are in pixels and would need to be converted to microns
//0.1.2
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
println("done")