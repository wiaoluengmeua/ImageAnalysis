import qupath.lib.roi.RoiTools
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import static qupath.lib.gui.scripting.QPEx.*

// Define the DAPI parent names to loop through
def dapiParentNames = [
    "DAPI +0µm",
    "DAPI +10µm",
    "DAPI +15µm",
    "DAPI +20µm",
    "DAPI +25µm",
    "DAPI +30µm"
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
    def opal620Parent = annotations.find { annotation ->
        annotation.getName() == "Opal 620"
    }
    
    // Find the parent annotation "Opal 520"
    def opal520Parent = annotations.find { annotation ->
        annotation.getName() == "Opal 520"
    }

    // Check that Opal parent annotations exist
    if (!opal620Parent || !opal520Parent) {
        println("Parent annotation 'Opal 620' or 'Opal 520' not found in image: ${entry.getImageName()}!")
        // Save & skip if we can't find these
        entry.saveImageData(imageData)
        imageData = null
        System.gc()
        continue
    }

    // Get the child annotation under "Opal 620"
    def opal620Child = opal620Parent.getChildObjects().find { it.isAnnotation() }
    // Get the child annotation under "Opal 520"
    def opal520Child = opal520Parent.getChildObjects().find { it.isAnnotation() }

    if (!opal620Child || !opal520Child) {
        println("No child annotation found under 'Opal 620' or 'Opal 520' in image: ${entry.getImageName()}!")
        // Save & skip if we can't find a child annotation
        entry.saveImageData(imageData)
        imageData = null
        System.gc()
        continue
    }

    // Now loop through each DAPI parent name
    dapiParentNames.each { dapiName ->
        // Find the DAPI parent for this name
        def dapiParent = annotations.find { annotation ->
            annotation.getName() == dapiName
        }

        // If not found, just skip it
        if (!dapiParent) {
            println("Parent annotation '${dapiName}' not found in image: ${entry.getImageName()}!")
            return
        }

        // Get child annotations under this DAPI parent
        def dapiChildren = dapiParent.getChildObjects().findAll { it.isAnnotation() }
        if (!dapiChildren) {
            println("No child annotations found under '${dapiName}' in image: ${entry.getImageName()}!")
            return
        }

        // For each DAPI child annotation, compute the triple overlap
        dapiChildren.each { dapiChild ->
            // Overlap with Opal 620 child
            def overlap620 = RoiTools.intersection(dapiChild.getROI(), opal620Child.getROI())
            if (overlap620) {
                // Overlap that result with Opal 520 child
                def tripleOverlap = RoiTools.intersection(overlap620, opal520Child.getROI())
                if (tripleOverlap) {
                    // Create a new annotation for the triple overlap
                    def overlapAnnotation = PathObjects.createAnnotationObject(tripleOverlap)
                    // Name the overlap annotation
                    overlapAnnotation.setName(
                        "${dapiChild.getName() ?: dapiName} ∩ " +
                        "${opal620Child.getName() ?: 'Opal 620'} ∩ " +
                        "${opal520Child.getName() ?: 'Opal 520'}"
                    )
                    overlapAnnotation.setPathClass(getPathClass("Overlap"))
                    
                    // Add the triple-overlap annotation as a child of the DAPI child
                    dapiChild.addChildObject(overlapAnnotation)
                    println "INFO: Created triple-overlap annotation: ${overlapAnnotation.getName()}"
                }
            }
        }
    }

    // Update the hierarchy so changes are visible
    fireHierarchyUpdate()
    println("Triple-overlap annotations created (where applicable) for image: ${entry.getImageName()}!")

    // Save the updated image data
    entry.saveImageData(imageData)

    // Clear the image data to free up memory
    imageData = null
    System.gc()
}

// Reset project and image batch mode
resetBatchProjectAndImage()

println("Processing completed for all project images!")
