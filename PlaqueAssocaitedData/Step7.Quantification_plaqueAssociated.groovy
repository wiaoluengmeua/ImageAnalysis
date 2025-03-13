// -----------------------------------------------------------------------------
// 1. Necessary imports
// -----------------------------------------------------------------------------
import qupath.lib.gui.scripting.QPEx
import qupath.lib.projects.Project
import qupath.lib.projects.ProjectImageEntry
import qupath.lib.regions.RegionRequest
import qupath.imagej.tools.IJTools
import ij.IJ
import ij.plugin.ChannelSplitter
import ij.plugin.ImageCalculator
import ij.process.ImageStatistics
import groovy.transform.Field

// -----------------------------------------------------------------------------
// 2. Define thresholds for each channel
//    Now, Opal 620 is C3 and Opal 520 is Plaque.
// -----------------------------------------------------------------------------
@Field def thresholds = [
    'C3' : 200,  // Opal 620
    'Plaque' : 110   // Opal 520
]

// -----------------------------------------------------------------------------
// 3. Define your output directory (adjust the path as needed)
// -----------------------------------------------------------------------------
@Field def outputDir = "Z:/Zhao-Group/Current-members/Zonghua/DataTransfer"

// -----------------------------------------------------------------------------
// 4. Prepare a list to store *all* results from the entire project
// -----------------------------------------------------------------------------
def allResults = []

// -----------------------------------------------------------------------------
// 5. Get the QuPath project & all images in the project
// -----------------------------------------------------------------------------
def project = getProject()
def entryList = project.getImageList()
if (!entryList) {
    println "No images found in the project!"
    return
}

// -----------------------------------------------------------------------------
// 6. Loop over every image in the project
// -----------------------------------------------------------------------------
int imageIndex = 1
for (entry in entryList) {
    try {
        // Open the image
        def imageData = entry.readImageData()
        def server = imageData.getServer()
        def hierarchy = imageData.getHierarchy()
        def imageName = entry.getImageName()
        setBatchProjectAndImage(project, imageData)
        
        println "Processing image ${imageIndex}/${entryList.size()}: ${imageName}"
        
        // Get all annotations
        def annotations = hierarchy.getAnnotationObjects()
        if (annotations.isEmpty()) {
            println "  No annotations found in image: ${imageName}"
            imageIndex++
            continue
        }
        
        // Only process annotations that contain both 'Child_Circle' AND 'Opal 620'
        def relevantAnnotations = annotations.findAll { ann ->
            def n = ann.getName() ?: ""
            return n =~ /Opal 520 \+\d+µm-\d+-/
        }
        
        // Updated comment to clarify we're specifically checking for "Child_Circle" AND "Opal 620"
        if (relevantAnnotations.isEmpty()) {
            println "  No relevant (Child_Circle + Opal 620) annotations in: ${imageName}"
            server.close()
            imageData = null
            hierarchy = null
            System.gc()
            imageIndex++
            continue
        }
        
        // We'll store the results for this image locally, then add to allResults
        def resultsForThisImage = []
        
        int annotationIndex = 1
        for (annotation in relevantAnnotations) {
            println "    Processing annotation ${annotationIndex}/${relevantAnnotations.size()} in: ${imageName}"
            
            // Skip invalid ROI
            if (annotation.getROI() == null || annotation.getROI().getArea() <= 0) {
                println "      Skipping annotation ${annotation.getName()} (invalid ROI)."
                annotationIndex++
                continue
            }
            
            // Process the annotation (coverage, intensity, etc.)
            def result = processAnnotation(annotation, server, imageName)
            if (result != null) {
                
                // If this annotation is at level 3, also measure its parent's area in pixel count
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
                        
                        // 'Plaque Circle Coverage' stores the parent annotation's area in pixels
                        result['Plaque Circle Coverage'] = parentAreaPixels
                        annotation.getMeasurements().put('Plaque Circle Coverage', parentAreaPixels)
                    }
                }
                
                // Add the main results
                resultsForThisImage << result
                
                // Updated comment: C3 = channels[1] (Opal 620), Plaque = channels[2] (Opal 520)
                annotation.getMeasurements().put("C3 Coverage",        result['C3 Coverage'])
                annotation.getMeasurements().put("C3 Mean Intensity",  result['C3 Mean Intensity'])
                annotation.getMeasurements().put("C3 Total Intensity", result['C3 Total Intensity'])
                annotation.getMeasurements().put("Annotation Area",    result['Annotation Area'])
                annotation.getMeasurements().put("Circle Offset",      result['Circle Offset'])
                annotation.getMeasurements().put("Thresholded Plaque Circle Coverage", result['Thresholded Plaque Circle Coverage'])
            } else {
                println "      Failed to process annotation: ${annotation.getName()}"
            }
            annotationIndex++
        }
        
        // Save results for this image into allResults
        allResults.addAll(resultsForThisImage)
        
        // Optionally save updated imageData back to the project
        entry.saveImageData(imageData)
        
        // Clean up
        hierarchy = null
        server.close()
        imageData = null
        System.gc()
        
        imageIndex++
    } catch (Exception e) {
        println "Error processing image ${entry.getImageName()}: ${e.message}"
        imageIndex++
    }
}

