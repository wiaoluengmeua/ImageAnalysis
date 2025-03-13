//QuPath 0.2.3+
// https://forum.image.sc/t/qupath-distance-between-annotations/47960/2
// Get the objects to compare
// This assumes that just *one* annotation has each specified name
def hifPos = getAnnotationObjects().find {it.getName() == 'HIF-positive area 2'}
def pimoPos = getAnnotationObjects().find {it.getName() == 'PIMO-positive area 2'}

// Make sure we're on the same plane (only really relevant for z-stacks, time series)
def plane = hifPos.getROI().getImagePlane()
if (plane != pimoPos.getROI().getImagePlane()) {
    println 'Annotations are on different planes!'
    return    
}

// Convert to geometries & compute distance
// Note: see https://locationtech.github.io/jts/javadoc/org/locationtech/jts/geom/Geometry.html#distance-org.locationtech.jts.geom.Geometry-
def g1 = hifPos.getROI().getGeometry()
def g2 = pimoPos.getROI().getGeometry()
double distancePixels = g1.distance(g2)
println "Distance between annotations: ${distancePixels} pixels"

// Attempt conversion to calibrated units
def cal = getCurrentServer().getPixelCalibration()
if (cal.pixelWidth != cal.pixelHeight) {
    println "Pixel width != pixel height ($cal.pixelWidth vs. $cal.pixelHeight)"
    println "Distance measurements will be calibrated using the average of these"
}
double distanceCalibrated = distancePixels * cal.getAveragedPixelSize()
println "Distance between annotations: ${distanceCalibrated} ${cal.pixelWidthUnit}"

// Check intersection as well
def intersection = g1.intersection(g2)
if (intersection.isEmpty())
    println "No intersection between areas"
else {
    def roi = GeometryTools.geometryToROI(intersection, plane)
    def annotation = PathObjects.createAnnotationObject(roi, getPathClass('Intersection'))
    addObject(annotation)
    selectObjects(annotation)
    println "Annotated created for intersection"
}