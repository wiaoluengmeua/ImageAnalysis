/* IMPORTS */
import qupath.lib.images.servers.TransformedServerBuilder
import qupath.lib.roi.interfaces.ROI
import qupath.imagej.tools.IJTools
import qupath.lib.images.PathImage
import qupath.lib.regions.RegionRequest
import ij.ImagePlus
import ij.process.ImageProcessor
import qupath.opencv.ml.pixel.PixelClassifiers
import qupath.lib.gui.viewer.OverlayOptions
import qupath.lib.gui.viewer.RegionFilter
import qupath.lib.gui.viewer.overlays.PixelClassificationOverlay
import qupath.lib.images.servers.ColorTransforms.ColorTransform
import qupath.opencv.ops.ImageOp
import qupath.opencv.ops.ImageOps
import qupath.lib.objects.PathObjects
import qupath.lib.projects.Projects

/* PARAMETERS */
double thresholdDownsample = 1 // Downsample for calculating threshold
def threshold = 200 // Threshold method
boolean darkBackground = false // Use dark background threshold adjustment
def thresholdFloor = null // Threshold floor value
String output = "annotation" // Output type: "annotation", "detection", "measurement", "preview", "threshold value"
double classifierDownsample = 1 // Downsample for classifier
double classifierGaussianSigma = 0.5 // Gaussian blur for classifier
double minArea = 5 // Minimum annotation area
double minHoleArea = 0 // Minimum hole area in annotation
String classifierObjectOptions = "" // Options for creating classifier objects

/* MAIN SCRIPT */

// Get the current project
def project = getProject()
if (project == null) {
    println("No project loaded!")
    return
}

// Loop through each image in the project
for (entry in project.getImageList()) {
    println("Processing image: ${entry.getImageName()}")

    // Load image data
    def imageData = entry.readImageData()
    if (imageData == null) {
        println("Failed to load image data for ${entry.getImageName()}")
        continue
    }

    // Set the current image for scripting
    setBatchProjectAndImage(project, imageData)

    // Find all annotations labeled as "Opal 620"
    def dapiAnnotations = getAnnotationObjects().findAll { it.getName() == "Opal 620" }

    if (dapiAnnotations.isEmpty()) {
        println("No 'Opal 620' annotations found in image: ${entry.getImageName()}")
        continue
    }

    // Apply thresholding to each Opal 620 annotation
    dapiAnnotations.forEach { annotation ->
        println("Applying threshold to Opal 620 annotation in ${entry.getImageName()}")

        autoThreshold(annotation, "Opal 620", thresholdDownsample, threshold, darkBackground, thresholdFloor, output, classifierDownsample, classifierGaussianSigma, null, "Positive", minArea, minHoleArea, classifierObjectOptions)
    }

    // Save updated image data
    entry.saveImageData(imageData)

    // Clear the image data to free up memory
    imageData = null
    System.gc()
}

// Reset project and image batch mode
resetBatchProjectAndImage()

println("Processing complete!")


