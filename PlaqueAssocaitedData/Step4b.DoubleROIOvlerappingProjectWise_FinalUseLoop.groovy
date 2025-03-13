import qupath.lib.roi.RoiTools
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import static qupath.lib.gui.scripting.QPEx.*

// Define the Opal 520 parent names you want to process
def dapiParentNames = [
    "Opal 520 +0µm",
    "Opal 520 +2µm",
    "Opal 520 +4µm",
    "Opal 520 +6µm"
]

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

    // Find the parent annotation "Opal 620"
    def opalParent = annotations.find { annotation ->
        annotation.getName() == "Opal 620"
    }

    // Check if "Opal 620" parent exists
    if (!opalParent) {
        println("Parent annotation 'Opal 620' not found in image: ${entry.getImageName()}!")
        // We can't do overlaps without Opal 620, so save & skip
        entry.saveImageData(imageData)
        imageData = null
        System.gc()
        continue
    }

    // Find the child annotation under "Opal 620"
    def opalChild = opalParent.getChildObjects().find { it.isAnnotation() }

    if (!opalChild) {
        println("No child annotation found under 'Opal 620' in image: ${entry.getImageName()}!")
        // Save & skip if we can't find a child annotation
        entry.saveImageData(imageData)
        imageData = null
        System.gc()
        continue
    }

    // Now loop through each Opal 520 parent name
    dapiParentNames.each { dapiName ->
        // Find the Opal 520 parent for this name
        def dapiParent = annotations.find { annotation ->
            annotation.getName() == dapiName
        }

        if (!dapiParent) {
            // Not found – just log and move on to the next
            println("Parent annotation '${dapiName}' not found in image: ${entry.getImageName()}!")
            return
        }

        // Get child annotations under the Opal 520 parent
        def dapiChildren = dapiParent.getChildObjects().findAll { it.isAnnotation() }
        if (!dapiChildren) {
            println("No child annotations found under '${dapiName}' in image: ${entry.getImageName()}!")
            return
        }

        // Compute overlap for each Opal 520 child with the Opal 620 child
        dapiChildren.each { dapiChild ->
            def intersectionROI = RoiTools.intersection(dapiChild.getROI(), opalChild.getROI())

            if (intersectionROI) {
                // Create a new annotation for the overlap
                def overlapAnnotation = PathObjects.createAnnotationObject(intersectionROI)
                overlapAnnotation.setName("${dapiChild.getName()} ∩ ${opalChild.getName() ?: 'Opal 620'}")
                overlapAnnotation.setPathClass(getPathClass("Overlap"))

                // Add the overlap annotation as a child of the Opal 520 child
                dapiChild.addChildObject(overlapAnnotation)
                println "INFO: Created overlap annotation: ${overlapAnnotation.getName()}"
            }
        }
    }

    // Update the hierarchy so changes are visible
    fireHierarchyUpdate()
    println("Overlap annotations created for image: ${entry.getImageName()}!")

    // Save the updated image data
    entry.saveImageData(imageData)

    // Clear the image data to free up memory
    imageData = null
    System.gc()
}

// Reset project and image batch mode
resetBatchProjectAndImage()

println("Processing completed for all project images!")
