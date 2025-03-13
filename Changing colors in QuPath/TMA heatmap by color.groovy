// Add percentages by cell class to each TMA core
//Confirmed for 0.2.0
print "Wait for the comment indicating that it is done!"
print "This process is slow."
import qupath.lib.objects.PathCellObject
def metadata = getCurrentImageData().getServer().getOriginalMetadata()
def pixelSize = metadata.pixelCalibration.pixelWidth.value

hierarchy = getCurrentHierarchy()
cores = hierarchy.getTMAGrid().getTMACoreList()
Set list = []
for (object in getAllObjects().findAll{it.isDetection() /*|| it.isAnnotation()*/}) {
    list << object.getPathClass()
}
print list
def cellList = []
cores.each {
    //Find the cell count in this core
   cellList = []
   cellList = qupath.lib.objects.PathObjectTools.getDescendantObjects(it, cellList, PathCellObject)
   total = cellList.size()
   //Prevent divide by zero errors in empty TMA cores
   if (total != 0){
   annos=it.getChildObjects()[0]
   if (annos.isAnnotation()){
       for (className in list) {
        cellType = cellList.findAll{p->p.getPathClass() == className}.size()
        annotationArea = annos.getROI().getArea()*pixelSize*pixelSize/1000000
        println(cellType)
        println(annotationArea)
        it.getMeasurementList().putMeasurement(className.toString()+" cells/mm^2", cellType/(annotationArea))
      }
      
   }
      for (className in list) {
        cellType = cellList.findAll{p->p.getPathClass() == className}.size()
        it.getMeasurementList().putMeasurement(className.toString()+" cell %", cellType/(total)*100)
      }
   }
   else {
     for (className in list) {
        it.getMeasurementList().putMeasurement(className.toString()+" cell %", 0)
     }
   }
}
import qupath.lib.objects.PathDetectionObject

cores.each {
    roi = it.getROI()
    coreName = it.getName()+" - Tile"
    def detection = new PathDetectionObject(roi, getPathClass("Tile"))
    hierarchy.addPathObject(detection)

    ml = it.getMeasurementList()
    for (i=0;i<ml.size(); i++){
    //println(ml.getMeasurementValue(i))
    //println(detection)
        detection.getMeasurementList().putMeasurement(ml.getMeasurementName(i), measurement(it, ml.getMeasurementName(i)))
    
    }
    fireHierarchyUpdate()
}
println("Now it is done.")
