// -----------------------------------------------------------------------------
// 1. Necessary imports
// -----------------------------------------------------------------------------
import qupath.lib.gui.scripting.QPEx
import qupath.lib.regions.RegionRequest
import qupath.imagej.tools.IJTools
import ij.IJ
import ij.plugin.ChannelSplitter
import ij.plugin.ImageCalculator
import ij.process.ImageStatistics
import groovy.transform.Field
import qupath.lib.roi.RoiTools

// -----------------------------------------------------------------------------
// 2. Define threshold for Opal 620 (C3)
//    Adjust as needed.
// -----------------------------------------------------------------------------
@Field def thresholds = [
    'C3' : 200
]

// -----------------------------------------------------------------------------
// 3. Define your output directory (adjust the path as needed)
// -----------------------------------------------------------------------------
@Field def outputDir = "Z:/Zhao-Group/Current-members/Zonghua/DataTransfer"

// -----------------------------------------------------------------------------
// 4. Get the currently opened image
// -----------------------------------------------------------------------------
def imageData = getCurrentImageData()
if (imageData == null) {
    println "No image is open."
    return
}
def server = imageData.getServer()
def hierarchy = imageData.getHierarchy()
def imageName = getProjectEntry().getImageName()  // Name of the current image

println "Processing image: ${imageName}"

// -----------------------------------------------------------------------------
// 5. Get all annotations & filter for "Opal 620"
// -----------------------------------------------------------------------------
def annotations = hierarchy.getAnnotationObjects()
if (annotations.isEmpty()) {
    println "No annotations found in image: ${imageName}"
    return
}

// Only process those whose name contains "Child_Circle" and "Opal 620"
def relevantAnnotations = annotations.findAll { ann ->
    def n = ann.getName() ?: ""
    return n.contains("Child_Circle") && n.contains("Opal 620")
}

if (relevantAnnotations.isEmpty()) {
    println "No relevant (Opal 620) annotations found in image: ${imageName}"
    return
}

// -----------------------------------------------------------------------------
// 6. Process each relevant annotation and record results
// -----------------------------------------------------------------------------
def results = []
int annotationIndex = 1

for (annotation in relevantAnnotations) {
    println "  Processing annotation ${annotationIndex} of ${relevantAnnotations.size()} in: ${imageName}"
    
    // Skip invalid ROI
    if (annotation.getROI() == null || annotation.getROI().getArea() <= 0) {
        println "  Skipping annotation ${annotation.getName()} due to invalid ROI."
        annotationIndex++
        continue
    }
    
    // Process annotation to measure coverage & intensity
    def result = processAnnotation(annotation, server, imageName)
    if (result != null) {
        
        // ----------------------------------------------------------------------------
        // If this is a Level 3 annotation, get its parent using .getParent()
        // and compute both the area (µm²) and pixel count.
        // ----------------------------------------------------------------------------
        if (annotation.getLevel() == 3) {
            def parentAnnotation = annotation.getParent()
            if (parentAnnotation && parentAnnotation.getROI()) {
                def parentAreaSqMicrons = parentAnnotation.getROI().getArea()
                
                // Convert parent's area from µm² to pixel count
                def cal = server.getPixelCalibration()
                double parentAreaPixels = parentAreaSqMicrons
                if (cal != null) {
                    double pixelAreaMicrons = cal.getPixelWidthMicrons() * cal.getPixelHeightMicrons()
                    if (pixelAreaMicrons > 0) {
                        parentAreaPixels = parentAreaSqMicrons / pixelAreaMicrons
                    }
                }
                
                // Save both measurements in the result & annotation's measurements
                result['Plaque Circle Coverage (area)'] = parentAreaSqMicrons
                result['Plaque Circle Coverage (pixels)'] = parentAreaPixels
                
                annotation.getMeasurements().put('Plaque Circle Coverage (area)', parentAreaSqMicrons)
                annotation.getMeasurements().put('Plaque Circle Coverage (pixels)', parentAreaPixels)
            }
        }
        
        // Add the main results to our list
        results << result
        
        // Add numeric measurements to the current annotation
        annotation.getMeasurements().put("C3 Coverage",         result['C3 Coverage'])
        annotation.getMeasurements().put("C3 Mean Intensity",   result['C3 Mean Intensity'])
        annotation.getMeasurements().put("C3 Total Intensity",  result['C3 Total Intensity'])
        annotation.getMeasurements().put("Annotation Area",     result['Annotation Area'])
        annotation.getMeasurements().put("Circle Offset",       result['Circle Offset'])
    } else {
        println "  Failed to process annotation: ${annotation.getName()}"
    }
    annotationIndex++
}

