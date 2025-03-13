import org.locationtech.jts.algorithm.MinimumBoundingCircle
import qupath.lib.roi.GeometryTools
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.projects.Projects
import static qupath.lib.gui.scripting.QPEx.*

// Define the radius increments *in micrometers* (NOT in pixels)
def radiusIncrementsMicrons = [0, 2, 4, 6]

// Get the project
def project = getProject()
if (project == null) {
    println "No project loaded!"
    return
}

// Loop through all images in the project
for (entry in project.getImageList()) {
    println "Processing image: ${entry.getImageName()}"

    // Load the image data
    def imageData = entry.readImageData()
    if (imageData == null) {
        println "Failed to load image data for ${entry.getImageName()}"
        continue
    }

    // Set the current image for scripting
    setBatchProjectAndImage(project, imageData)

    // Retrieve pixel size (assumes isotropic pixels)
    // If your images have different pixelWidth & pixelHeight, you may need to handle that separately
    def server = imageData.getServer()
    double pixelWidth = server.getPixelCalibration().getPixelWidthMicrons()
    if (pixelWidth <= 0) {
        println "WARNING: Pixel size could not be determined or is zero. Using pixel increments instead."
        pixelWidth = 1.0  // fallback to avoid division by zero
    }

    // Convert the micrometer increments into pixel increments
    def radiusIncrementsPixels = radiusIncrementsMicrons.collect { incrementUm ->
        incrementUm / pixelWidth
    }

    // Get all annotations in the current image
    def allAnnotations = getAnnotationObjects()

    // Filter for parent annotations (those with child annotations and name containing "DAPI")
    def parentAnnotations = allAnnotations.findAll { 
        it.getChildObjects()?.size() > 0 && (it.getName()?.contains("Opal 520") ?: false)
    }

    println "INFO: Found ${parentAnnotations.size()} parent annotations with 'DAPI' in their name."

    // Process each parent annotation
    parentAnnotations.each { parent ->
        def parentName = parent.getName() ?: "Unnamed Annotation"
        println "INFO: Processing Parent Annotation: ${parentName}"

        // Get child annotations
        def childAnnotations = parent.getChildObjects().findAll { it.isAnnotation() }

        if (childAnnotations.isEmpty()) {
            println "INFO: No child annotations found for parent: ${parentName}"
            return
        }

        // Create a new parent annotation for each radius increment (microns)
        def newParentAnnotations = [:] // Map to store parent annotations for each increment

        // For display/labeling, we'll keep using the original micrometer values in the name
        radiusIncrementsMicrons.each { incrementUm ->
            // Create a placeholder parent annotation for this increment
            def placeholderROI = ROIs.createRectangleROI(0, 0, 1, 1, parent.getROI().getImagePlane())
            def newParentAnnotation = PathObjects.createAnnotationObject(placeholderROI)
            newParentAnnotation.setName("${parentName} +${incrementUm}µm")
            newParentAnnotations[incrementUm] = newParentAnnotation
        }

        // Process each child annotation
        childAnnotations.each { child ->
            try {
                def geom = child.getROI().getGeometry()
                def mbc = new MinimumBoundingCircle(geom)

                // Get the center and radius of the MBC (in pixels)
                def center = mbc.getCentre()
                def radius = mbc.getRadius()

                if (center != null && radius > 0) {
                    def imagePlane = child.getROI().getImagePlane()

                    // Create concentric circles and add them to the respective parent annotation
                    for (int i = 0; i < radiusIncrementsPixels.size(); i++) {
                        def incrementUm = radiusIncrementsMicrons[i]
                        def incrementPx = radiusIncrementsPixels[i]

                        def newRadiusPx = radius + incrementPx
                        def newCircleROI = ROIs.createEllipseROI(
                            center.x - newRadiusPx, // Top-left X in pixels
                            center.y - newRadiusPx, // Top-left Y in pixels
                            2 * newRadiusPx,        // Width in pixels
                            2 * newRadiusPx,        // Height in pixels
                            imagePlane
                        )

                        // Create an annotation for the new circle
                        def newCircleAnnotation = PathObjects.createAnnotationObject(newCircleROI)
                        newCircleAnnotation.setName("${child.getName() ?: 'Child'}_Circle+${incrementUm}µm")

                        // Add the new circle annotation under the corresponding parent annotation
                        newParentAnnotations[incrementUm].addChildObject(newCircleAnnotation)
                        println "  INFO: Created concentric circle annotation: ${newCircleAnnotation.getName()} under ${newParentAnnotations[incrementUm].getName()}"
                    }
                } else {
                    println "  ERROR: Invalid MBC for child annotation: ${child.getName()}"
                }
            } catch (Exception e) {
                println "  ERROR: Error processing child annotation: ${child.getName()} - ${e.message}"
            }
        }

        // Add all new parent annotations to the hierarchy
        newParentAnnotations.values().each { addObject(it) }
        println "INFO: Created new parent annotations for ${parentName}: ${newParentAnnotations.keySet()}"
    }

    // Save the updated image data
    entry.saveImageData(imageData)

    // Clear the image data from memory
    imageData = null
    System.gc()
}

// Reset project and image batch mode
resetBatchProjectAndImage()

println "INFO: Processing completed for all project images!"
