// from https://forum.image.sc/t/problem-running-script-from-publication/35542/7
//0.2.0
import qupath.lib.gui.tools.*

// Print the names (just to check which you want)
println MeasurementMapper.loadDefaultColorMaps()

// Choose one of them
def colorMapper = MeasurementMapper.loadDefaultColorMaps().find {it.getName() == 'Magma'}

// Create a measurement mapper
def detections = getDetectionObjects()
def measurementMapper = new MeasurementMapper(colorMapper, 'Nucleus: DAB OD mean', detections)

// Show the measurement mapper in the current viewer
def viewer = getCurrentViewer()
def overlayOptions = viewer.getOverlayOptions()
overlayOptions.setMeasurementMapper(measurementMapper)