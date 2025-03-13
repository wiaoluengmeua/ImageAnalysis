//Once your nucleus detection is settled using Cell Detection, replace the cell detection line of code with your own

//The first few lines of code create a whole image object and lock it so that you can draw annotations within.

createSelectAllObject(true);
selected = getSelectedObject()
selected.setLocked(true)

runPlugin('qupath.imagej.detect.nuclei.WatershedCellDetection', '{"detectionImageFluorescence": 3,  "requestedPixelSizeMicrons": 0.5,  "backgroundRadiusMicrons": 0.0,  "medianRadiusMicrons": 1.0,  "sigmaMicrons": 1.5,  "minAreaMicrons": 50.0,  "maxAreaMicrons": 600.0,  "threshold": 400,  "watershedPostProcess": true,  "cellExpansionMicrons": 0.0,  "includeNuclei": true,  "smoothBoundaries": true,  "makeMeasurements": true}');

//Step 2 is entirely manual at this point and requires that you hand draw your cytoplasms