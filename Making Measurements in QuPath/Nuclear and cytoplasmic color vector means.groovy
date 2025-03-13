//Calculate the mean OD values in the nucleus and cytoplasm for any number of sets of color vectors 
//Intended for 0.1.2, there are easier ways to do this in 0.2.0 with the ability to choose Nucleus as the ROI for Add intensity features.
import qupath.lib.objects.*

//This function holds a list of color vectors and their Add Intensity Features command that will add the desired measurements
//to your cells.  Make sure you name the stains (for example in the first example, Stain 1 is called "Blue") differently
//so that their Measurements will end up labeled differently.  Notice that the Add Intensity Features command includes 
//"Colorstain":true, etc. which needs to be true for the measurements you wish to add.
  
def addColors(){
    setColorDeconvolutionStains('{"Name" : "DAB Yellow", "Stain 1" : "Blue", "Values 1" : "0.56477 0.65032 0.50806 ", "Stain 2" : "Yellow", "Values 2" : "0.0091 0.01316 0.99987 ", "Background" : " 255 255 255 "}');
    runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 0.25,  "region": "ROI",  "tileSizeMicrons": 25.0,  "colorOD": true,  "colorStain1": true,  "colorStain2": true,  "colorStain3": false,  "colorRed": false,  "colorGreen": false,  "colorBlue": false,  "colorHue": false,  "colorSaturation": false,  "colorBrightness": false,  "doMean": true,  "doStdDev": false,  "doMinMax": false,  "doMedian": false,  "doHaralick": false,  "haralickDistance": 1,  "haralickBins": 32}');

    setColorDeconvolutionStains('{"Name" : "Background1", "Stain 1" : "Blue Background1", "Values 1" : "0.56195 0.77393 0.29197 ", "Stain 2" : "Beige Background1", "Values 2" : "0.34398 0.59797 0.72396 ", "Background" : " 255 255 255 "}');
    runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 0.25,  "region": "ROI",  "tileSizeMicrons": 25.0,  "colorOD": false,  "colorStain1": true,  "colorStain2": true,  "colorStain3": false,  "colorRed": false,  "colorGreen": false,  "colorBlue": false,  "colorHue": false,  "colorSaturation": false,  "colorBrightness": false,  "doMean": true,  "doStdDev": false,  "doMinMax": false,  "doMedian": false,  "doHaralick": false,  "haralickDistance": 1,  "haralickBins": 32}');
}

//The only thing beyond this point that should need to be modified is the removalList command at the end, which you can disable
//if you wish to keep whole cell measurements

// Get cells & create temporary nucleus objects - storing link to cell in a map
def cells = getCellObjects()
def map = [:]
for (cell in cells) {
	def detection = new PathDetectionObject(cell.getNucleusROI())
	map[detection] = cell
}

// Get the nuclei as a list
def nuclei = map.keySet() as List
// and then select the nuclei
getCurrentHierarchy().getSelectionModel().setSelectedObjects(nuclei, null)

// Add as many sets of color deconvolution stains and Intensity features plugins as you want here
//This section ONLY adds measurements to the temporary nucleus objects, not the cell
addColors()

//etc etc. make sure each set has different names for the stains or else they will overwrite

// Don't need selection now
clearSelectedObjects()

// Can update measurements generated for the nucleus to the parent cell's measurement list
for (nucleus in nuclei) {
    def cell = map[nucleus]
    def cellMeasurements = cell.getMeasurementList()
    for (key in nucleus.getMeasurementList().getMeasurementNames()) {
        double value = nucleus.getMeasurementList().getMeasurementValue(key)
        def listOfStrings = key.tokenize(':')     
        def baseValueName = listOfStrings[-2]+listOfStrings[-1]
        nuclearName = "Nuclear" + baseValueName
        cellMeasurements.putMeasurement(nuclearName, value)
    }
    cellMeasurements.closeList()
}

//I want to remove the original whole cell measurements which contain the mu symbol
// Not yet sure I will find the whole cell useful so not adding it back in yet.
def removalList = []

//Create whole cell measurements for all of the above stains
selectDetections()

addColors()

//Create cytoplasmic measurements by subtracting the nuclear measurements from the whole cell, based total intensity (mean value*area)
for (cell in cells) {

   //A mess of things I could probably call within functions
   def cellMeasurements = cell.getMeasurementList()
   double cellArea = cell.getMeasurementList().getMeasurementValue("Cell: Area")
   double nuclearArea = cell.getMeasurementList().getMeasurementValue("Nucleus: Area")
   double cytoplasmicArea = cellArea-nuclearArea

   for (key in cell.getMeasurementList().getMeasurementNames()) {
           //check if the value is one of the added intensity measurements
        if (key.contains("per pixel")){
            //check if we already have this value in the list.
            //probably an easier way to do this outside of every cycle of the for loop
            if (!removalList.contains(key)) removalList<<key
            double value = cell.getMeasurementList().getMeasurementValue(key)
            //calculate the sum of the OD measurements
            cellOD = value * cellArea
            
            //break each measurement into component parts, then take the last two 
            // which will usually contain the color vector and "mean"
            def listOfStrings = key.tokenize(':')          
            def baseValueName = listOfStrings[-2]+listOfStrings[-1]
            //access the nuclear value version of the base name, and use it and the whole cell value to
            //calcuate the rough cytoplasmic value
            def cytoplasmicKey = "Cytopasmic" + baseValueName
            def nuclearKey = "Nuclear" + baseValueName
            def nuclearOD = nuclearArea * cell.getMeasurementList().getMeasurementValue(nuclearKey)

            def cytoplasmicValue = (cellOD - nuclearOD)/cytoplasmicArea
            cellMeasurements.putMeasurement(cytoplasmicKey, cytoplasmicValue)
        }
    }
    cellMeasurements.closeList()
}
removalList.each {println(it)}

//comment out this line if you want the whole cell measurements.
removalList.each {removeMeasurements(qupath.lib.objects.PathCellObject, it)}

fireHierarchyUpdate()
println "Done!"
