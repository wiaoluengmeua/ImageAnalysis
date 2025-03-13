/* 0.2.3, https://forum.image.sc/t/tma-dearrayer-problem/48234/4
 * Run QuPath's TMA dearrayer at a different resolution.
 * This can give a bit more control over the output if the original dearraying fails.
 * 
 * @author Pete Bankhead
 */

// Change this to adjust the resolution at which cores are detected
// Lower values mean the dearraying is done on a larger image (and will be slower)
double requestedPixelSize = 10

// Adjust these for other detection parameters
double coreDiameter = 1200
double roiScaleFactor = 1.05
def horizontalLabels = 'A-J'
def verticalLabels = '1-16'
boolean horizontalLabelFirst = 100
double densityThreshold = 0.05
boolean isFluorescence = false


// Actually do the dearraying
import qupath.imagej.detect.dearray.TMADearrayerPluginIJ.Dearrayer
def dearrayer = new Dearrayer()

def server = getCurrentServer()
double downsample = requestedPixelSize / server.getPixelCalibration().getAveragedPixelSize()
double fullCoreDiameterPx = coreDiameter / server.getPixelCalibration().getAveragedPixelSize()

def request = RegionRequest.createInstance(server, downsample)
dearrayer.ip = IJTools.convertToImagePlus(server, request).getImage().getProcessor()

def tmaGrid = dearrayer.doDearraying(
    fullCoreDiameterPx,
    downsample, 
    densityThreshold,
    roiScaleFactor,
    isFluorescence,
    PathObjectTools.parseTMALabelString(horizontalLabels),
    PathObjectTools.parseTMALabelString(verticalLabels),
    horizontalLabelFirst
)
getCurrentHierarchy().setTMAGrid(tmaGrid)
println 'Done! ' + println tmaGrid 