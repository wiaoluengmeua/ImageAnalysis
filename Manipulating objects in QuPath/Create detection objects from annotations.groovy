/*
 * A script to create detection object(s) having the same ROI as all other annotation objects
 */
import qupath.lib.objects.PathTileObject
import qupath.lib.roi.RectangleROI
import qupath.lib.scripting.QP

// Set this to true to use the bounding box of the ROI, rather than the ROI itself
boolean useBoundingBox = false

// Get the current hierarchy
def hierarchy = QP.getCurrentHierarchy()

// Get all annotation objects: You may want to change this to select only certain annotations based on class
// or just annotations you have selected using =getSelectedObjects()
def selected = getAnnotationObjects()

// Check we have anything to work with
if (selected.isEmpty()) {
    print("No objects selected!")
    return
}

// Loop through objects
def newDetections = new ArrayList<>()
for (def pathObject in selected) {

    // Unlikely to happen... but skip any objects not having a ROI
    if (!pathObject.hasROI()) {
        print("Skipping object without ROI: " + pathObject)
        continue
    }

    // Don't create a second annotation, unless we want a bounding box
    if (!useBoundingBox && pathObject.isDetection()) {
        print("Skipping annotation: " + pathObject)
        continue
    }

    // Create an annotation for whichever object is selected, with the same class
    // Note: because ROIs are (or should be) immutable, the same ROI is used here, rather than a duplicate
    def roi = pathObject.getROI()
    if (useBoundingBox)
        roi = new RectangleROI(
                roi.getBoundsX(),
                roi.getBoundsY(),
                roi.getBoundsWidth(),
                roi.getBoundsHeight(),
                roi.getC(),
                roi.getZ(),
                roi.getT())
    def detection = new PathTileObject(roi, pathObject.getPathClass())
    newDetections.add(detection)
    print("Adding " + detection)
}

// Actually add the objects
hierarchy.addPathObjects(newDetections, false)
if (newDetections.size() > 1)
    print("Added " + newDetections.size() + " detections(s)")