//Checks for all detections within a given annotation, DOES NOT EXCLUDE DETECTIONS WITHIN SUB-ANNOTATIONS.
//That last bit should make it compatible with trained classifiers.
//0.1.2
import qupath.lib.objects.PathDetectionObject
def imageData = getCurrentImageData()
def server = imageData.getServer()
def pixelSize = server.getPixelHeightMicrons()
Set classList = []
for (object in getAllObjects().findAll{it.isDetection() /*|| it.isAnnotation()*/}) {
    classList << object.getPathClass()
}
println(classList)
hierarchy = getCurrentHierarchy()

for (annotation in getAnnotationObjects()){

    for (aClass in classList){
        if (aClass){
            def tiles = hierarchy.getDescendantObjects(annotation,null, PathDetectionObject).findAll{it.getPathClass() == aClass}
            double totalArea = 0
    
            for (def tile in tiles){
                totalArea += tile.getROI().getArea()
            }
            annotation.getMeasurementList().putMeasurement(aClass.getName()+" area px", totalArea)
            annotation.getMeasurementList().putMeasurement(aClass.getName()+" area um^2", totalArea*pixelSize*pixelSize)

            def annotationArea = annotation.getROI().getArea()
            annotation.getMeasurementList().putMeasurement(aClass.getName()+" area %", totalArea/annotationArea*100)
        }
    }

}
println("done")