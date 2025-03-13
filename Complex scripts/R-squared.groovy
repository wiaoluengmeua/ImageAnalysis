//Version 5 with plots and data export! Updated for 0.2.3
//Note, saving process is VERY simple, and will error out if you attempt to overwrite an open file
//It will also continue to append to an existing file rather than overwrite it.

//Not sure what happens if your sum values go over the size of a "double"



Logger logger = LoggerFactory.getLogger(QuPathGUI.class);
folder = buildFilePath(PROJECT_BASE_DIR, "R-squared results")

//Settings to control the dialog boxes for the GUI
int col = 0
int row = 0
int textFieldWidth = 120
int labelWidth = 150
def gridPane = new GridPane()
def dataStorage = []
dataStorage <<["Measurement1", "Measurement2", "Class", "R-squared", "Slope", "Intercept"]
gridPane.setPadding(new Insets(10, 10, 10, 10));
gridPane.setVgap(2);
gridPane.setHgap(10);
boolean first = true;
def classifications = new ArrayList<>(getDetectionObjects().collect {it.getPathClass()} as Set)

int dec = 4 //decimal places for results
   
def checkClass = new CheckBox("All Classes")
checkClass.setSelected(true)
def mChoice = new Label("Measurement                                                                    ")
mChoice.setMaxWidth(400)
mChoice.setAlignment(Pos.CENTER_RIGHT)
def mChoice2 = new Label("Measurement2                                                                  ")
mChoice2.setMaxWidth(400)
mChoice2.setAlignment(Pos.CENTER_RIGHT)
def mClass = new Label("Class")
mClass.setMaxWidth(400)
mClass.setAlignment(Pos.CENTER_RIGHT)
def rsq = new Label("0")
rsq.setMaxWidth(400)
rsq.setAlignment(Pos.CENTER_LEFT)
def rsqLabel = new Label("R-squared:")
rsqLabel.setMaxWidth(400)
rsqLabel.setAlignment(Pos.CENTER_RIGHT)
//VBox vbox = new VBox()





gridPane.add( mChoice, col++, row, 1,1)
gridPane.add( mChoice2, col++, row, 1,1)
gridPane.add( mClass, col, row++, 1,1)

measurement1 = new ComboBox()
PathClassifierTools.getAvailableFeatures(getDetectionObjects()).each {measurement1.getItems().add(it) }
measurement2 = new ComboBox()
PathClassifierTools.getAvailableFeatures(getDetectionObjects()).each {measurement2.getItems().add(it) }
subsetClass = new ComboBox()
classifications.each {subsetClass.getItems().add(it) }
col=0
               
gridPane.add(measurement1, col++, row, 1,1)
gridPane.add(measurement2, col++, row, 1,1)
gridPane.add(subsetClass, col++, row, 1,1)
gridPane.add( checkClass, col, row++, 1,1)
Button runButton = new Button()
runButton.setText("Calculate")
Button saveButton = new Button()
saveButton.setText("Export all calculations to CSV")
saveButton.setTooltip(new Tooltip("Saves file to base Project folder directory"))
gridPane.add(runButton, 0, row, 1, 1)
gridPane.add(saveButton, 2, row++, 1, 1)
gridPane.add(rsqLabel, 0,row,1,1)
gridPane.add(rsq, 1,row++,1,1)



