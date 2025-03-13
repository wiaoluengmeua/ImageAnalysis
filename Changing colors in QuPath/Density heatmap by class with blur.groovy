// https://forum.image.sc/t/cell-density-map/40050/16
/**
 * Create a 'counts' image in QuPath that can be used to compute the local density of specific objects.
 *
 * This implementation uses ImageJ to create and display the image, which can then be filtered as required.
 * 
 * Written for QuPath v0.2.0.
 *
 * @author Pete Bankhead
 */


// Define the resolution at which the image should be generated
double requestedPixelSizeMicrons = 50
double sigma = 1.5
double accuracy = 0.01

classList = getCurrentHierarchy().getDetectionObjects().collect{it.getPathClass()} as Set
// Get the current image
def imageData = getCurrentImageData()
def server = imageData.getServer()
// Set the downsample directly (without using the requestedPixelSize) if you want; 1.0 indicates the full resolution
double downsample = requestedPixelSizeMicrons / server.getPixelCalibration().getAveragedPixelSizeMicrons()
def request = RegionRequest.createInstance(server, downsample)
def imp = IJTools.convertToImagePlus(server, request).getImage()

// Get the objects you want to count
// Potentially you can add filters for specific objects, e.g. to get only those with a 'Positive' classification
def detections = getDetectionObjects()

classList.each{c->
    cellList = getCellObjects().findAll{it.getPathClass() == c}
    // Create a counts image in ImageJ, where each pixel corresponds to the number of centroids at that pixel
    int width = imp.getWidth()
    int height = imp.getHeight()
    def fp = new FloatProcessor(width, height)
        for (detection in cellList) {
        // Get ROI for a detection; this method gets the nucleus if we have a cell object (and the only ROI for anything else)
        def roi = PathObjectTools.getROI(detection, true)
        int x = (int)(roi.getCentroidX() / downsample)
        int y = (int)(roi.getCentroidY() / downsample)
        fp.setf(x, y, fp.getf(x, y) + 1 as float)
    }
    
    //Here we blur fp. Increase sigma for more blurring.
    def g = new GaussianBlur();
    g.blurGaussian(fp, sigma, sigma, accuracy);
    
    
    for (detection in detections) {
        // Get ROI for a detection; this method gets the nucleus if we have a cell object (and the only ROI for anything else)
        def roi = PathObjectTools.getROI(detection, true)
        int x = (int)(roi.getCentroidX() / downsample)
        int y = (int)(roi.getCentroidY() / downsample)
        detection.getMeasurementList().putMeasurement(c.toString()+' Density', fp.getf(x, y))
    }
    
}    


import ij.ImagePlus
import ij.process.FloatProcessor
import ij.plugin.filter.GaussianBlur;
//import ij.plugin.filter.PlugInFilter;

import qupath.imagej.gui.IJExtension
import qupath.lib.objects.PathObjectTools
import qupath.lib.regions.RegionRequest

import static qupath.lib.gui.scripting.QPEx.*
import qupath.imagej.tools.IJTools