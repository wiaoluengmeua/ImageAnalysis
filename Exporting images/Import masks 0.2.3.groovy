// Script written for QuPath v0.2.3
// Minimal working script to import labelled images 
// (from the TileExporter) back into QuPath as annotations.

import qupath.lib.objects.PathObjects
import qupath.lib.regions.ImagePlane
import static qupath.lib.gui.scripting.QPEx.*
import ij.IJ
import ij.process.ColorProcessor
import qupath.imagej.processing.RoiLabeling
import qupath.imagej.tools.IJTools
import java.util.regex.Matcher
import java.util.regex.Pattern


def directoryPath = 'path/to/your/directory' // TO CHANGE
File folder = new File(directoryPath);
File[] listOfFiles = folder.listFiles();

listOfFiles.each { file ->
    def path = file.getPath()
    def imp = IJ.openImage(path)
    
    // Only process the labelled images, not the originals
    if (!path.endsWith("-labelled.tif"))
        return
        
    print "Now processing: " + path
    
    // Parse filename to understand where the tile was located
    def parsedXY = parseFilename(GeneralTools.getNameWithoutExtension(path))
   
    double downsample = 1 // TO CHANGE (if needed)
    ImagePlane plane = ImagePlane.getDefaultPlane()
    
    
    // Convert labels to ImageJ ROIs
    def ip = imp.getProcessor()
    if (ip instanceof ColorProcessor) {
        throw new IllegalArgumentException("RGB images are not supported!")
    }
    
    int n = imp.getStatistics().max as int
    if (n == 0) {
        print 'No objects found!'
        return
    }
    def roisIJ = RoiLabeling.labelsToConnectedROIs(ip, n)
    
    
    
    // Convert ImageJ ROIs to QuPath ROIs
    def rois = roisIJ.collect {
        if (it == null)
            return
        return IJTools.convertToROI(it, -parsedXY[0]/downsample, -parsedXY[1]/downsample, downsample, plane);
    }
    
    // Remove all null values from list
    rois = rois.findAll{null != it}
    
    // Convert QuPath ROIs to objects
    def pathObjects = rois.collect {
        return PathObjects.createAnnotationObject(it)
    }
    addObjects(pathObjects)
}

resolveHierarchy()



int[] parseFilename(String filename) {
    def p = Pattern.compile("\\[x=(.+?),y=(.+?),")
    parsedXY = []
    Matcher m = p.matcher(filename)
    if (!m.find())
        throw new IOException("Filename does not contain tile position")
            
    parsedXY << (m.group(1) as double)
    parsedXY << (m.group(2) as double)
    
    return parsedXY
}