/* FUNCTIONS */
def autoThreshold(annotation, channel, thresholdDownsample, threshold, darkBackground, thresholdFloor, output, classifierDownsample, classifierGaussianSigma, classBelow, classAbove, minArea, minHoleArea, classifierObjectOptions) {
    def qupath = getQuPath()
    def imageData = getCurrentImageData()
    def imageType = imageData.getImageType()
    def server = imageData.getServer()
    def cal = server.getPixelCalibration()
    def resolution = cal.createScaledInstance(classifierDownsample, classifierDownsample)
    def classifierChannel

    if (imageType.toString().contains("Brightfield")) {
        def stains = imageData.getColorDeconvolutionStains()

        if (channel == "HTX") {
            server = new TransformedServerBuilder(server).deconvolveStains(stains, 1).build()
            classifierChannel = ColorTransforms.createColorDeconvolvedChannel(stains, 1)
        } else if (channel == "DAB") {
            server = new TransformedServerBuilder(server).deconvolveStains(stains, 2).build()
            classifierChannel = ColorTransforms.createColorDeconvolvedChannel(stains, 2)
        } else if (channel == "Residual") {
            server = new TransformedServerBuilder(server).deconvolveStains(stains, 3).build()
            classifierChannel = ColorTransforms.createColorDeconvolvedChannel(stains, 3)
        } else if (channel == "Average") {
            server = new TransformedServerBuilder(server).averageChannelProject().build()
            classifierChannel = ColorTransforms.createMeanChannelTransform()
        }
    } else if (imageType.toString() == "Fluorescence") {
        if (channel == "Average") {
            server = new TransformedServerBuilder(server).averageChannelProject().build()
            classifierChannel = ColorTransforms.createMeanChannelTransform()
        } else {
            server = new TransformedServerBuilder(server).extractChannels(channel).build()
            classifierChannel = ColorTransforms.createChannelExtractor(channel)
        }
    } else {
        logger.error("Current image type not compatible with auto threshold.")
        return
    }

    // Check if threshold is Double (for fixed threshold) or String (for auto threshold)
    String thresholdMethod
    if (threshold instanceof String) {
        thresholdMethod = threshold
    } else {
        thresholdMethod = "Fixed"
    }

    // Apply the selected algorithm
    def validThresholds = ["Fixed", "Default", "Huang", "Intermodes", "IsoData", "IJ_IsoData", "Li", "MaxEntropy", "Mean", "MinError", "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag", "Triangle", "Yen"]

    double thresholdValue
    if (thresholdMethod in validThresholds){
        if (thresholdMethod == "Fixed") {
            thresholdValue = threshold
        } else {
            // Determine threshold value by auto threshold method
            ROI pathROI = annotation.getROI() // Get QuPath ROI
            PathImage pathImage = IJTools.convertToImagePlus(server, RegionRequest.createInstance(server.getPath(), thresholdDownsample, pathROI)) // Get PathImage within bounding box of annotation
            def ijRoi = IJTools.convertToIJRoi(pathROI, pathImage) // Convert QuPath ROI into ImageJ ROI
            ImagePlus imagePlus = pathImage.getImage() // Convert PathImage into ImagePlus
            ImageProcessor ip = imagePlus.getProcessor() // Get ImageProcessor from ImagePlus
            ip.setRoi(ijRoi) // Add ImageJ ROI to the ImageProcessor to limit the histogram to within the ROI only

            if (darkBackground) {
                ip.setAutoThreshold("${thresholdMethod} dark")
            } else {
                ip.setAutoThreshold("${thresholdMethod}")
            }

            thresholdValue = ip.getMaxThreshold()
            if (thresholdValue != null && thresholdValue < thresholdFloor) {
                thresholdValue = thresholdFloor
            }
        }
    } else {
        logger.error("Invalid auto-threshold method")
        return
    }

    // If specified output is "threshold value, return threshold value in annotation measurements
    if (output == "threshold value") {
        logger.info("${thresholdMethod} threshold value: ${thresholdValue}")
        annotation.measurements.put("${thresholdMethod} threshold value".toString(), thresholdValue)
        return
    }

    // Assign classification
    def classificationBelow
    if (classBelow instanceof PathClass) {
        classificationBelow = classBelow
    } else if (classBelow instanceof String) {
        classificationBelow = getPathClass(classBelow)
    } else if (classBelow == null) {
        classificationBelow = classBelow
    }
    
    def classificationAbove
    if (classAbove instanceof PathClass) {
        classificationAbove = classAbove
    } else if (classAbove instanceof String) {
        classificationAbove = getPathClass(classAbove)
    } else if (classAbove == null) {
        classificationAbove = classAbove
    }

    Map<Integer, PathClass> classifications = new LinkedHashMap<>()
    classifications.put(0, classificationBelow)
    classifications.put(1, classificationAbove)

    // Define parameters for pixel classifier
    List<ImageOp> ops = new ArrayList<>()
    ops.add(ImageOps.Filters.gaussianBlur(classifierGaussianSigma))
    ops.add(ImageOps.Threshold.threshold(thresholdValue))

    // Create pixel classifier
    def op = ImageOps.Core.sequential(ops)
    def transformer = ImageOps.buildImageDataOp(classifierChannel).appendOps(op)
    def classifier = PixelClassifiers.createClassifier(
        transformer,
        resolution,
        classifications
    )

    // Apply classifier
    selectObjects(annotation)
    if (output == "annotation") {
        logger.info("Creating annotations in ${annotation} from ${thresholdMethod}: ${thresholdValue}")
        
        if (classifierObjectOptions) {
            classifierObjectOptions = classifierObjectOptions.split(',')
            def allowedOptions = ["SPLIT", "DELETE_EXISTING", "INCLUDE_IGNORED", "SELECT_NEW"]
            boolean checkValid = classifierObjectOptions.every{allowedOptions.contains(it)}

            if (checkValid) {
                createAnnotationsFromPixelClassifier(classifier, minArea, minHoleArea, classifierObjectOptions)
            } else {
                logger.warn("Invalid create annotation options")
                return
            }
        } else {
            createAnnotationsFromPixelClassifier(classifier, minArea, minHoleArea)
        }
    }
    if (output == "detection") {
        logger.info("Creating detections in ${annotation} from ${thresholdMethod}: ${thresholdValue}")

        if (classifierObjectOptions) {
            classifierObjectOptions = classifierObjectOptions.split(',')
            def allowedOptions = ["SPLIT", "DELETE_EXISTING", "INCLUDE_IGNORED", "SELECT_NEW"]
            boolean checkValid = classifierObjectOptions.every{allowedOptions.contains(it)}

            if (checkValid) {
                createDetectionsFromPixelClassifier(classifier, minArea, minHoleArea, classifierObjectOptions)
            } else {
                logger.warn("Invalid create detection options")
                return
            }
        } else {
            createDetectionsFromPixelClassifier(classifier, minArea, minHoleArea)
        }
    }
    if (output == "measurement") {
        logger.info("Measuring thresholded area in ${annotation} from ${thresholdMethod}: ${thresholdValue}")
        def measurementID = "${thresholdMethod} threshold"
        addPixelClassifierMeasurements(classifier, measurementID)
    }
    if (output == "preview") {
        logger.info("Showing preview of ${annotation} with ${thresholdMethod}: ${thresholdValue}")
        OverlayOptions overlayOption = qupath.getOverlayOptions()
        overlayOption.setPixelClassificationRegionFilter(RegionFilter.StandardRegionFilters.ANY_ANNOTATIONS) // RegionFilter.StandardRegionFilters.ANY_ANNOTATIONS
        PixelClassificationOverlay previewOverlay = PixelClassificationOverlay.create(overlayOption, classifier)
        previewOverlay.setLivePrediction(true)
        qupath.getViewer().setCustomPixelLayerOverlay(previewOverlay)
    }
    
    if (classificationBelow == null) {
        annotation.measurements.put(thresholdMethod + " threshold value", thresholdValue)
    }
    if (classificationAbove == null) {
        annotation.measurements.put("${thresholdMethod}: ${classificationBelow.toString()} threshold value", thresholdValue)
    }
    if (classificationBelow != null && classificationAbove != null) {
        annotation.measurements.put("${thresholdMethod} threshold value", thresholdValue)
    }
}