// -----------------------------------------------------------------------------
// 7. Write all results into a single CSV for the entire project
// -----------------------------------------------------------------------------
def csvFile = new File("${outputDir}/PlaqueAccociated_Quantification.csv")
csvFile.withPrintWriter { pw ->
    // Include columns for parent coverage and the new thresholded measurement from Plaque (Opal 520)
    pw.println("Sample Name,Image ID,Circle Offset,C3 Coverage,C3 Mean Intensity,C3 Total Intensity,Annotation,Annotation Area,Plaque Circle Coverage,Thresholded Plaque Circle Coverage")
    
    allResults.each { res ->
        pw.println(
            "${res['Sample Name']}," +
            "${res['Image ID']}," +
            "${res['Circle Offset']}," +
            "${res['C3 Coverage']}," +
            "${res['C3 Mean Intensity']}," +
            "${res['C3 Total Intensity']}," +
            "${res['Annotation']}," +
            "${res['Annotation Area']}," +
            "${res.get('Plaque Circle Coverage', '')}," +
            "${res.get('Thresholded Plaque Circle Coverage', '')}"
        )
    }
}

println "Done! Results written to: ${csvFile.getAbsolutePath()}"

// -----------------------------------------------------------------------------
// 8. processAnnotation function for measuring both C3 (Opal 620) & Plaque (Opal 520)
// -----------------------------------------------------------------------------
def processAnnotation(annotation, server, imageName) {
    // Use the new thresholds:
    def thresholdC3 = thresholds['C3']   // For Opal 620 (now C3)
    def thresholdPlaque = thresholds['Plaque']   // For Opal 520 (now Plaque)
    
    // Parse the circle offset from annotation name (e.g. "Child_Circle+2µm" → 2)
    def offset = ""
    try {
        def matcher = (annotation.getName() =~ /Child_Circle\+(\d+)µm/)
        if (matcher.find()) {
            offset = matcher.group(1)
        }
    } catch(e) {
        offset = ""
    }
    def numericOffset = (offset == "" ? 0 : offset.toDouble())

    try {
        // Create a region request at full resolution
        def request = RegionRequest.createInstance(server.getPath(), 1, annotation.getROI())
        def pathImagePlus = IJTools.convertToImagePlus(server, request)
        if (pathImagePlus == null) {
            println "      Could not convert region for annotation: ${annotation.getName()}"
            return null
        }
        def imgPlus = pathImagePlus.getImage()
        if (imgPlus == null) {
            println "      Converted ImagePlus is null for annotation: ${annotation.getName()}"
            return null
        }

        // Split the channels
        def channels = ChannelSplitter.split(imgPlus)
        
        // ---------------------------------------------------------------------
        // Assign channels based on the new definitions:
        //   channels[1] -> C3 (Opal 620, threshold=200)
        //   channels[2] -> Plaque (Opal 520, threshold=110)
        // ---------------------------------------------------------------------
        def imgC3Original = channels[1].duplicate()
        def imgC3 = channels[1]
        def imgPlaqueOriginal = channels[2].duplicate()
        def imgPlaque = channels[2]

        // Close other channels if they exist
        for (int i = 0; i < channels.size(); i++) {
            if (i != 1 && i != 2) {
                channels[i].close()
                channels[i].flush()
            }
        }
        // Close the composite image
        imgPlus.close()
        imgPlus.flush()
        imgPlus = null

        def ic = new ImageCalculator()
        
        // ---------------------------------------------------------------------
        // 1) Threshold & measure C3 (Opal 620)
        // ---------------------------------------------------------------------
        IJ.setThreshold(imgC3, thresholdC3, 65535)
        IJ.run(imgC3, "Convert to Mask", "")
        def imgC3_masked = ic.run("Multiply create", imgC3Original, imgC3)
        def statsC3 = imgC3_masked.getStatistics()
        double C3Coverage       = (statsC3.pixelCount - statsC3.histogram[0]) as double
        double C3TotalIntensity = statsC3.pixelCount * statsC3.mean
        double C3MeanIntensity  = (C3Coverage > 0) ? (C3TotalIntensity / C3Coverage) : 0.0
        
        // Clean up C3 images
        imgC3Original.close(); imgC3Original.flush()
        imgC3.close();         imgC3.flush()
        imgC3_masked.close();  imgC3_masked.flush()

        // ---------------------------------------------------------------------
        // 2) Threshold & measure Plaque (Opal 520) → "Thresholded Plaque Circle Coverage"
        // ---------------------------------------------------------------------
        IJ.setThreshold(imgPlaque, thresholdPlaque, 65535)
        IJ.run(imgPlaque, "Convert to Mask", "")
        def imgPlaque_masked = ic.run("Multiply create", imgPlaqueOriginal, imgPlaque)
        def statsPlaque = imgPlaque_masked.getStatistics()
        double PlaqueCoverage = (statsPlaque.pixelCount - statsPlaque.histogram[0]) as double

        // Clean up Plaque images
        imgPlaqueOriginal.close(); imgPlaqueOriginal.flush()
        imgPlaque.close();         imgPlaque.flush()
        imgPlaque_masked.close();  imgPlaque_masked.flush()

        // Physical area of the annotation ROI
        def annotationArea = annotation.getROI()?.getArea() ?: 0.0
        def sampleName     = annotation.getName()

        // Return result map with both channel measurements and the new thresholded coverage
        return [
            'Sample Name'                      : sampleName,
            'Image ID'                         : imageName,
            'Circle Offset'                    : numericOffset,
            'C3 Coverage'                      : C3Coverage,
            'C3 Mean Intensity'                : C3MeanIntensity,
            'C3 Total Intensity'               : C3TotalIntensity,
            'Annotation'                       : sampleName,
            'Annotation Area'                  : annotationArea,
            'Thresholded Plaque Circle Coverage' : PlaqueCoverage
        ]
    } catch (Exception e) {
        println "      Error processing annotation ${annotation.getName()}: ${e.message}"
        return null
    } finally {
        System.gc()
    }
}
