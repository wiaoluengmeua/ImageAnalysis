//0.2.2
//https://forum.image.sc/t/script-for-send-region-to-imagej/39554/18

import ij.IJ
import qupath.imagej.gui.IJExtension

double downsample = 4.0

def server = getCurrentServer()
def request = RegionRequest.createInstance(server, downsample)
boolean setROI = false

def imp = IJExtension.extractROIWithOverlay(
    getCurrentServer(),
    null,
    getCurrentHierarchy(),
    request,
    setROI,
    getCurrentViewer().getOverlayOptions()
    ).getImage()
 
IJ.save(imp, '/path/to/export.tif')