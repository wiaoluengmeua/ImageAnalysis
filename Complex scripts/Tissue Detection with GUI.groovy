//v3.8
//This version REMOVES any current annotations. Comment out lines 30-31 ish to prevent this from happening.
//Important to note that you will almost certainly need to downsample significantly for any whole slide image.
//The script will error out VERY quickly otherwise, and I am not programmery enough to handle that cleanly. Crash away!
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
import javafx.scene.control.TableColumn
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.control.Tooltip
import javafx.stage.Stage
import qupath.lib.gui.QuPathGUI
import qupath.imagej.plugins.ImageJMacroRunner
import qupath.lib.plugins.parameters.ParameterList
import qupath.lib.roi.*
import qupath.lib.objects.*


def imageData = getCurrentImageData()
def server = imageData.getServer()

//Initially clear all objects and create a whole image annotation. You could instead delete this annotation and create your own
clearAllObjects()
createSelectAllObject(true);
getAnnotationObjects().each{it.setLocked(true)}
//calculate bit depth for initially suggested upper threhsold
int maxPixel = Math.pow((double) 2,(double)server.getBitsPerPixel())-1
def pixelSize = server.getPixelHeightMicrons()

//Some values for setting up the dialog box
int col = 0
int row = 0
int textFieldWidth = 100
int labelWidth = 150
def gridPane = new GridPane()
gridPane.setPadding(new Insets(10, 10, 10, 10));
gridPane.setVgap(5);
gridPane.setHgap(10);


def titleLabel = new Label("Adjust the current annotation or create a new one.\nMultiple overlapping annotations are not recommended")
gridPane.add(titleLabel,col, row++, 2, 1)
titleLabel.setTooltip(new Tooltip("The script automatically clears all objects\n and creates a whole image annotation.\n You may create your own annotations\n before clicking run, but non-rectangle\n annotations may exhibit unexpected behavior."))


//Checkbox for splitting annotations
def checkLabel = new Label("Split unconnected annotations")
gridPane.add(checkLabel,col++, row, 1, 1)
def splitBox = new CheckBox();
gridPane.add(splitBox, col++, row++,1,1)


//Downsample test section-extra spaces->terrible way to determine column width!
col=0
def downsampleLabel = new Label("Downsample:                                        ")
downsampleLabel.setTooltip(new Tooltip("Increase this if you get an error trying to export the image to ImageJ"));
downsampleLabel.setMinWidth(labelWidth)
def TextField downsampleText = new TextField("8.0");
downsampleText.setMaxWidth( textFieldWidth);
downsampleText.setAlignment(Pos.CENTER_RIGHT)
gridPane.add(downsampleLabel,col++, row, 1, 1)
gridPane.add(downsampleText,col, row++, 1, 1)

//reset the column count whenever starting a new row
col = 0
//Sigma test section
def sigmaLabel = new Label("Sigma:  ")
sigmaLabel.setTooltip(new Tooltip("Lower the sigma to remove empty space around annotations, raise it to remove empty spaces within the annotation.\nApplies a gaussian blur."));
def TextField sigmaText = new TextField("4.0");
sigmaText.setMaxWidth( textFieldWidth);
sigmaText.setAlignment(Pos.CENTER_RIGHT)
gridPane.add(sigmaLabel, col++, row, 1, 1)
gridPane.add(sigmaText, col, row++, 1, 1)

col = 0
//lowerThreshold section
def lowerThresholdLabel = new Label("Lower Threshold:  ")
lowerThresholdLabel.setTooltip(new Tooltip("No annotation usually means the threshold is too high, full image annotation means the threshold is too low"));
def TextField lowerThresholdText = new TextField("20");
lowerThresholdText.setMaxWidth( textFieldWidth);
lowerThresholdText.setAlignment(Pos.CENTER_RIGHT)
gridPane.add(lowerThresholdLabel, col++, row, 1, 1)
gridPane.add(lowerThresholdText, col, row++, 1, 1)

//upperThreshold section
col=0
def upperThresholdLabel = new Label("Upper Threshold:  ")
upperThresholdLabel.setTooltip(new Tooltip("Default is the max bit depth -1"));
def TextField upperThresholdText = new TextField(maxPixel.toString());
upperThresholdText.setMaxWidth( textFieldWidth);
upperThresholdText.setAlignment(Pos.CENTER_RIGHT)
gridPane.add(upperThresholdLabel, col++, row, 1, 1)
gridPane.add(upperThresholdText, col, row++, 1, 1)


