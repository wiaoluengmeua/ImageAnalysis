// Import necessary classes
import qupath.lib.projects.Projects
import qupath.lib.objects.PathObject

// Get the project
def project = getProject()
if (project == null) {
    println("No project loaded!")
    return
}

/**
 * Recursively rename child objects.
 * 
 * @param object     The current PathObject (annotation or any hierarchy object).
 * @param parentPath A list of integers representing the hierarchy path so far.
 */
def renameHierarchy(PathObject object, List parentPath) {
    // Skip renaming for root's direct children (level 1)
    if (!parentPath.isEmpty()) {
        // Construct the name: ParentName-{last segment of parentPath}
        def parent = object.getParent()
        if (parent != null) {
            def baseName = parent.getName()
            def suffix = parentPath.last()
            object.setName("${baseName}-${suffix}")
        }
    }

    // Process each child with an updated path
    def children = object.getChildObjects()
    children.eachWithIndex { child, index ->
        // Extend the path by the child index (index + 1)
        def childPath = parentPath + (index + 1)
        renameHierarchy(child, childPath)
    }
}

// Loop through all images in the project
for (entry in project.getImageList()) {
    println "Processing image: ${entry.getImageName()}"

    // Open the image data
    def imageData = entry.readImageData()
    if (imageData == null) {
        println "Failed to load image data for ${entry.getImageName()}"
        continue
    }

    // Make this image “active” for scripting
    setBatchProjectAndImage(project, imageData)

    // Get the hierarchy & root object
    def hierarchy = getCurrentHierarchy()
    def root = hierarchy.getRootObject()
    if (root == null) {
        println "No root object found for ${entry.getImageName()}"
        continue
    }

    // Rename each top-level object
    root.getChildObjects().eachWithIndex { child, index ->
        // Start recursive renaming with an empty path
        renameHierarchy(child, [])
    }

    // Save changes back to the project
    entry.saveImageData(imageData)

    // Optionally clear memory (helpful if many images)
    imageData = null
    System.gc()
}

// Reset image/project references
resetBatchProjectAndImage()
println "Processing complete!"
