/**
 * Create a region annotation with a fixed size in QuPath, based on the current viewer location.
 * 0.1.2
 * @author Pete Bankhead
 */

import qupath.lib.objects.PathAnnotationObject
import qupath.lib.objects.classes.PathClassFactory
import qupath.lib.roi.RectangleROI
import qupath.lib.scripting.QPEx

// Define the size of the region to create
double sizeMicrons = 200.0

// Get main data structures
def imageData = QPEx.getCurrentImageData()
def server = imageData.getServer()

// Convert size in microns to pixels - QuPath ROIs are defined in pixel units of the full-resolution image
int sizePixels = Math.round(sizeMicrons / server.getAveragedPixelSizeMicrons())

// Get the current viewer & the location of the pixel currently in the center
def viewer = QPEx.getCurrentViewer()
double cx = viewer.getCenterPixelX()
double cy = viewer.getCenterPixelY()

// Create a new Rectangle ROI
def roi = new RectangleROI(cx-sizePixels/2, cy-sizePixels/2, sizePixels, sizePixels)

// Create & new annotation & add it to the object hierarchy
def annotation = new PathAnnotationObject(roi, PathClassFactory.getPathClass("Region"))
imageData.getHierarchy().addPathObject(annotation, false)