def channelLabel = new Label("Final weights will be normalized.")
channelLabel.setTooltip(new Tooltip("I am so bad at programming"));
gridPane.add(channelLabel, 0, row, 1, 1)
def channelLabel2 = new Label("Channel Weights")
channelLabel2.setTooltip(new Tooltip("Any non-negative values"));
gridPane.add(channelLabel2, 1, row++, 1, 1)
//Set up rows for data entry for each fluorescent channel
def channels = []
//Variable to track channel count
int c = 0
ArrayList<Label> channelLabels 
ArrayList<TextField> channelWeights
//Pretty sure these could be lists
if (!imageData.getServer().isRGB()) {
    channels = getQuPath().getViewer().getImageDisplay().getAvailableChannels()
    channelLabels = new ArrayList(channels.size())
    channelWeights = new ArrayList(channels.size())
    for (channel in channels) {

        channelLabels.add( new Label(channel.toString()))
        channelWeights.add( new TextField((1/channels.size()).toString()));
        channelWeights[c].setMaxWidth( textFieldWidth);
        channelWeights[c].setAlignment(Pos.CENTER_RIGHT)
    
        //Add to dialog box, new row for each
        col=0
        gridPane.add(channelLabels[c], col++, row, 1, 1)
        gridPane.add(channelWeights.get(c), col, row++, 1, 1)
        c++
    }
    
} else {
    //Sloppy but it works to get RGB images included
    channels = ["Red","Green","Blue"]
    channelLabels = new ArrayList(3)
    channelWeights = new ArrayList(3)
    for (channel in channels) {
        channelLabels.add( new Label(channel))
        channelWeights.add( new TextField((1/channels.size()).toString()));
        channelWeights[c].setMaxWidth( textFieldWidth);
        channelWeights[c].setAlignment(Pos.CENTER_RIGHT)
    
        //Add to dialog box, new row for each
        col=0
        gridPane.add(channelLabels[c], col++, row, 1, 1)
        gridPane.add(channelWeights.get(c), col, row++, 1, 1)
        c++
    }
   
}


//Cycle through all channels to set up most of the rest of the dialog box


def runButtonLabel = new Label("This button will always run\n regardless of error message->\nLarge images may be slow.\nPrimarily intended for\nfluorescent images.")
gridPane.add(runButtonLabel, 0, row, 1, 1)
//Finally create a run button to start everything
Button runButton = new Button()
runButton.setText("Run")
gridPane.add(runButton, 1, row++, 1, 1)
runButton.setTooltip(new Tooltip("This may take a little bit of time depending on image size and downsampling."));


