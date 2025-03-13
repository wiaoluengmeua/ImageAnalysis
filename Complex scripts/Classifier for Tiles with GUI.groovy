//V1 Edited slightly to work for tiles/SLICs

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
import javafx.scene.control.ColorPicker
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.control.Tooltip
import javafx.stage.Stage
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.helpers.ColorToolsFX;
import javafx.scene.paint.Color;
//Settings to control the dialog boxes for the GUI
int col = 0
int row = 0
int textFieldWidth = 120
int labelWidth = 150
def gridPane = new GridPane()
gridPane.setPadding(new Insets(10, 10, 10, 10));
gridPane.setVgap(2);
gridPane.setHgap(10);
def server = getCurrentImageData().getServer()
//Upper thresholds will default to the max bit depth, since that is likely the most common upper limit for a given image.
maxPixel = Math.pow((double) 2,(double)server.getBitsPerPixel())-1
positive = []
//print(maxPixel)
def titleLabel = new Label("Intended for use where one marker determines a base class.\nFor example, you could use Channel 1 Cytoplasmic Mean and Channel 2 Nuclear Mean\nto generate two base classes and a Double positive class where each condition is true.\n\n")
gridPane.add(titleLabel,col, row++, 3, 1)

def requestLabel = new Label("How many base classes/single measurements are you interested in?\nThe above example would have two.\n")
gridPane.add(requestLabel,col, row++, 3, 1)

def TextField classText = new TextField("2");
classText.setMaxWidth( textFieldWidth);
classText.setAlignment(Pos.CENTER_RIGHT)
gridPane.add(classText, col++, row, 1, 1)

//ArrayList<Label> channelLabels 
Button startButton = new Button()
startButton.setText("Start Classifying")
gridPane.add(startButton, col, row++, 1, 1)
startButton.setTooltip(new Tooltip("If you need to change the number of classes, re-run the script"));
col = 0
row+=10 //spacer

def loadLabel = new Label("Load a classifier:")
gridPane.add(loadLabel,col++, row, 2, 1)
def TextField classFile = new TextField("MyClassifier");
classFile.setMaxWidth( textFieldWidth);
classFile.setAlignment(Pos.CENTER_RIGHT)
gridPane.add( classFile, col++, row, 1, 1)

Button loadButton = new Button()
loadButton.setText("Load Classifier")
gridPane.add(loadButton, col++, row++, 1, 1)

