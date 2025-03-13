//Checks for all detections within a TMA, DOES NOT EXCLUDE DETECTIONS WITHIN SUB-ANNOTATIONS.
//That last bit should make it compatible with trained classifiers.
//Cells per mm^2 assumes an annotation within the TMA, if there are no annotations within the TMA, remove related lines.
//0.1.2
import qupath.lib.objects.PathCellObject
def imageData = getCurrentImageData()
def server = imageData.getServer()
def pixelSize = server.getPixelHeightMicrons()
Set classList = []
for (object in getCellObjects()) {
    classList << object.getPathClass()
}
println(classList)
hierarchy = getCurrentHierarchy()

getTMACoreList().each{
    totalCells = hierarchy.getDescendantObjects(it,null, PathCellObject)

    for (aClass in classList){
        if (aClass){
            if (totalCells.size() > 0){
                def cells = hierarchy.getDescendantObjects(it,null, PathCellObject).findAll{it.getPathClass() == aClass}
                //it.getMeasurementList().putMeasurement(aClass.getName()+" cells", cells.size())
                it.getMeasurementList().putMeasurement(aClass.getName()+" %", cells.size()*100/totalCells.size())

                def annotationArea = it.getChildObjects()[0].getROI().getArea()
                it.getMeasurementList().putMeasurement(aClass.getName()+" cells/mm^2", cells.size()/(annotationArea*pixelSize*pixelSize/1000000))
            } else {
                //it.getMeasurementList().putMeasurement(aClass.getName()+" cells", 0)
                it.getMeasurementList().putMeasurement(aClass.getName()+" %", 0)
                it.getMeasurementList().putMeasurement(aClass.getName()+" cells/mm^2", 0)
            
            }
        }
    }

}
println("done")