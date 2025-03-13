//For earlier versions of QuPath, tested in 0.1.3
//Create a detection object the same size and shape as the TMA core
//Give it summary measurements for the percentage of cells of each class within the core
//When one of the Class % measurements is selected while viewing Measure->Measurement Maps, all other detections will disappear
//and only the summary detection objects will be visible.
//It may be best to turn off annotation visibility.

import qupath.lib.objects.PathDetectionObject
import qupath.lib.objects.PathCellObject
hierarchy = getCurrentHierarchy()
cores = hierarchy.getTMAGrid().getTMACoreList()
Set list = []

for (object in getAllObjects().findAll{it.isDetection() /*|| it.isAnnotation()*/}) {
    list << object.getPathClass().toString()
}
print list
print "before cores"
cores.each {
print "initiating core"
    //Find the cell count in this core
   total = hierarchy.getDescendantObjects(it, null, PathCellObject).size()
   //Prevent divide by zero errors in empty TMA cores
   if (total != 0){
      for (className in list) {
        cellType = hierarchy.getDescendantObjects(it,null, PathCellObject).findAll{it.getPathClass() == getPathClass(className)}.size()
        it.getMeasurementList().putMeasurement(className+" cell %", cellType/(total)*100)
      }
   }
   else {
     for (className in list) {
        it.getMeasurementList().putMeasurement(className+" cell %", 0)
     }
   }
   fireHierarchyUpdate()
   print "core complete"
}

cores.each {
print it
    roi = it.getROI()
    coreName = it.getName()+" - Tile"
    def detection = new PathDetectionObject(roi, getPathClass("Tile"))
    hierarchy.addPathObject(detection, false)

    ml = it.getMeasurementList()

    for (i=0;i<ml.size(); i++){

        detection.getMeasurementList().putMeasurement(ml.getMeasurementName(i), measurement(it, ml.getMeasurementName(i)))
    
    }
    fireHierarchyUpdate()
}
println("Are you done yet?")