//incredibly lazy and sloppy coding, just a copy and paste taking slightly different inputs
loadButton.setOnAction{
    path = buildFilePath(PROJECT_BASE_DIR, "classifiers",classFile.getText())
    new File(path).withObjectInputStream {
        cObj = it.readObject()
    }
        //Create an arraylist with the same number of entries as classes
    CHANNELS = cObj.size()

    col = 0
    row = 0
    def secondGridPane = new GridPane()
    secondGridPane.setPadding(new Insets(10, 10, 10, 10));
    secondGridPane.setVgap(2);
    secondGridPane.setHgap(10);
    def assist = new Label("Short Class Names are recommended as dual positives and beyond use the full names of all positive classes.\n ")
    secondGridPane.add(assist, col, row++, 5, 1)

    def mChoice = new Label("Measurement                                                                    ")
    mChoice.setMaxWidth(400)
    mChoice.setAlignment(Pos.CENTER_RIGHT)
    def mLowThresh = new Label("Lower Threshold   <=  ")
    def mHighThresh = new Label("<= Upper Threshold  ")
    def mClassName = new Label("Class Name      ")
    
    secondGridPane.add( mChoice, col++, row, 1,1)
    secondGridPane.add( mLowThresh, col++, row, 1,1)
    secondGridPane.add( mClassName, col++, row, 1,1)
    secondGridPane.add( mHighThresh, col, row++, 1,1)


    //create data structures to use for building the classifier

    boxes = new ComboBox [CHANNELS]
    lowerTs = new TextField [CHANNELS]
    classList = new TextField [CHANNELS]
    upperTs = new TextField [CHANNELS]
    colorPickers = new ColorPicker [CHANNELS]
    //create the dialog where the user will select the measurements of interest and values
    for (def i=0; i<CHANNELS;i++) {
        col =0

        //Add to dialog box, new row for each
        boxes[i] = new ComboBox()
        qupath.lib.classifiers.PathClassificationLabellingHelper.getAvailableFeatures(getDetectionObjects()).each {boxes[i].getItems().add(it) }
        boxes[i].setValue(cObj[i][0])
        classList[i] = new TextField(cObj[i][2])
        lowerTs[i] = new TextField(cObj[i][1])
        upperTs[i] = new TextField(cObj[i][3])
        classList[i].setMaxWidth( textFieldWidth);
        classList[i].setAlignment(Pos.CENTER_RIGHT)
        lowerTs[i].setMaxWidth( textFieldWidth);
        lowerTs[i].setAlignment(Pos.CENTER_RIGHT)
        upperTs[i].setMaxWidth( textFieldWidth);
        upperTs[i].setAlignment(Pos.CENTER_RIGHT)
        colorPickers[i] = new ColorPicker(Color.web(cObj[i][4]))
       
        secondGridPane.add(boxes[i], col++, row, 1,1)
        secondGridPane.add(lowerTs[i], col++, row, 1,1)
        secondGridPane.add(classList[i], col++, row, 1, 1)
        secondGridPane.add(upperTs[i], col++, row, 1,1)
        secondGridPane.add(colorPickers[i], col++, row++, 1,1)
        
        
    }
    Button runButton = new Button()
    runButton.setText("Run Classifier")
    secondGridPane.add(runButton, 0, row++, 1, 1)

    //All stuff for actually classifying cells
    runButton.setOnAction {
        //set up for classifier
        def cells = getDetectionObjects()
        
        cells.each {it.setPathClass(getPathClass('Negative'))}
        startTime = System.currentTimeMillis()
        //start classifier with all cells negative

        for (def i=0; i<CHANNELS; i++){
            def lower = Float.parseFloat(lowerTs[i].getText())
            def upper = Float.parseFloat(upperTs[i].getText())
            //create lists for each measurement, classify cells based off of those measurements
            positive[i] = cells.findAll {measurement(it, boxes[i].getValue()) >= lower && measurement(it, boxes[i].getValue()) <= upper}
            positive[i].each {it.setPathClass(getPathClass(classList[i].getText()+' positive')); it.getMeasurementList().putMeasurement("ClassDepth", 1)}
            c = colorPickers[i].getValue()
            currentPathClass = getPathClass(classList[i].getText()+' positive')
            //for some reason setColor needs to be used here instead of setColorRGB which applies to objects and not classes?
            currentPathClass.setColor(ColorToolsFX.getRGBA(c))
        }
        //Call the classifier on each list of positive single class cells, except for the last one!
        for (def i=0; i<(CHANNELS-1); i++){
            println("ROUND "+i)
            int remaining = 0
            for (def j = i+1; j<CHANNELS; j++){
                remaining +=1
            }
            //println("SENDING CELLS TO CLASSIFIER "+positive[i].size())
            depth = 2
            classifier(classList[i].getText(), positive[i], remaining, i)

        }
        println("clasifier done")
        fireHierarchyUpdate()
    }
    //end Run Button
    row+=10 //spacer
    Button saveButton = new Button()
    saveButton.setText("Save Classifier")
    secondGridPane.add(saveButton, 1, row, 1, 1)
    def TextField saveFile = new TextField("MyClassifier");
    saveFile.setMaxWidth( textFieldWidth);
    saveFile.setAlignment(Pos.CENTER_RIGHT)
    secondGridPane.add( saveFile, 2, row++, 1, 1)

    

    //All stuff for actually classifying cells
    saveButton.setOnAction {
        def export = []

        for (def l=0; l<CHANNELS;l++){
           export << [boxes[l].getValue(), lowerTs[l].getText(), classList[l].getText(), upperTs[l].getText(), colorPickers[l].getValue().toString()]

        }

        mkdirs(buildFilePath(PROJECT_BASE_DIR, "classifiers"))
        path = buildFilePath(PROJECT_BASE_DIR, "classifiers",saveFile.getText())
        new File(path).withObjectOutputStream {
            it.writeObject(export)
        }
    }
//End of classifier window
    Platform.runLater {

        def stage3 = new Stage()
        stage3.initOwner(QuPathGUI.getInstance().getStage())
        stage3.setScene(new Scene( secondGridPane))
        stage3.setTitle("Loaded Classifier "+classFile.getText())
        stage3.setWidth(870);
        stage3.setHeight(900);
        //stage.setResizable(false);

        stage3.show()

    }
}


//end of the loaded classifier

