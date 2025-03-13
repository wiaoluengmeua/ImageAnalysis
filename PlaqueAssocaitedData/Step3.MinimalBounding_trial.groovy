import org.locationtech.jts.algorithm.MinimumBoundingCircle
import qupath.lib.roi.GeometryTools
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs

// Define the radius increments
def radiusIncrements = [0, 10, 15, 20, 25]

// Get all annotations in the current image
def allAnnotations = getAnnotationObjects()

// Filter for parent annotations (those with child annotations and name containing "DAPI")
def parentAnnotations = allAnnotations.findAll { 
    it.getChildObjects()?.size() > 0 && (it.getName()?.contains("DAPI") ?: false)
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

    // Create a new parent annotation for each radius increment
    def newParentAnnotations = [:] // Map to store parent annotations for each increment

    radiusIncrements.each { increment ->
        // Create a placeholder parent annotation for this increment
        def placeholderROI = ROIs.createRectangleROI(0, 0, 1, 1, parent.getROI().getImagePlane())
        def newParentAnnotation = PathObjects.createAnnotationObject(placeholderROI)
        newParentAnnotation.setName("${parentName} +${increment}")
        newParentAnnotations[increment] = newParentAnnotation
    }

    // Process each child annotation
    childAnnotations.each { child ->
        try {
            def geom = child.getROI().getGeometry()
            def mbc = new MinimumBoundingCircle(geom)

            // Get the center and radius of the MBC
            def center = mbc.getCentre()
            def radius = mbc.getRadius()

            if (center != null && radius > 0) {
                def imagePlane = child.getROI().getImagePlane()

                // Create concentric circles and add them to the respective parent annotation
                radiusIncrements.each { increment ->
                    def newRadius = radius + increment
                    def newCircleROI = ROIs.createEllipseROI(
                        center.x - newRadius, // Top-left X
                        center.y - newRadius, // Top-left Y
                        2 * newRadius,        // Width
                        2 * newRadius,        // Height
                        imagePlane
                    )
                    
                    // Create an annotation for the new circle
                    def newCircleAnnotation = PathObjects.createAnnotationObject(newCircleROI)
                    newCircleAnnotation.setName("${child.getName() ?: 'Child'}_Circle+${increment}")
                    
                    // Add the new circle annotation under the corresponding parent annotation
                    newParentAnnotations[increment].addChildObject(newCircleAnnotation)
                    println "  INFO: Created concentric circle annotation: ${newCircleAnnotation.getName()} under ${newParentAnnotations[increment].getName()}"
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

println "INFO: Processing completed!"
