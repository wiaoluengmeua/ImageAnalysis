//0.1.2 and 0.2.0

import qupath.lib.roi.RectangleROI
import qupath.lib.objects.PathAnnotationObject

// Size in pixels at the base resolution
// note that the actual size will be one pixel larger in each dimension
int size = 255

// Get center pixel
def viewer = getCurrentViewer()
int cx = viewer.getCenterPixelX()
int cy = viewer.getCenterPixelY()

// Create & add annotation
def roi = new RectangleROI(cx-size/2, cy-size/2, size, size)
def rgb = getColorRGB(50, 50, 200)
def pathClass = getPathClass('Other', rgb)
def annotation = new PathAnnotationObject(roi, pathClass)
addObject(annotation)