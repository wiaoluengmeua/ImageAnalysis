//Objective: Find the number of neighbors within distance X
//Problem: Delaunay clusters exclude nearby cells if another cluster obscures those cells
// 0.2.0 - VERY SLOW

/**********Detection radius*************/
distanceMicrons = 25
/***************************************/

//Replace the next coding line with
//totalCells = getCellObjects().findAll{it.getPathClass() == getPathClass("Tumor"}
//to look at only one class (Tumor, in the above example) in the line below.
totalCells = getCellObjects()
print "please wait, this may take a long time"
totalCells.each{
    originalClass = it.getPathClass()
    it.setPathClass(getPathClass("DjdofiSdflKFj"))
    detectionCentroidDistances(false)
    closeCells = totalCells.findAll{measurement(it,"Distance to detection DjdofiSdflKFj µm") <= distanceMicrons && measurement(it,"Distance to detection DjdofiSdflKFj µm") != 0}
    it.getMeasurementList().putMeasurement("Cells within "+distanceMicrons+" microns", closeCells.size())
    it.setPathClass(originalClass)
}
removeMeasurements(qupath.lib.objects.PathCellObject, "Distance to detection DjdofiSdflKFj µm");

println("Done")