startButton.setOnAction {
    col = 0
    row = 0
    //Create an arraylist with the same number of entries as classes
    CHANNELS = Float.parseFloat(classText.getText())
    //channelLabels = new ArrayList( CHANNELS)
    
    def secondGridPane = new GridPane()
    secondGridPane.setPadding(new Insets(10, 10, 10, 10));
    secondGridPane.setVgap(2);
    secondGridPane.setHgap(10);
    def assist = new Label("Short Class Names are recommended as dual positives and beyond use the full names of all positive classes.\n ")
    secondGridPane.add(assist, col, row++, 5, 1)

    def mChoice = new Label("Measurement                                                                    ")
    mChoice.setMaxWidth(400)
    mChoice.setAlignment(Pos.CENTER_RIGHT)
    def mLowThresh = new Label("Lower Threshold   <=  ")
    def mHighThresh = new Label("<= Upper Threshold  ")
    def mClassName = new Label("Class Name      ")
    
    secondGridPane.add( mChoice, col++, row, 1,1)
    secondGridPane.add( mLowThresh, col++, row, 1,1)
    secondGridPane.add( mClassName, col++, row, 1,1)
    secondGridPane.add( mHighThresh, col, row++, 1,1)


    //create data structures to use for building the classifier

    boxes = new ComboBox [CHANNELS]
    lowerTs = new TextField [CHANNELS]
    classList = new TextField [CHANNELS]
    upperTs = new TextField [CHANNELS]
    colorPickers = new ColorPicker [CHANNELS]
    //create the dialog where the user will select the measurements of interest and values
    for (def i=0; i<CHANNELS;i++) {
        col =0
        //Add to dialog box, new row for each
        boxes[i] = new ComboBox()
        qupath.lib.classifiers.PathClassificationLabellingHelper.getAvailableFeatures(getDetectionObjects()).each {boxes[i].getItems().add(it) }
        classList[i] = new TextField("C" + (i+1))
        lowerTs[i] = new TextField("0")
        upperTs[i] = new TextField(maxPixel.toString())
        classList[i].setMaxWidth( textFieldWidth);
        classList[i].setAlignment(Pos.CENTER_RIGHT)
        lowerTs[i].setMaxWidth( textFieldWidth);
        lowerTs[i].setAlignment(Pos.CENTER_RIGHT)
        upperTs[i].setMaxWidth( textFieldWidth);
        upperTs[i].setAlignment(Pos.CENTER_RIGHT)
        colorPickers[i] = new ColorPicker()
                
        secondGridPane.add(boxes[i], col++, row, 1,1)
        secondGridPane.add(lowerTs[i], col++, row, 1,1)
        secondGridPane.add(classList[i], col++, row, 1, 1)
        secondGridPane.add(upperTs[i], col++, row, 1,1)
        secondGridPane.add(colorPickers[i], col++, row++, 1,1)
        
        
    }
    Button runButton = new Button()
    runButton.setText("Run Classifier")
    secondGridPane.add(runButton, 0, row++, 1, 1)

    //All stuff for actually classifying cells
    runButton.setOnAction {
        //set up for classifier
        def cells = getDetectionObjects()
        
        cells.each {it.setPathClass(getPathClass('Negative'))}

        //start classifier with all cells negative
        
        for (def i=0; i<CHANNELS; i++){
            def lower = Float.parseFloat(lowerTs[i].getText())
            def upper = Float.parseFloat(upperTs[i].getText())
            //create lists for each measurement, classify cells based off of those measurements
            positive[i] = cells.findAll {measurement(it, boxes[i].getValue()) >= lower && measurement(it, boxes[i].getValue()) <= upper}
            positive[i].each {it.setPathClass(getPathClass(classList[i].getText()+' positive')); it.getMeasurementList().putMeasurement("ClassDepth", 1)}
            c = colorPickers[i].getValue()
            currentPathClass = getPathClass(classList[i].getText()+' positive')
            //for some reason setColor needs to be used here instead of setColorRGB which applies to objects and not classes?
            currentPathClass.setColor(ColorToolsFX.getRGBA(c))
        }
        //Call the classifier on each list of positive single class cells, except for the last one!
        for (def i=0; i<(CHANNELS-1); i++){
            println("ROUND "+i)
            int remaining = 0
            for (def j = i+1; j<CHANNELS; j++){
                remaining +=1
            }
            //println("SENDING CELLS TO CLASSIFIER "+positive[i].size())
            depth = 2
            classifier(classList[i].getText(), positive[i], remaining, i)

        }
        println("clasifier done")
        fireHierarchyUpdate()
    }
    //end Run Button
//////////////////////////
    row+=10 //spacer
    Button saveButton = new Button()
    saveButton.setText("Save Classifier")
    secondGridPane.add(saveButton, 1, row, 1, 1)
    def TextField saveFile = new TextField("MyClassifier");
    saveFile.setMaxWidth( textFieldWidth);
    saveFile.setAlignment(Pos.CENTER_RIGHT)
    secondGridPane.add( saveFile, 2, row++, 1, 1)

    

    //All stuff for actually classifying cells
    saveButton.setOnAction {
        def export = []
        for (def l=0; l<CHANNELS;l++){
             
            export << [boxes[l].getValue(), lowerTs[l].getText(), classList[l].getText(), upperTs[l].getText(), colorPickers[l].getValue().toString()]

        }

        mkdirs(buildFilePath(PROJECT_BASE_DIR, "classifiers"))
        path = buildFilePath(PROJECT_BASE_DIR, "classifiers",saveFile.getText())
        new File(path).withObjectOutputStream {
            it.writeObject(export)
        }
    }
    
//////////////////////
//End of classifier window
    Platform.runLater {

        def stage2 = new Stage()
        stage2.initOwner(QuPathGUI.getInstance().getStage())
        stage2.setScene(new Scene( secondGridPane))
        stage2.setTitle("Build Classifier ")
        stage2.setWidth(870);
        stage2.setHeight(900);
        //stage.setResizable(false);

        stage2.show()

    }
}



