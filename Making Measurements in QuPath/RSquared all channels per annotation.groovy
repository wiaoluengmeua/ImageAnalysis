//Tested and working in 0.2.3
//Intended to be used on small area annotations to check for particularly high R-Sq
//Measurements below the cutoff (between 0-1) will be ignored - not added to the measurement list
cutoff = 0.5

//Adds R^2 value between two chosen channels within all annotations.
//Look in the measurement list of any given annotation
//ANNOTATIONS MAY FAIL IF THE ANNOTATIONS ARE TOO LARGE.

def imageData = getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def serverOriginal = imageData.getServer()
nchannels = getCurrentImageData().getServer().nChannels()
String path = serverOriginal.getPath()
double downsample = 1.0
ImageServer<BufferedImage> server = serverOriginal

logger.info("Channel count: "+nchannels)
getAnnotationObjects().each{
    //Get the bounding box region around the target detection
    roi = it.getROI()
    request = RegionRequest.createInstance(path, downsample, roi)
    pathImage = IJTools.convertToImagePlus(server, request)
    imp = IJTools.convertToImagePlus(server, request).getImage()
   
    //println(imp.getClass())
    //Extract the first channel as a list of pixel values
    for (c = 1; c<=nchannels; c++){
    
        firstChanImage = imp.getStack().getProcessor(c)
    
        firstChanImage = firstChanImage.convertToFloatProcessor()  //Needed to handle big numbers
    
        ch1Pixels = firstChanImage.getPixels()
        
        //Create a mask so that only the pixels we want from the bounding box area are used in calculations
        bpSLICs = createObjectMask(pathImage, it).getPixels()
        
        int size = ch1Pixels.size()
        //Cycle through all remaining channels to compare them to channel i
        for (k = c+1;k<=nchannels;k++){
            secondChanImage= imp.getStack().getProcessor(k)
            secondChanImage=secondChanImage.convertToFloatProcessor()
            ch2Pixels = secondChanImage.getPixels()
            ch1 = []
            ch2 = []
            
            for (i=0; i<size-1; i++){
                if(bpSLICs[i]){
                    ch1<<ch1Pixels[i]
                    ch2<<ch2Pixels[i]
                }
            }
            
            def points = new double [ch1.size()][2]
            for(i=0;i < ch1.size()-1; i++){ 
               points[i][0] = ch1[i]
               points[i][1] = ch2[i]
            }
            def regression = new org.apache.commons.math3.stat.regression.SimpleRegression()
            regression.addData(points)
            
            double r2 = regression.getRSquare()
            double slope = regression.getSlope()
            String name = c+"+"+k+" R^2"
            String slopeName = c+"+"+k+" slope"
            if (r2 > cutoff){
                it.getMeasurementList().putMeasurement(name, r2)
                it.getMeasurementList().putMeasurement(slopeName, slope)
            }
        }    
    }
}


def createObjectMask(PathImage pathImage, PathObject object) {
    //create a byteprocessor that is the same size as the region we are analyzing
    def bp = new ByteProcessor(pathImage.getImage().getWidth(), pathImage.getImage().getHeight())
    //create a value to fill into the "good" area
    bp.setValue(1.0)

    def roi = object.getROI()
    roiIJ = IJTools.convertToIJRoi(roi, pathImage)
    bp.fill(roiIJ)
    
    //fill the ROI with the setValue to create the mask, the other values should be 0
    
    return bp
}

import javafx.beans.property.SimpleLongProperty

import qupath.lib.gui.QuPathGUI
import qupath.lib.regions.RegionRequest
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.image.BufferedImage
//import qupath.imagej.objects.ROIConverterIJ
import ij.process.ImageProcessor
import qupath.lib.images.servers.ImageServer
import qupath.lib.objects.PathObject
import qupath.imagej.tools.IJTools
import qupath.lib.images.PathImage
//import qupath.imagej.objects.PathImagePlus
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;