import qupath.lib.roi.RoiTools
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import static qupath.lib.gui.scripting.QPEx.*

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

    // Get all annotations in the current image
    def annotations = getAnnotationObjects()

    // Find the parent annotation "DAPI +15"
    def dapiParent = annotations.find { annotation ->
        annotation.getName() == "DAPI +15"
    }

    // Find the parent annotation "Opal 620"
    def opalParent = annotations.find { annotation ->
        annotation.getName() == "Opal 620"
    }

    // Ensure both parent annotations exist
    if (dapiParent && opalParent) {
        // Get child annotations under "DAPI +15"
        def dapiChildren = dapiParent.getChildObjects().findAll { it.isAnnotation() }
        
        // Get the child annotation under "Opal 620"
        def opalChild = opalParent.getChildObjects().find { it.isAnnotation() }
        
        if (opalChild) {
            // Create a new parent annotation to hold all overlaps
            def imagePlane = dapiParent.getROI().getImagePlane()
            def placeholderROI = ROIs.createRectangleROI(0, 0, 1, 1, imagePlane) // Placeholder ROI
            def newParentAnnotation = PathObjects.createAnnotationObject(placeholderROI)
            newParentAnnotation.setName("Overlap Parent: DAPI +15 ∩ Opal 620")
            newParentAnnotation.setPathClass(getPathClass("Overlap Parent"))

            // Iterate over each DAPI child and compute overlap with the Opal child
            dapiChildren.each { dapiChild ->
                def intersectionROI = RoiTools.intersection(dapiChild.getROI(), opalChild.getROI())
                
                if (intersectionROI) {
                    // Create a new annotation for the overlap
                    def overlapAnnotation = PathObjects.createAnnotationObject(intersectionROI)
                    
                    // Set a name for the overlap annotation
                    overlapAnnotation.setName("${dapiChild.getName()} ∩ ${opalChild.getName() ?: 'Annotation'}")
                    overlapAnnotation.setPathClass(getPathClass("Overlap"))
                    
                    // Add the overlap annotation as a child to the new parent annotation
                    newParentAnnotation.addChildObject(overlapAnnotation)
                    println "INFO: Created overlap annotation: ${overlapAnnotation.getName()}"
                }
            }
            
            // Add the new parent annotation to the hierarchy
            getCurrentHierarchy().addObject(newParentAnnotation)
            fireHierarchyUpdate()
            println("Overlap annotations organized under a new parent annotation successfully for image: ${entry.getImageName()}!")
        } else {
            println("No child annotation found under 'Opal 620' in image: ${entry.getImageName()}!")
        }
    } else {
        println("Parent annotation 'DAPI +15' or 'Opal 620' not found in image: ${entry.getImageName()}!")
    }

    // Save the updated image data
    entry.saveImageData(imageData)

    // Clear the image data to free up memory
    imageData = null
    System.gc()
}

// Reset project and image batch mode
resetBatchProjectAndImage()

println("Processing completed for all project images!")
