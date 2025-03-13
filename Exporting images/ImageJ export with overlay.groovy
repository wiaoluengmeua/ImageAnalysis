//0.2.0
//https://forum.image.sc/t/script-for-send-region-to-imagej/39554/3?u=research_associate
//exporting to imageJ with overlay
import qupath.imagej.gui.IJExtension

double downsample = 2.0

def server = getCurrentServer()
def selectedObject = getSelectedObject()
def request = RegionRequest.createInstance(server.getPath(), downsample, selectedObject.getROI())
boolean setROI = true

def imp = IJExtension.extractROIWithOverlay(
    getCurrentServer(),
    selectedObject,
    getCurrentHierarchy(),
    request,
    setROI,
    getCurrentViewer().getOverlayOptions()
    ).getImage()
 
 imp.show()