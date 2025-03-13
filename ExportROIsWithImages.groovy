// Import necessary classes
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.imagej.tools.IJTools
import ij.plugin.frame.RoiManager
import ij.io.FileSaver
import ij.ImagePlus
import qupath.lib.images.servers.ImageServer
import qupath.lib.regions.RegionRequest

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

    // Get the image server
    def server = getCurrentServer()

    // Index for annotation numbering
    int roiIndex = 0

    // Process each annotation
    annotations.each { annotation ->
        roiIndex++
        def roi = annotation.getROI()

        // Create a region request for the ROI at full resolution (downsample = 1)
        def requestROI = RegionRequest.createInstance(server.getPath(), 1, roi)

        // Define the path where you want to save the image
        def imageName = entry.getImageName().replaceAll("\\.", "_")
        def annotationName = annotation.getName() != null ? annotation.getName() : "Annotation_${roiIndex}"
        def imagePath = "C:/Users/M198507/Downloads/${imageName}_${annotationName}.tif"

        // Write the image region directly to a file
        writeImageRegion(server, requestROI, imagePath)

        // Convert the ROI to ImageJ ROI and save it
        def roiIJ = IJTools.convertToIJRoi(roi, 0, 0, 1)
        def roiMan = new RoiManager(false)
        roiMan.addRoi(roiIJ)
        def roiPath = "C:/Users/M198507/Downloads/${imageName}_${annotationName}_ROI.zip"
        roiMan.runCommand("Save", roiPath)

        print "Saved image and ROI for annotation ${annotationName} in image: " + entry.getImageName()
    }

    // Clear the image data from memory
    imageData = null
    System.gc()

    // Optionally, clear the selection for the next iteration
    deselectAll()
}

// Reset the batch project and image
resetBatchProjectAndImage()
