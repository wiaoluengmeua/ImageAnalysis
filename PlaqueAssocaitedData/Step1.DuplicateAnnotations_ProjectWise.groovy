// Import necessary classes
import qupath.lib.objects.PathObjects
import qupath.lib.projects.Projects

// Get the project
def project = getProject()
if (project == null) {
    println("No project loaded!")
    return
}

// Loop through each image in the project
for (entry in project.getImageList()) {
    println("Processing image: ${entry.getImageName()}")

    // Open the image data
    def imageData = entry.readImageData()
    if (imageData == null) {
        println("Failed to load image data for ${entry.getImageName()}")
        continue
    }

    // Set the image data as the current image for scripting
    setBatchProjectAndImage(project, imageData)

    // Get all annotation objects
    def annotations = getAnnotationObjects().findAll { it.isAnnotation() }

    if (annotations.isEmpty()) {
        println("No annotations found in image: ${entry.getImageName()}")
        continue
    }

    // Loop through each annotation and perform duplication and renaming
    annotations.each { annotation ->
        selectObjects([annotation]) // Select the annotation

        // Duplicate the annotation twice
        duplicateSelectedAnnotations() // First duplication
        duplicateSelectedAnnotations() // Second duplication

        // Get all annotations again to access the duplicates
        def updatedAnnotations = getAnnotationObjects().findAll { it.isAnnotation() }

        // Find the newly created duplicates (last two annotations)
        def annotationIndex = updatedAnnotations.indexOf(annotation)
        def duplicates = updatedAnnotations.subList(annotationIndex + 1, annotationIndex + 3)

        if (duplicates.size() == 2) {
            def duplicate1 = duplicates[0]
            def duplicate2 = duplicates[1]

            // Rename original and duplicates
            annotation.setName("DAPI")
            duplicate1.setName("Opal 620")
            duplicate2.setName("Opal 520")

            // Change the color of the first duplicate (Opal 620) to red
            duplicate1.setColor(255, 0, 0) // RGB for red

            // Print areas of the annotations
            println("Annotation processed for ${entry.getImageName()}:")
            println(" - Original (DAPI) area: ${annotation.getROI()?.getArea()}")
            println(" - Duplicate 1 (Opal 620) area: ${duplicate1.getROI()?.getArea()}")
            println(" - Duplicate 2 (Opal 520) area: ${duplicate2.getROI()?.getArea()}")
        } else {
            println("Failed to create duplicates for annotation in ${entry.getImageName()}.")
        }
    }

    // Save the updated image data back to the project
    entry.saveImageData(imageData)

    // Clear the image data from memory after saving
    imageData = null
    System.gc() // Call the garbage collector to free up memory
}

// Reset the batch project and image
resetBatchProjectAndImage()

println("Processing complete!")
