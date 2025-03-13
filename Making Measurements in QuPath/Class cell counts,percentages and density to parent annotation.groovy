//Checks for all detections within a given annotation, DOES NOT EXCLUDE DETECTIONS WITHIN SUB-ANNOTATIONS.
//That last bit should make it compatible with trained classifiers.
//0.1.2

//Do you want to include cell counts? If so, make True. This can cause duplicate measurements in 1.3 and beyond
COUNTS = false
import qupath.lib.objects.PathCellObject
imageData = getCurrentImageData()
server = imageData.getServer()
pixelSize = server.getPixelHeightMicrons()
Set classList = []
for (object in getAllObjects().findAll{it.isDetection() /*|| it.isAnnotation()*/}) {
    classList << object.getPathClass()
}
println(classList)
hierarchy = getCurrentHierarchy()

for (annotation in getAnnotationObjects()){
    totalCells = []
    totalCells = hierarchy.getDescendantObjects(annotation,null, PathCellObject)

    for (aClass in classList){
        if (aClass){
            if (totalCells.size() > 0){
                cells = hierarchy.getDescendantObjects(annotation,null, PathCellObject).findAll{it.getPathClass() == aClass}
                if(COUNTS){annotation.getMeasurementList().putMeasurement(aClass.getName()+" cells", cells.size())}
                annotation.getMeasurementList().putMeasurement(aClass.getName()+" %", cells.size()*100/totalCells.size())

                annotationArea = annotation.getROI().getArea()
                annotation.getMeasurementList().putMeasurement(aClass.getName()+" cells/mm^2", cells.size()/(annotationArea*pixelSize*pixelSize/1000000))
            } else {
                if(COUNTS){annotation.getMeasurementList().putMeasurement(aClass.getName()+" cells", 0)}
                annotation.getMeasurementList().putMeasurement(aClass.getName()+" %", 0)
                annotation.getMeasurementList().putMeasurement(aClass.getName()+" cells/mm^2", 0)
            
            }
        }
    }

}
println("done")
