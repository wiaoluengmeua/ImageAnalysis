// Save the total value of your subcellular detection intensities to the cell measurement list so that it may be exported
// with the cell, or used for classification
//0.1.2 and 0.2.0
// This value could then be divided by the total area of subcellular detection (Num spots, if Expected spot size is left as 1)
// for the mean intensity

// Create the name of the new measurement, in this case Channel 3 of a fluorescent image.
// ONLY the "Channel 3" should change to the name of the stain you are measuring, for example "DAB" in a brightfield image
def subcellularDetectionChannel = "Subcellular cluster: Channel 3: "

def newKey = subcellularDetectionChannel+"Mean Intensity"

//This step ensures that there is at least a measurement value of 0 in each cell
for (def cell : getCellObjects()) {
    def ml = cell.getMeasurementList()
    ml.putMeasurement(newKey, 0)

}
//Create a list of all subcellular objects
def subCells = getObjects({p -> p.class == qupath.imagej.detect.cells.SubcellularDetection.SubcellularObject.class})

// Loop through all subcellular detections
for (c in subCells) {
    // Find the containing cell
    def cell = c.getParent()
    def ml = cell.getMeasurementList()
    double area = c.getMeasurementList().getMeasurementValue( subcellularDetectionChannel+"Area")
    double intensity = c.getMeasurementList().getMeasurementValue( subcellularDetectionChannel+"Mean channel intensity")
    //calculate the total intensity of stain in this subcellular object, and add it to the total
    double stain = area*intensity
    double x = cell.getMeasurementList().getMeasurementValue(newKey);
    x = x+stain
    ml.putMeasurement(newKey, x)


}
println("Total subcellular stain intensity added to cell measurement list as " + newKey)