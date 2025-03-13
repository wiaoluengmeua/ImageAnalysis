import qupath.lib.images.ImageData

def project = getProject()
if (project == null) {
    println "No project loaded!"
    return
}

// Loop through each image in the project
for (entry in project.getImageList()) {
    println "Loading image data for: ${entry.getImageName()}"
    def imageData = entry.readImageData()
    if (imageData == null) {
        println "Could not read image data!"
        continue
    }

    // Set the image type here, e.g. to Fluorescence:
    imageData.setImageType(ImageData.ImageType.FLUORESCENCE)

    // Alternatively, for brightfield:
    // imageData.setImageType(ImageData.ImageType.BRIGHTFIELD)

    // Save changes back to the project
    entry.saveImageData(imageData)
}

// If you want to ensure the project is fully synced:
project.syncChanges()
println "Done setting image types!"
