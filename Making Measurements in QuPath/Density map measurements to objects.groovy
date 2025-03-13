//0.3.2
//TWO scripts, make sure to only take the one you want.

//https://gist.github.com/petebankhead/2e7325d8c560677bba9b867f68070300
/**
 * Script to add density map values to detection centroids in QuPath v0.3.
 * 
 * Note that this hasn't been tested very much... and assumes a 2D image.
 * At the very least, you should use 'Measure -> Show measurement maps' as a sanity check.
 *
 * Written for https://forum.image.sc/t/qupath-number-of-detections-per-tile/64603/10
 *
 * @author Pete Bankhead
 */

String densityMapName = 'Tumor density map' // You'll need a saved density map with this name in the project
String densityMeasurementName = densityMapName // Make this more meaningful if needed

// Get the current image
def imageData = getCurrentImageData()

// Load a density map builder & create an ImageServer from it
def builder = loadDensityMap(densityMapName)
def server = builder.buildServer(imageData)

// Read the entire density map (we assume it's 2D, and low enough resolution for this to work!)
def request = RegionRequest.createInstance(server)
def img = server.readBufferedImage(request)

// Get all the objects to which we want to add measurements
def pathObjects = getDetectionObjects()
double downsample = request.getDownsample()

// Select the band (channel) of the density map to use
// (there might only be 1... counting starts at 0)
int band = 0

// Add centroid measurement to all objects
pathObjects.parallelStream().forEach { p ->
    int x = (int)(p.getROI().getCentroidX() / downsample)
    int y = (int)(p.getROI().getCentroidY() / downsample)
    float val = img.getRaster().getSampleFloat(x, y, band)
    try (def ml = p.getMeasurementList()) {
        ml.putMeasurement(densityMeasurementName, val)
    }
}

// Finish up
fireHierarchyUpdate()
println 'Done!'



//https://gist.github.com/petebankhead/6286adcea24dd73af83e822bdb7a2132

/**
 * Script to add density map values to *some* detection centroids in QuPath v0.3,
 * limited to only use a subset of the detections on the image.
 *
 * It does this by copying the relevant objects and adding them to a temporary ImageData.
 * 
 * Note that this hasn't been tested very much... and assumes a 2D image.
 * At the very least, you should use 'Measure -> Show measurement maps' as a sanity check.
 *
 * Written for https://forum.image.sc/t/qupath-number-of-detections-per-tile/64603/14
 *
 * @see https://gist.github.com/petebankhead/2e7325d8c560677bba9b867f68070300
 *
 * @author Pete Bankhead
 */

String densityMapName = 'Tumor density map' // You'll need a saved density map with this name in the project
String densityMeasurementName = 'Some useful name' // Make this more meaningful if needed

// Get the current image
def imageData = getCurrentImageData()
def hierarchy = imageData.getHierarchy()

// Get the parent objects we care about
def parentAnnotations = getSelectedObjects()

// Alternatively, define parent objects using all annotations with a specified class
// String parentClass = 'Tumor'
//def parentAnnotations = getAnnotationObjects().findAll { p ->
//    return p.isAnnotation() && p.getPathClass() == getPathClass(parentClass)
//}

// Get all the detections that fall inside the parent annotations
def pathObjects = new HashSet<>()
for (def parent in parentAnnotations) {
    def contained = hierarchy.getObjectsForROI(null, parent.getROI()).findAll {p -> p.isDetection()}
    pathObjects.addAll(contained)
}

// Add a clone of the detections to a new, temporary object hierarchy 
// (and a new, temporary ImageData)
def imageDataTemp = new qupath.lib.images.ImageData(imageData.getServer())
def hierarchyTemp = imageDataTemp.getHierarchy()
def clonedObjects = pathObjects.collect { p -> PathObjectTools.transformObject(p, null, false) }
hierarchyTemp.addPathObjects(clonedObjects)

// Load a density map builder & create an ImageServer from it
def builder = loadDensityMap(densityMapName)
def server = builder.buildServer(imageDataTemp)

// Read the entire density map (we assume it's 2D, and low enough resolution for this to work!)
def request = RegionRequest.createInstance(server)
def img = server.readBufferedImage(request)
double downsample = request.getDownsample()

// Select the band (channel) of the density map to use
// (there might only be 1... counting starts at 0)
int band = 0

// Add centroid measurement to all objects
pathObjects.parallelStream().forEach { p ->
    int x = (int)(p.getROI().getCentroidX() / downsample)
    int y = (int)(p.getROI().getCentroidY() / downsample)
    float val = img.getRaster().getSampleFloat(x, y, band)
    try (def ml = p.getMeasurementList()) {
        ml.putMeasurement(densityMeasurementName, val)
    }
}

// Finish up
fireHierarchyUpdate()
println 'Done!'