runButton.setOnAction {

    originalAnnotations = getAnnotationObjects()
    //At the moment I don't think any of these values should need anything larger than a float... though if greater bit depths are used this might need changing
    float downsample = Float.parseFloat(downsampleText.getText());
    float sigma = Float.parseFloat(sigmaText.getText());
    float lowerThreshold = Float.parseFloat(lowerThresholdText.getText());
    float upperThreshold = Float.parseFloat(upperThresholdText.getText());
    def weights = []
    //Place all of the final weights into an array that can be read into ImageJ
    for (i=0;i<channels.size();i++){
        weights.add(Float.parseFloat(channelWeights.get(i).getText()))
    }
    //Normalize weights
    def sum = weights.sum()
    if (sum<=0){
        print "Please use positive weights"
        runButton.setText("Weight error.")
        return;
    }
    for (i=0; i<weights.size(); i++){
        weights[i] = weights[i]/sum
    }
    
    //[1,2,3,4] format can't be read into ImageJ arrays (or at least I didn't see an easy way), it needs to be converted to 1,2,3,4
    def weightList =weights.join(", ")
    //Get rid of everything already in the image.  Not totally necessary, but useful when I am spamming various values.
    def annotations = getAnnotationObjects()

    def params = new ImageJMacroRunner(getQuPath()).getParameterList()

    // Change the value of a parameter, using the JSON to identify the key
    params.getParameters().get('downsampleFactor').setValue(downsample)
    params.getParameters().get('sendROI').setValue(false)
    params.getParameters().get('sendOverlay').setValue(false)
    params.getParameters().get('getOverlay').setValue(false)
    if (!getQuPath().getClass().getPackage()?.getImplementationVersion()){
        params.getParameters().get('getOverlayAs').setValue('Annotations')
    }
    params.getParameters().get('getROI').setValue(true)
    params.getParameters().get('clearObjects').setValue(false)

    // Get the macro text and other required variables
    def macro ='original = getImageID();run("Duplicate...", "title=X3t4Y6lEt duplicate");'+
    'weights=newArray('+weightList+');run("Stack to Images");name=getTitle();'+
    'baseName = substring(name, 0, lengthOf(name)-1);'+
    'for (i=0; i<'+channels.size()+';'+
    'i++){currentImage = baseName+(i+1);selectWindow(currentImage);'+
    'run("Multiply...", "value="+weights[i]);}'+
    'run("Images to Stack", "name=Stack title=[X3t4Y6lEt] use");'+
    'run("Z Project...", "projection=[Sum Slices]");'+
    'run("Gaussian Blur...", "sigma='+sigma+'");'+
    'setThreshold('+lowerThreshold+', '+upperThreshold+');run("Convert to Mask");'+
    'run("Create Selection");run("Colors...", "foreground=white background=black selection=white");'+
    'run("Properties...", "channels=1 slices=1 frames=1 unit=um pixel_width='+pixelSize+' pixel_height='+pixelSize+' voxel_depth=1");'+
    'selectImage(original);run("Restore Selection");'

    def macroRGB = 'weights=newArray('+weightList+');'+
    'original = getImageID();run("Duplicate...", " ");'+
    'run("Make Composite");run("Stack to Images");'+
    'selectWindow("Red");rename("Red X3t4Y6lEt");run("Multiply...", "value="+weights[0]);'+
    'selectWindow("Green");rename("Green X3t4Y6lEt");run("Multiply...", "value="+weights[1]);'+
    'selectWindow("Blue");rename("Blue X3t4Y6lEt");run("Multiply...", "value="+weights[2]);'+
    'run("Images to Stack", "name=Stack title=[X3t4Y6lEt] use");'+
    'run("Z Project...", "projection=[Sum Slices]");'+
    'run("Gaussian Blur...", "sigma='+sigma+'");'+
    'setThreshold('+lowerThreshold+', '+upperThreshold+');run("Convert to Mask");'+
    'run("Create Selection");run("Colors...", "foreground=white background=black selection=cyan");'+
    'run("Properties...", "channels=1 slices=1 frames=1 unit=um pixel_width='+pixelSize+' pixel_height='+pixelSize+' voxel_depth=1");'+
    'selectImage(original);run("Restore Selection");'


    for (annotation in annotations) {
        //Check if we need to use the RGB version
        if (imageData.getServer().isRGB()) {
            ImageJMacroRunner.runMacro(params, imageData, null, annotation, macroRGB)
        } else{ ImageJMacroRunner.runMacro(params, imageData, null, annotation, macro)}
    }

    //remove whole image annotation and lock the new annotation
    removeObjects(annotations,true)

    if (splitBox.isSelected()){
        def areaAnnotations = getAnnotationObjects().findAll {it.getROI() instanceof AreaROI}

        areaAnnotations.each { selected ->
            def polygons = PathROIToolsAwt.splitAreaToPolygons(selected.getROI())
            def newPolygons = polygons[1].collect {
                updated = it
                    for (hole in polygons[0])
                        updated = PathROIToolsAwt.combineROIs(updated, hole, PathROIToolsAwt.CombineOp.SUBTRACT)
            return updated
            }

        // Remove original annotation, add new ones
        annotations = newPolygons.collect {new PathAnnotationObject(it)}
        resetSelection()
        removeObject(selected, true)
        addObjects(annotations)

        }
    }
    
    //Otherwise setLocked generates an error if no annotation was created
    getAnnotationObjects().each{it.setLocked(true)}
    
    runButton.setText("Run again?")

}

//Reset button to keep re-trying the same beginning annotation rather than continuing within resulting annotation
Button resetButton = new Button()
resetButton.setText("Reset?")
gridPane.add(resetButton, 0, ++row, 1, 1)
resetButton.setTooltip(new Tooltip("Clears all annotations and creates the pre-Run annotation."));

resetButton.setOnAction {
    clearAllObjects()
    addObjects(originalAnnotations)
    getAnnotationObjects().each{it.setLocked(true)}
}

def warningLabel = new Label("These buttons will split your annotations regardless\nof checkbox at top.")
gridPane.add(warningLabel, 0, ++row, 2, 1)


//Option to remove small sized annotation areas. Requires pixel size

Button clipButton = new Button()
clipButton.setText("Remove Small")
gridPane.add(clipButton, 0, ++row, 1, 1)
clipButton.setTooltip(new Tooltip("Remove annotations below the indicated area IN SQUARE MICRONS.\nHave not made a version that works for this without pixel size."));

