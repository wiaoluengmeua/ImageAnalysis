//0.1.2 - This is much easier to do through the interface or scripting in 0.2.0

//To use in 0.2.0 swap the ColorToolsFX import to import qupath.lib.gui.tools.ColorToolsFX
import javafx.application.Platform
import javafx.beans.property.SimpleLongProperty
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.control.ColorPicker
import javafx.scene.control.ComboBox
import javafx.scene.control.TableColumn
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.control.Tooltip
import javafx.stage.Stage
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.helpers.ColorToolsFX;
import javafx.scene.paint.Color;

int col = 0
int row = 0
int textFieldWidth = 120
int labelWidth = 150
def gridPane = new GridPane()
gridPane.setPadding(new Insets(10, 10, 10, 10));
gridPane.setVgap(2);
gridPane.setHgap(10);


def titleLabel = new Label("Alter the color and name of a class of objects")
gridPane.add(titleLabel,col, row++, 3, 1)

def requestLabel = new Label("Original Name")
gridPane.add(requestLabel,col++, row, 1, 1)
def requestLabel2 = new Label("New Name")
gridPane.add(requestLabel2,col++, row, 1, 1)
def requestLabel3 = new Label("New Color")
gridPane.add(requestLabel3,col++, row++, 1, 1)
//new row
col = 0

//generate a list of all in-use classes
Set classList = []
for (object in getAllObjects().findAll{it.isDetection() || it.isAnnotation()}) {
    classList << object.getPathClass()
}
//place all classes in a combobox
def ComboBox classText = new ComboBox();
classList.each{classText.getItems().add(it)}

gridPane.add(classText, col++, row, 1, 1)

def TextField classText2 = new TextField("MyNewClass");
classText2.setMaxWidth( textFieldWidth);
classText2.setAlignment(Pos.CENTER_RIGHT)
gridPane.add(classText2, col++, row, 1, 1)

def colorPicker = new ColorPicker()
gridPane.add(colorPicker, col, row++, 1, 1)


//ArrayList<Label> channelLabels 
Button startButton = new Button()
startButton.setText("Alter Class")
gridPane.add(startButton, 0, row++, 1, 1)
//startButton.setTooltip(new Tooltip("If you need to change the number of classes, re-run the script"));


startButton.setOnAction {

    changeList = getAllObjects().findAll{it.getPathClass() == getPathClass(classText.getValue().toString())}
    changeList.each{ 
    it.setPathClass(getPathClass(classText2.getText()))
        newClass = getPathClass(classText2.getText())
        newClass.setColor(ColorToolsFX.getRGBA(colorPicker.getValue()))
    }
    fireHierarchyUpdate()
}




//Some stuff that controls the dialog box showing up. I don't really understand it but it is needed.
Platform.runLater {

    def stage = new Stage()
    stage.initOwner(QuPathGUI.getInstance().getStage())
    stage.setScene(new Scene( gridPane))
    stage.setTitle("Class editor")
    stage.setWidth(450);
    stage.setHeight(200);
    //stage.setResizable(false);

    stage.show()

}
