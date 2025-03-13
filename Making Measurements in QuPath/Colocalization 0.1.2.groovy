//Version 3.1 Should work for 0.1.2 and 0.1.3

//****************VALUES TO EDIT***********//

//Channel numbers are based on cell measurement/channel order in Brightness/contrast menu, starting with 1
int FIRST_CHANNEL = 9
int SECOND_CHANNEL = 9

//CHOOSE ONE: "cell", "nucleus", "cytoplasm", "tile", "detection", "subcell"
//"detection" should be the equivalent of everything
String objectType = "cell"

//These should be figured out for a given sample to eliminate background signal
//Pixels below this value will not be considered for a given channel.
//Used for Manders coefficients only.
ch1Background = 100000
ch2Background = 100000

//***************NO TOUCHEE past here************//

import qupath.lib.regions.RegionRequest
import qupath.imagej.images.servers.ImagePlusServer
import qupath.imagej.images.servers.ImagePlusServerBuilder
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.image.BufferedImage
import ij.ImagePlus
import qupath.imagej.objects.ROIConverterIJ
import ij.process.ImageProcessor
import qupath.lib.roi.RectangleROI
import qupath.lib.images.servers.ImageServer
import qupath.lib.objects.PathObject
import qupath.imagej.helpers.IJTools
import qupath.lib.gui.ImageWriterTools

def imageData = getCurrentImageData()
def hierarchy = imageData.getHierarchy()

ImageServer<BufferedImage>  serverOriginal = imageData.getServer()
String path = serverOriginal.getPath()
double downsample = 1.0
def server = ImagePlusServerBuilder.ensureImagePlusWholeSlideServer(serverOriginal)



println("Running, please wait...")
//target the objects you want to analyze
if(objectType == "cell" || objectType == "nucleus" || objectType == "cytoplasm" ){detections = getCellObjects()}
if(objectType == "tile"){detections = getDetectionObjects().findAll{it.isTile()}}
if(objectType == "detection"){detections = getDetectionObjects()}
if(objectType == "subcell") {detections = getObjects({p-> p.class == qupath.imagej.detect.cells.SubcellularDetection.SubcellularObject.class})}



println("Count = "+ detections.size())

