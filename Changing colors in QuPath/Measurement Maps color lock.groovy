/**
 * Set a MeasurementMapper in QuPath v0.1.2 to control the display in a script.
 *
 * Created for https://groups.google.com/d/msg/qupath-users/9kMNlg4sgAs/dUHQpROLDwAJ
 *
 * @author Pete Bankhead
 */


import qupath.lib.gui.helpers.MeasurementMapper

import static qupath.lib.scripting.QPEx.*

// Define measurement & display range
def name = "Nucleus/Cell area ratio" // Set to null to reset
double minValue = 0.0
double maxValue = 1.0

// Request current viewer & objects
def viewer = getCurrentViewer()
def options = viewer.getOverlayOptions()
def detections = getDetectionObjects()

// Update the display
if (name) {
    print String.format('Setting measurement map: %s (%.2f - %.2f)', name, minValue, maxValue)
    def mapper = new MeasurementMapper(name, detections)
    mapper.setDisplayMinValue(minValue)
    mapper.setDisplayMaxValue(maxValue)
    options.setMeasurementMapper(mapper)
} else {
    print 'Resetting measurement map'
    options.setMeasurementMapper(null)
}