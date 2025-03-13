import qupath.lib.objects.PathCellObject

// Get the current hierarchy
def hierarchy = getCurrentHierarchy()

// Get the non-top level annotations.  This assumes you have a tissue area selected, and have hand drawn some cells within that.

def targets = getObjects{return it.getLevel()!=1 && it.isAnnotation()}


// Check we have anything to work with
if ( targets.isEmpty()) {
    print("No objects selected!")
    return
}

// Loop through objects
def newDetections = new ArrayList<>()
for (def pathObject in  targets) {

    // Unlikely to happen... but skip any objects not having a ROI
    if (!pathObject.hasROI()) {
        print("Skipping object without ROI: " + pathObject)
        continue
    }
    def roi = pathObject.getROI()
    def detection = new PathCellObject(roi,roi,pathObject.getPathClass())
    newDetections.add(detection)
    print("Adding " + detection)
}
removeObjects( targets, true)
// Actually add the objects
hierarchy.addPathObjects(newDetections, false)
//Remove nucleus ROI
newDetections.each{it.nucleus = null}
fireHierarchyUpdate()
if (newDetections.size() > 1)
    print("Added " + newDetections.size() + " detections(s)")