def TextField clipSizeText = new TextField("50");
clipSizeText.setMaxWidth( textFieldWidth);
clipSizeText.setAlignment(Pos.CENTER_RIGHT)
gridPane.add(clipSizeText, 1, row, 1, 1)
clipSizeText.setTooltip(new Tooltip("Remove annotations below the indicated area IN SQUARE MICRONS.\nHave not made a version that works for this without pixel size."));

//Clip button goes with the Remove Small button on the dialog, to remove objects below the text box amount in um^2
clipButton.setOnAction {
        def areaAnnotations = getAnnotationObjects().findAll {it.getROI() instanceof AreaROI}

        for (section in areaAnnotations){
            
            def polygons = PathROIToolsAwt.splitAreaToPolygons(section.getROI())
            def newPolygons = polygons[1].collect {
                updated = it
                for (hole in polygons[0])
                    updated = PathROIToolsAwt.combineROIs(updated, hole, PathROIToolsAwt.CombineOp.SUBTRACT)
                return updated
            }
                    // Remove original annotation, add new ones
        annotations = newPolygons.collect {new PathAnnotationObject(it)}
        
        removeObject(section, true)
        addObjects(annotations)

            
        }




    //PART2


    double pixelWidth = server.getPixelWidthMicrons()
    double pixelHeight = server.getPixelHeightMicrons()
    def smallAnnotations = getAnnotationObjects().findAll {it.getROI().getScaledArea(pixelWidth, pixelHeight) < Double.parseDouble(clipSizeText.getText());}
    println("small "+smallAnnotations)
    removeObjects(smallAnnotations, true)
    fireHierarchyUpdate()


}

//Fill holes option

Button fillButton = new Button()
fillButton.setText("Fill holes")
gridPane.add(fillButton, 0, ++row, 1, 1)
fillButton.setTooltip(new Tooltip("Fill in annotation holes less than the indicated area IN SQUARE MICRONS.\nHave not made a version that works for this without pixel size."));

def TextField fillSizeText = new TextField("50");
fillSizeText.setMaxWidth( textFieldWidth);
fillSizeText.setAlignment(Pos.CENTER_RIGHT)
gridPane.add(fillSizeText, 1, row, 1, 1)
fillSizeText.setTooltip(new Tooltip("Fill in annotations holes less than the indicated area IN SQUARE MICRONS.\nHave not made a version that works for this without pixel size."));

//Clip button goes with the Remove Small button on the dialog, to remove objects below the text box amount in um^2
fillButton.setOnAction {
    // Get selected objects
    // If you're willing to loop over all annotation objects, for example, then use getAnnotationObjects() instead
    def pathObjects = getAnnotationObjects()

    // Create a list of objects to remove, add their replacements
    def toRemove = []
    def toAdd = []
    for (pathObject in pathObjects) {
        def roi = pathObject.getROI()
        // AreaROIs are the only kind that might have holes
        if (roi instanceof AreaROI ) {
            // Extract exterior polygons
            def polygons = PathROIToolsAwt.splitAreaToPolygons(roi)[1] as List
            // If we have multiple polygons, merge them
            def roiNew = polygons.remove(0)
            def roiNegative = PathROIToolsAwt.splitAreaToPolygons(roi)[0] as List
            for (temp in polygons){
                roiNew = PathROIToolsAwt.combineROIs(temp, roiNew, PathROIToolsAwt.CombineOp.ADD)
            }
            for (temp in roiNegative){  
                if (temp.getArea() > Double.parseDouble(fillSizeText.getText())/pixelSize/pixelSize){
                    roiNew = PathROIToolsAwt.combineROIs(roiNew, temp, PathROIToolsAwt.CombineOp.SUBTRACT)
                }
            }
            // Create a new annotation
            toAdd << new PathAnnotationObject(roiNew, pathObject.getPathClass())
            toRemove << pathObject
        }
}

// Remove & add objects as required
def hierarchy = getCurrentHierarchy()
hierarchy.getSelectionModel().clearSelection()
hierarchy.removeObjects(toRemove, true)
hierarchy.addPathObjects(toAdd, false)

}

//Some stuff that controls the dialog box showing up. I don't really understand it but it is needed.
Platform.runLater {

    def stage = new Stage()
    stage.initOwner(QuPathGUI.getInstance().getStage())
    stage.setScene(new Scene( gridPane))
    stage.setTitle("Another Tissue Detection ")
    stage.setWidth(350);
    stage.setHeight(800);
    //stage.setResizable(false);

    stage.show()

}
