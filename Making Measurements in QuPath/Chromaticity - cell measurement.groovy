// https://forum.image.sc/t/detecting-purple-chromogen-classifying-cells-based-on-green-chromaticity/35576/5
//0.2.0
// Add intensity features (cells already detected)
selectCells();
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 1.0,  "region": "Cell nucleus",  "tileSizeMicrons": 25.0,  "colorOD": false,  "colorStain1": false,  "colorStain2": false,  "colorStain3": false,  "colorRed": true,  "colorGreen": true,  "colorBlue": true,  "colorHue": false,  "colorSaturation": false,  "colorBrightness": false,  "doMean": true,  "doStdDev": false,  "doMinMax": false,  "doMedian": false,  "doHaralick": false,  "haralickDistance": 1,  "haralickBins": 32}');
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 1.0,  "region": "ROI",  "tileSizeMicrons": 25.0,  "colorOD": false,  "colorStain1": false,  "colorStain2": false,  "colorStain3": false,  "colorRed": true,  "colorGreen": true,  "colorBlue": true,  "colorHue": false,  "colorSaturation": false,  "colorBrightness": false,  "doMean": true,  "doStdDev": false,  "doMinMax": false,  "doMedian": false,  "doHaralick": false,  "haralickDistance": 1,  "haralickBins": 32}');

// Add chromaticity measurements
def nucleusMeasurement = "Nucleus: 1.00 µm per pixel: %s: Mean"
def cellMeasurement = "ROI: 1.00 µm per pixel: %s: Mean"

for (cell in getCellObjects()) {
    def measurementList = cell.getMeasurementList()
    addGreenChromaticity(measurementList, nucleusMeasurement)
    addGreenChromaticity(measurementList, cellMeasurement)
    measurementList.close()
}
fireHierarchyUpdate()

def addGreenChromaticity(measurementList, measurement) {
    double r = measurementList.getMeasurementValue(String.format(measurement, "Red"))
    double g = measurementList.getMeasurementValue(String.format(measurement, "Green"))
    double b = measurementList.getMeasurementValue(String.format(measurement, "Blue"))
    def name = String.format(measurement, "Green chromaticity")
    measurementList.putMeasurement(name, g/Math.max(1, r+g+b))
}