// -----------------------------------------------------------------------------
// 7. Refresh the hierarchy so measurements appear
// -----------------------------------------------------------------------------
hierarchy.fireHierarchyChangedEvent((Object) null)

// -----------------------------------------------------------------------------
// 8. Write results to CSV
// -----------------------------------------------------------------------------
def csvFile = new File("${outputDir}/C3_Quantification_singleImage.csv")
csvFile.withPrintWriter { pw ->
    // CSV header, now including both parent area measurements
    pw.println("Sample Name,Image ID,Circle Offset,C3 Coverage,C3 Mean Intensity,C3 Total Intensity,Annotation,Annotation Area,Plaque Circle Coverage (area),Plaque Circle Coverage (pixels)")

    // Each result is a CSV row
    results.each { res ->
        pw.println(
            "${res['Sample Name']}," +
            "${res['Image ID']}," +
            "${res['Circle Offset']}," +
            "${res['C3 Coverage']}," +
            "${res['C3 Mean Intensity']}," +
            "${res['C3 Total Intensity']}," +
            "${res['Annotation']}," +
            "${res['Annotation Area']}," +
            "${res.get('Plaque Circle Coverage (area)', '')}," +
            "${res.get('Plaque Circle Coverage (pixels)', '')}"
        )
    }
}

println "Results written to: ${csvFile.getAbsolutePath()}"

// -----------------------------------------------------------------------------
// 9. processAnnotation function for single-channel (C3) measurement
// -----------------------------------------------------------------------------
def processAnnotation(annotation, server, imageName) {
    // Threshold for C3
    def thresholdC3 = thresholds['C3']

    // Parse the circle offset from the annotation name 
    // e.g. "Child_Circle+2µm" → offset "2"
    def offset = ""
    try {
        def matcher = (annotation.getName() =~ /Child_Circle\+(\d+)µm/)
        if (matcher.find()) {
            offset = matcher.group(1)
        }
    } catch(e) {
        offset = ""
    }
    // Convert offset to a numeric value
    def numericOffset = (offset == "" ? 0 : offset.toDouble())

    try {
        // Create region request @ full resolution
        def request = RegionRequest.createInstance(server.getPath(), 1, annotation.getROI())
        def pathImagePlus = IJTools.convertToImagePlus(server, request)
        if (pathImagePlus == null) {
            println "Could not convert region for annotation: ${annotation.getName()}"
            return null
        }
        def imgPlus = pathImagePlus.getImage()
        if (imgPlus == null) {
            println "Converted ImagePlus is null for annotation: ${annotation.getName()}"
            return null
        }

        // Split the channels
        def channels = ChannelSplitter.split(imgPlus)
        // Adjust index if needed (assuming C3 is index 1)
        def imgC3Original = channels[1].duplicate()
        def imgC3         = channels[1]

        // Close extra channels
        for (int i = 0; i < channels.size(); i++) {
            if (i != 1) {
                channels[i].close()
                channels[i].flush()
            }
        }
        // Close the composite
        imgPlus.close()
        imgPlus.flush()
        imgPlus = null

        // Threshold & masking for C3
        def ic = new ImageCalculator()
        IJ.setThreshold(imgC3, thresholdC3, 65535)
        IJ.run(imgC3, "Convert to Mask", "")
        def imgC3_masked = ic.run("Multiply create", imgC3Original, imgC3)

        // Stats from masked image
        def statsC3 = imgC3_masked.getStatistics()
        double c3Coverage       = (statsC3.pixelCount - statsC3.histogram[0]) as double
        double c3TotalIntensity = statsC3.pixelCount * statsC3.mean
        double c3MeanIntensity  = (c3Coverage > 0) ? (c3TotalIntensity / c3Coverage) : 0.0

        // Close & flush
        imgC3Original.close()
        imgC3Original.flush()
        imgC3.close()
        imgC3.flush()
        imgC3_masked.close()
        imgC3_masked.flush()

        // Physical area (µm² if the image is calibrated)
        def annotationArea = annotation.getROI()?.getArea() ?: 0.0
        def sampleName     = annotation.getName()

        // Return result map
        return [
            'Sample Name'         : sampleName,
            'Image ID'            : imageName,
            'Circle Offset'       : numericOffset,
            'C3 Coverage'         : c3Coverage,
            'C3 Mean Intensity'   : c3MeanIntensity,
            'C3 Total Intensity'  : c3TotalIntensity,
            'Annotation'          : sampleName,
            'Annotation Area'     : annotationArea
        ]
    } catch (Exception e) {
        println "Error processing annotation ${annotation.getName()}: ${e.message}"
        return null
    } finally {
        System.gc()
    }
}
