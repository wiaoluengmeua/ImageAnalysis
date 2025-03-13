// Get the current project
def project = getProject()
if (project == null) {
    println "No project loaded!"
    return
}

// Loop through each image in the project
for (entry in project.getImageList()) {
    println "-----"
    println "Image name: ${entry.getImageName()}"

    // Read (load) the image data
    def imageData = entry.readImageData()
    if (imageData == null) {
        println "Failed to load image data for ${entry.getImageName()}"
        continue
    }

    // Print the image type
    println "Image type: ${imageData.getImageType()}"
    
    // Optionally free resources
    imageData = null
    System.gc()
}

println "Done!"
