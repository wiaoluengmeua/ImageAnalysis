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

    // Find the parent annotation "DAPI +15µm"
    def dapiParent = annotations.find { annotation ->
        annotation.getName() == "DAPI +15µm"
    }

    // Find the parent annotation "Opal 620"
    def opal620Parent = annotations.find { annotation ->
        annotation.getName() == "Opal 620"
    }
    
    // Find the parent annotation "Opal 520"
    def opal520Parent = annotations.find { annotation ->
        annotation.getName() == "Opal 520"
    }

    // Ensure the three parent annotations exist
    if (dapiParent && opal620Parent && opal520Parent) {
        // Get child annotations under "DAPI +15µm"
        def dapiChildren = dapiParent.getChildObjects().findAll { it.isAnnotation() }

        // Get the child annotation under "Opal 620"
        def opal620Child = opal620Parent.getChildObjects().find { it.isAnnotation() }

        // Get the child annotation under "Opal 520"
        def opal520Child = opal520Parent.getChildObjects().find { it.isAnnotation() }

        // Check that each "Opal" parent has at least one child annotation
        if (opal620Child && opal520Child) {
            // For each DAPI child, compute the triple overlap with Opal 620 & Opal 520
            dapiChildren.each { dapiChild ->
                
                // First, overlap with Opal 620 child
                def overlap620 = RoiTools.intersection(dapiChild.getROI(), opal620Child.getROI())
                if (overlap620) {
                    // Then, overlap the result with Opal 520 child
                    def tripleOverlap = RoiTools.intersection(overlap620, opal520Child.getROI())
                    if (tripleOverlap) {
                        // Create a new annotation for the triple overlap
                        def overlapAnnotation = PathObjects.createAnnotationObject(tripleOverlap)
                        
                        // Set a name for the overlap annotation
                        // e.g. "DAPI Cell 1 ∩ Opal 620 ∩ Opal 520"
                        overlapAnnotation.setName("${dapiChild.getName()} ∩ ${opal620Child.getName() ?: 'Opal 620'} ∩ ${opal520Child.getName() ?: 'Opal 520'}")
                        overlapAnnotation.setPathClass(getPathClass("Overlap"))

                        // Add the triple-overlap annotation as a child of the DAPI child
                        dapiChild.addChildObject(overlapAnnotation)
                        println "INFO: Created triple-overlap annotation: ${overlapAnnotation.getName()}"
                    }
                }
            }

            // Update the hierarchy so changes are visible
            fireHierarchyUpdate()
            println("Triple-overlap annotations created for image: ${entry.getImageName()}!")
        } else {
            println("No child annotation found under 'Opal 620' or 'Opal 520' in image: ${entry.getImageName()}!")
        }
    } else {
        println("Parent annotation 'DAPI +15µm', 'Opal 620', or 'Opal 520' not found in image: ${entry.getImageName()}!")
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
