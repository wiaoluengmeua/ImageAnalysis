//Tested and working in 0.2.3
//Adds R^2 value between two chosen channels within any particular object - to that object -.
//Look in the measurement tables or measurement maps.
//ANNOTATIONS MAY FAIL IF THE ANNOTATIONS ARE TOO LARGE.

Logger logger = LoggerFactory.getLogger(QuPathGUI.class);
int col = 0
int row = 0
int textFieldWidth = 120
int labelWidth = 150
def gridPane = new GridPane()
def imageData = getCurrentImageData()
def hierarchy = imageData.getHierarchy()
gridPane.setPadding(new Insets(10, 10, 10, 10));
gridPane.setVgap(2);
gridPane.setHgap(10);

TYPE = ["cell", "nucleus", "cytoplasm", "tile", "detection", "subcell", "annotation"]
def serverOriginal = imageData.getServer()
String path = serverOriginal.getPath()
double downsample = 1.0
ImageServer<BufferedImage> server = serverOriginal
def mChoice = new Label("First Channel      ")
mChoice.setMaxWidth(400)
mChoice.setAlignment(Pos.CENTER_RIGHT)
def mChoice2 = new Label("Second Channel    ")
mChoice2.setMaxWidth(400)
mChoice2.setAlignment(Pos.CENTER_RIGHT)
gridPane.add( mChoice, col++, row, 1,1)
gridPane.add( mChoice2, col++, row++, 1,1)

col = 0
measurement1 = new ComboBox()
getQuPath().getViewer().getImageDisplay().availableChannels().each {measurement1.getItems().add(it) }
measurement2 = new ComboBox()
getQuPath().getViewer().getImageDisplay().availableChannels().each {measurement2.getItems().add(it) }
objectTypes = new ComboBox()
TYPE.each{objectTypes.getItems().add(it)}

gridPane.add(measurement1, col++, row, 1,1)
gridPane.add(measurement2, col++, row, 1,1)
gridPane.add(objectTypes, col ,row++,1,1)
Button runButton = new Button()
runButton.setText("Calculate")
gridPane.add(runButton, 0, row++, 1, 1)
def warn = new Label("VERY slow, check CPU useage to see if it is still running")
warn.setMaxWidth(400)
warn.setAlignment(Pos.CENTER_RIGHT)
gridPane.add( warn, 0, row, 4,1)

runButton.setOnAction {
    
  int FIRST_CHANNEL = measurement1.getSelectionModel().getSelectedIndex()+1
  int SECOND_CHANNEL = measurement2.getSelectionModel().getSelectedIndex()+1
  objectType = objectTypes.getValue().toString()
  if (!objectType){objectType = "detection"}
  if(objectType == "cell" || objectType == "nucleus" || objectType == "cytoplasm" ){detections = getQuPath().getImageData().getHierarchy().getCellObjects()}
  if(objectType == "tile"){detections = getQuPath().getImageData().getHierarchy().getDetectionObjects().findAll{it.isTile()}}
  if(objectType == "annotation"){detections = getQuPath().getImageData().getHierarchy().getAnnotationObjects()}

  if(objectType == "detection"){detections = getQuPath().getImageData().getHierarchy().getDetectionObjects()}
  if(objectType == "subcell") {detections = getQuPath().getImageData().getHierarchy().getObjects({p-> p.class == qupath.lib.objects.PathDetectionObject.class})}
  logger.info("Start"+detections.size())
  detections.each{
    //Get the bounding box region around the target detection
    roi = it.getROI()
    request = RegionRequest.createInstance(path, downsample, roi)
    pathImage = IJTools.convertToImagePlus(server, request)
    imp = IJTools.convertToImagePlus(server, request).getImage()
    imp.show()

    
    //println(imp.getClass())
    //Extract the first channel as a list of pixel values
    firstChanImage = imp.getStack().getProcessor(FIRST_CHANNEL)
    logger.info("aftergetprocessor")
    firstChanImage = firstChanImage.convertToFloatProcessor()  //Needed to handle big numbers
    logger.info("after converttofloat")
    ch1Pixels = firstChanImage.getPixels()

    //Create a mask so that only the pixels we want from the bounding box area are used in calculations
    bpSLICs = createObjectMask(pathImage, it, objectType).getPixels()
    
    int size = ch1Pixels.size()
    secondChanImage= imp.getStack().getProcessor(SECOND_CHANNEL)
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

    name = measurement1.getValue().toString()+"+"+measurement2.getValue().toString()+" "+objectType+" R^2"

    it.getMeasurementList().putMeasurement(name, r2)
    
  }
}

Platform.runLater {

    def stage = new Stage()
    stage.initOwner(QuPathGUI.getInstance().getStage())
    stage.setScene(new Scene( gridPane))

    stage.setTitle("R-squared between two Channels")
    stage.setWidth(600);
    stage.setHeight(200);
    //stage.setResizable(false);

    stage.show()

}


def createObjectMask(PathImage pathImage, PathObject object, String objectType) {
    //create a byteprocessor that is the same size as the region we are analyzing
    def bp = new ByteProcessor(pathImage.getImage().getWidth(), pathImage.getImage().getHeight())
    //create a value to fill into the "good" area
    bp.setValue(1.0)

    if (objectType == "nucleus"){
        def roi = object.getNucleusROI()
        def roiIJ = IJTools.convertToIJRoi(roi, pathImage)
        bp.fill(roiIJ)
        
    }else if (objectType == "cytoplasm"){
        def nucleus = object.getNucleusROI()
        roiIJNuc = IJTools.convertToIJRoi(nucleus, pathImage)
        def roi = object.getROI()
        //fill in the whole cell area
        def roiIJ = IJTools.convertToIJRoi(roi, pathImage)
        bp.fill(roiIJ)
        //remove the nucleus
        bp.setValue(0)
        bp.fill(roiIJNuc)
        
    } else { 
        def roi = object.getROI()
        roiIJ = IJTools.convertToIJRoi(roi, pathImage)
        bp.fill(roiIJ)
    }

    
    //fill the ROI with the setValue to create the mask, the other values should be 0
    
    return bp
}

import javafx.application.Platform
import javafx.beans.property.SimpleLongProperty
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.TableColumn
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.control.Tooltip
import javafx.stage.Stage
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