saveButton.setOnAction{
    
    fileName = getQuPath().getProject().getEntry(getQuPath().getImageData()).getImageName()+ " R-sq results.csv"

    checkFolder = new File(folder)

    if(!checkFolder.exists()){checkFolder.mkdirs()}

    path = buildFilePath(folder,fileName)
    File file = new File(path)
    for (x in dataStorage){
        x = x.toString()
        x = x.replaceAll("[\\[\\]]","")
        file.append(x)
        file.append('\n')
    }

}

    
runButton.setOnAction {
   //CALCULATE R2 HERE
    logger.info("Start")
    def gridPlot = new GridPane()
    gridPlot.setPadding(new Insets(10, 10, 10, 10));
    gridPlot.setVgap(2);
    gridPlot.setHgap(10);

    //find the objects that need to be analyzed. 
    if (checkClass.isSelected()){
        allCells = getQuPath().getImageData().getHierarchy().getDetectionObjects()
        logger.info("allclasses "+allCells.size())
        cells = allCells.findAll{it.getMeasurementList().containsNamedMeasurement(measurement1.getValue()) && it.getMeasurementList().containsNamedMeasurement(measurement2.getValue())};
        
    } else if (subsetClass.getValue()){ cells = getQuPath().getImageData().getHierarchy().getDetectionObjects().findAll{it.getPathClass() == getPathClass(subsetClass.getValue().toString()) && it.getMeasurementList().containsNamedMeasurement(measurement1.getValue()) && it.getMeasurementList().containsNamedMeasurement(measurement2.getValue()) }; logger.info("singleclass ")
    } else {logger.info("A class selection is needed"); return}
    if (cells.size() <1){logger.info("Need more objects to run R^2 analysis " + cells.size()); return}
    
    def points = new double [cells.size()-1][2]
    logger.info("Cycle through objects")
    for(i=0;i < cells.size()-1; i++){ 
       points[i][0] = measurement(cells[i], measurement1.getValue())
       points[i][1] = measurement(cells[i], measurement2.getValue())
    }
    println(measurement1.getValue().toString())
    println(measurement2.getValue().toString())
    //REGRESSION CALCULATION HERE
    def regression = new org.apache.commons.math3.stat.regression.SimpleRegression()
    regression.addData(points)
    double rSquared = regression.getRSquare()
    double intercept = regression.getIntercept()
    double slope = regression.getSlope()
    def xStats = Arrays.stream(points).mapToDouble({p -> p[0]}).summaryStatistics()
    def yStats = Arrays.stream(points).mapToDouble({p -> p[1]}).summaryStatistics()
    println(rSquared.trunc(4).toString()+"   slope :"+slope.trunc(4)+"   intercept :"+intercept.trunc(4))
    rsq.setText(rSquared.trunc(4).toString()+"   slope :"+slope.trunc(4)+"   intercept :"+intercept.trunc(4))

    NumberAxis xAxis = new NumberAxis(xStats.getMin(),xStats.getMax(),(xStats.getMax()-xStats.getMin())/10)
    xAxis.setLabel(measurement1.getValue())
    NumberAxis yAxis = new NumberAxis(yStats.getMin(),yStats.getMax(),(yStats.getMax()-yStats.getMin())/10)
    yAxis.setLabel(measurement2.getValue())
    ScatterChart<Number, Number> scatter = new ScatterChart<Number,Number>(xAxis,yAxis)
    XYChart.Series scatterPlot = new XYChart.Series()
    def data = []
    for(i=0;i < cells.size()-1; i++){
        data << new XYChart.Data(points[i][0], points[i][1])
        
    }
    scatterPlot.getData().addAll(data)
    
    if (checkClass.isSelected()){classCheck = "All Classes"
    } else {classCheck = subsetClass.getValue().toString()}
    scatterPlot.setName(classCheck) 
    
    scatter.setPrefSize(500, 400);
    scatter.getData().add(scatterPlot)


    def rsq2 = new Label(rSquared.trunc(dec).toString()+"   slope: "+slope.trunc(dec)+"   intercept: "+intercept.trunc(dec))
    rsq2.setMaxWidth(400)
    rsq2.setAlignment(Pos.CENTER_LEFT)
    def rsqLabel2 = new Label("R-squared:")
    rsqLabel2.setMaxWidth(400)
    rsqLabel2.setAlignment(Pos.CENTER_RIGHT)
    
    gridPlot.add(rsqLabel2, 0,0,1,1)
    gridPlot.add(rsq2, 1,0,1,1)
    gridPlot.add(scatter, 0,1,2,3)
    cleanClassName = classCheck.replace(",","_")
    println(cleanClassName)
    dataStorage << [measurement1.getValue().toString(), measurement2.getValue().toString(), cleanClassName, rSquared.trunc(dec), slope.trunc(dec), intercept.trunc(dec)]
    
    Platform.runLater {

        def stage2 = new Stage()
        stage2.initOwner(QuPathGUI.getInstance().getStage())
        stage2.setScene(new Scene( gridPlot))
        stage2.setTitle("Scatterplot ")
        stage2.setWidth(600);
        stage2.setHeight(500);
        //stage.setResizable(false);

        stage2.show()

    }

}


//Some stuff that controls the dialog box showing up. I don't really understand it but it is needed.
Platform.runLater {

    def stage = new Stage()
    stage.initOwner(QuPathGUI.getInstance().getStage())
    stage.setScene(new Scene( gridPane))

    stage.setTitle("Calculate R-squared between two measurements")
    stage.setWidth(1200);
    stage.setHeight(200);
    //stage.setResizable(false);

    stage.show()

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
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;