detections.each{
    //Get the bounding box region around the target detection
    roi = it.getROI()
    region = RegionRequest.createInstance(path, downsample, roi)
    imp = server.readImagePlusRegion(region).getImage()
    //Extract the first channel as a list of pixel values
    imp.setC(FIRST_CHANNEL)
    firstChanImage = imp.getProcessor()
    firstChanImage = firstChanImage.convertToFloatProcessor()  //Needed to handle big numbers
    ch1Pixels = firstChanImage.getPixels()
    //Create a mask so that only the pixels we want from the bounding box area are used in calculations
    bpSLICs = createObjectMask(firstChanImage, downsample, it, objectType).getPixels()
    
    //println(bpSLICs)
    //println(bpSLICs.getPixels())
    //println("ch1 size"+ch1.size())
    size = ch1Pixels.size()
    imp.setC(SECOND_CHANNEL)
    secondChanImage= imp.getProcessor()
    secondChanImage=secondChanImage.convertToFloatProcessor()
    ch2Pixels = secondChanImage.getPixels()
    //use mask to extract only the useful pixels into new lists
    //Maybe it would be faster to remove undesirable pixels instead?
    ch1 = []
    ch2 = []
    for (i=0; i<size; i++){
        if(bpSLICs[i]){
            ch1<<ch1Pixels[i]
            ch2<<ch2Pixels[i]
        }
    }
    
    //Calculating the mean for Pearson's
    ch1Premean = []
    ch2Premean = []    
    for (x in ch1) ch1Premean<<(x/ch1.size())
    for (x in ch2) ch2Premean<<(x/ch2.size())
    double ch1Mean = ch1Premean.sum()
    double ch2Mean = ch2Premean.sum()
    //get the new number of pixels to be analyzed
    size = ch1.size()
    
    //Create the sum for the top half of the pearson's correlation coefficient
    top = []
    for (i=0; i<size;i++){top << (ch1[i]-ch1Mean)*(ch2[i]-ch2Mean)}
    pearsonTop = top.sum()
    
    //Sums for the two bottom parts
    botCh1 = []
    for (i=0; i<size;i++){botCh1<< (ch1[i]-ch1Mean)*(ch1[i]-ch1Mean)}
    rootCh1 = Math.sqrt(botCh1.sum())
    
    botCh2 = []
    for (i=0; i<size;i++){botCh2 << (ch2[i]-ch2Mean)*(ch2[i]-ch2Mean)}
    rootCh2 = Math.sqrt(botCh2.sum())



    pearsonBot = rootCh2*rootCh1

    double pearson = pearsonTop/pearsonBot
    String name = "Pearson Corr "+FIRST_CHANNEL+"+"+SECOND_CHANNEL
    it.getMeasurementList().putMeasurement(name, pearson)

    //Start Manders calculations
    double m1Top = 0
    for (i=0; i<size;i++){if (ch2[i] > ch2Background){m1Top += Math.max(ch1[i]-ch1Background,0)}}
    double m1Bottom = 0
    for (i=0; i<size;i++){m1Bottom += Math.max(ch1[i]-ch1Background,0)}
    double m2Top = 0
    for (i=0; i<size;i++){if (ch1[i] > ch1Background){m2Top += Math.max(ch2[i]-ch2Background,0)}}
    double m2Bottom = 0
    for (i=0; i<size;i++){m2Bottom += Math.max(ch2[i]-ch2Background,0)}
    
    //Check for divide by zero and add measurements
    name = "M1 "+objectType+": ratio of Ch"+FIRST_CHANNEL+" intensity in Ch"+SECOND_CHANNEL+" areas"
    double M1 = m1Top/m1Bottom
    if (M1.isNaN()){M1 = 0}
    it.getMeasurementList().putMeasurement(name, M1)
    double M2 = m2Top/m2Bottom
    if (M2.isNaN()){M2 = 0}
    name = "M2 "+objectType+": ratio of Ch"+SECOND_CHANNEL+" intensity in Ch"+FIRST_CHANNEL+" areas"
    it.getMeasurementList().putMeasurement(name, M2)
}    

    
println("Done!")    

//Making a mask. Phantom of the Opera style.

def createObjectMask(ImageProcessor ip, double downsample, PathObject object, String objectType) {
    //create a byteprocessor that is the same size as the region we are analyzing
    def bp = new ByteProcessor(ip.getWidth(), ip.getHeight())
    //create a value to fill into the "good" area
    bp.setValue(1.0)

    //extract the ROI and shift the position so that it is within the stand-alone image region
    //Otherwise the coordinates are based off of the original image, and not just the small subsection we are analyzing
    if (objectType == "nucleus"){
        def roi = object.getNucleusROI()
        shift = roi.translate(ip.getWidth()/2-roi.getCentroidX(), ip.getHeight()/2-roi.getCentroidY())
        def roiIJ = ROIConverterIJ.convertToIJRoi(shift, 0, 0, downsample)
        bp.fill(roiIJ)
        
    }else if (objectType == "cytoplasm"){
        def nucleus = object.getNucleusROI()
        shiftNuc = nucleus.translate(ip.getWidth()/2-roi.getCentroidX(), ip.getHeight()/2-roi.getCentroidY())
        roiIJNuc = ROIConverterIJ.convertToIJRoi(shiftNuc, 0, 0, downsample)
        
        def roi = object.getROI()
        shift = roi.translate(ip.getWidth()/2-roi.getCentroidX(), ip.getHeight()/2-roi.getCentroidY())
        def roiIJ = ROIConverterIJ.convertToIJRoi(shift, 0, 0, downsample)
        bp.fill(roiIJ)
        bp.setValue(0)
        bp.fill(roiIJNuc)
        
    } else { 
        def roi = object.getROI()
        shift = roi.translate(ip.getWidth()/2-roi.getCentroidX(), ip.getHeight()/2-roi.getCentroidY())
        roiIJ = ROIConverterIJ.convertToIJRoi(shift, 0, 0, downsample)
        bp.fill(roiIJ)
    }

    
    //fill the ROI with the setValue to create the mask, the other values should be 0
    
    return bp
}
