//from Pete
//0.2.0
//Change for 0.1.2 shown below

import qupath.imagej.objects.*

getCellObjects().each{
    def ml = it.getMeasurementList()
    def roi = it.getNucleusROI()
    //for 0.1.2
    //def roiIJ = ROIConverterIJ.convertToIJRoi(roi, 0, 0, 1)
    roiIJ = IJTools.convertToIJRoi(roi, 0, 0, 1)
    def angle = roiIJ.getFeretValues()[1]
    ml.putMeasurement('Nucleus angle', angle)
    ml.close()
}
fireHierarchyUpdate()
print "done"