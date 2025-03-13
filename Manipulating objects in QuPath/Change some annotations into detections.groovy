// https://groups.google.com/forum/#!topic/qupath-users/rKHqWQHhaEE
// Get all annotations with ellipse ROIs
def annotations = getAnnotationObjects()

//change this line to subset = annotations if you want to convert ALL current annotations to detections. Otherwise adjust as desired.
def subset = annotations.findAll {it.getROI() instanceof qupath.lib.roi.EllipseROI}


// Create corresponding detections (name this however you like)
def classification = getPathClass('Node')
def detections = subset.collect {
    new qupath.lib.objects.PathDetectionObject(it.getROI(), classification)
}


// Remove ellipse annotations & replace with detections
removeObjects(subset, true)
addObjects(detections)