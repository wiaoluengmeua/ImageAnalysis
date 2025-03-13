// Add percentages by cell class to each TMA core, tested for 
//0.1.2

import qupath.lib.objects.PathCellObject
hierarchy = getCurrentHierarchy()
cores = hierarchy.getTMAGrid().getTMACoreList()
Set list = []

for (object in getAllObjects().findAll{it.isDetection() /*|| it.isAnnotation()*/}) {
    list << object.getPathClass().toString()
}

cores.each {
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
}