//Some stuff that controls the dialog box showing up. I don't really understand it but it is needed.
Platform.runLater {

    def stage = new Stage()
    stage.initOwner(QuPathGUI.getInstance().getStage())
    stage.setScene(new Scene( gridPane))
    stage.setTitle("Simple Classifier for Multiple Classes ")
    stage.setWidth(550);
    stage.setHeight(300);
    //stage.setResizable(false);

    stage.show()

}

//Recursive function to keep track of what needs to be classified next.
//listAName is the current classifier name (for example Class 1 during the first pass) which gets modified with the intersect
//and would result in cells from this pass being called Class 1,Class2 positive.
//listA is the current list of cells being checked for intersection with the first member of...
//remainingListSize is the number of lists in "positive[]" that the current list needs to be checked against
//position keeps track of the starting position of listAName class. So on the first runthrough everything will start with C1
//The next runthrough will start with position 2 since the base class will be C2
 void classifier (listAName, listA, remainingListSize, position = 0){
    //println("listofLists " +remainingListSize)
    //println("base list size"+listA.size())
    for (def y=0; y <remainingListSize; y++){
        //println("listofLists in loop" +remainingListSize)
        //println("y "+y)
        //println("depth"+depth)
        k = (position+y+1).intValue()
        //println("k "+k)
    // get the measurements needed to determine if a cell is a member of the next class (from listOfLists)
        def lower = Float.parseFloat(lowerTs[k].getText())
        def upper = Float.parseFloat(upperTs[k].getText())
    //intersect the listA with the first of the listOfLists
    //on the first run, this would take all of Class 1, and compare it with measurements that determine Class 2, resulting in a subset of 
    //Class 1 that meet both criteria
        def passList = listA.findAll {measurement(it, boxes[k].getValue()) >= lower && measurement(it, boxes[k].getValue()) <= upper}
        newName = classList[k].getText()
    
    //Create a new name based off of the current name and the newly compared class
    // on the first runthrough this would give "Class 1,Class 2 positive"
        def mergeName = listAName+","+newName
        //println("depth "+depth)
        //println(mergeName+" with number of remaining lists "+remainingListSize)
        passList.each{
            //Check if class being applies is "shorter" than the current class. 
            //This prevents something like "C2,C3" from overwriting "C1,C2,C3,C4" from the first call.
            if (it.getMeasurementList().getMeasurementValue("ClassDepth") < depth) {
                it.setPathClass(getPathClass(mergeName+' positive')); 
                it.getMeasurementList().putMeasurement("ClassDepth", depth)
            }
        }
        
        if (k == (positive.size()-1)){ 
            //If we are comparing the current list to the last positive class list, we are done
            //Go up one level of classifier depth and return
            depth -=1
            return;
        } else{ 
            //Otherwise, move one place further along the "positive" list of base classes, and increase depth
            //This happens when going from C1,C2 to C1,C2,C3 etc.
            def passAlong = remainingListSize-1
            //println("passAlong "+passAlong.size())
            //println("name for next " +mergeName)
            depth +=1
            classifier(mergeName, passList, passAlong, k)
        }
        //println("loopy depth"+depth)
    }
 }