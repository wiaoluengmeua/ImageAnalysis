// Import necessary classes
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import ij.plugin.frame.RoiManager
import qupath.imagej.tools.IJTools

// Get the project
def project = getProject()
if (project == null) {
    print 'No project open!'
    return
}

// Loop through each image in the project
for (entry in project.getImageList()) {
    // Print the image name
    println "Processing image: ${entry.getImageName()}"

    // Load the image data
    def imageData = entry.readImageData()
    if (imageData == null) {
        print "Could not load image data for entry: " + entry.getImageName()
        continue
    }

    // Set the image data as the current image for scripting
    setBatchProjectAndImage(project, imageData)

    // Get the annotations for the image
    def annotations = getAnnotationObjects()
    if (annotations.isEmpty()) {
        print "No annotations found for image: " + entry.getImageName()
        continue
    }

    def roiMan = new RoiManager(false)
    double x = 0
    double y = 0
    double downsample = 1 // Adjust if needed

    annotations.each {
        def roi = IJTools.convertToIJRoi(it.getROI(), x, y, downsample)
        roiMan.addRoi(roi)
    }

    // Define the path where you want to save the ROI file
    def imageName = entry.getImageName().replaceAll("\\.", "_")
    def path = "C:/Users/M198507/Downloads/" + imageName + "_rois.zip"

    roiMan.runCommand("Save", path)
    print "Saved ROIs for image: " + entry.getImageName()

    // Clear the image data from memory after saving
    imageData = null
    System.gc()  // Call the garbage collector to free up memory

    // Optionally, clear the selection for the next iteration
    deselectAll()
}

// Reset the batch project and image
resetBatchProjectAndImage()
