guiscript=true
//0.1.2 - Largely obsolete with the Keep settings checkbox in Brightness/contrast.

//Script sets up some buttons to allow easy viewing of a fixed measurement map. Buttons are only removed when closing QuPath
//Keep these labels fairly short, or increase the button size. Be careful about having enough room!
int buttonSize = 40
String[] buttonLabels = ["1C","2C", "3N", "4C", "5C", "6C"] as String[]

String[] names = ["Cytoplasm: Channel 1 mean", "Cytoplasm: Channel 2 mean", "Nucleus: Channel 3 mean", "Cytoplasm: Channel 4 mean", "Cytoplasm: Channel 5 mean", "Cytoplasm: Channel 6 mean"] as String[]
double[] minValue= [0,0,0,0,0,0]
double[] maxValue= [18, 1,6,1,20,10]

//import javafx.application.Platform
import javafx.scene.control.Button
import javafx.scene.control.Tooltip
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.helpers.MeasurementMapper
def qupath = QuPathGUI.getInstance()

int size = names.size()
buttons = new Button [size]

//check some things
if (minValue.size()!= size || maxValue.size()!= size || buttonLabels.size() != size ) {println("All lists not same size"); return;}
if (getDetectionObjects().size() < 1){println("Detections NEED to be present before running this script"); return;}

maps = new MeasurementMapper [size]
def unColor = new Button('Clear')
unColor.setPrefSize(50, QuPathGUI.iconSize)
unColor.setTooltip(new Tooltip("Remove coloring"));
unColor.setOnAction {

        print 'Resetting measurement map'
        getCurrentViewer().getOverlayOptions().setMeasurementMapper(null)

}
qupath.addToolbarButton(unColor);
for (i = 0; i<size; i++){
    maps[i] = new MeasurementMapper(names[i], getDetectionObjects())
}

//println(maps)

for (i = 0; i<size; i++){

    buttons[i] = new Button(buttonLabels[i])
    buttons[i].setPrefSize(buttonSize, QuPathGUI.iconSize)
    buttons[i].setTooltip(new Tooltip("Measurement Maps "+names[i]));

    buttons[i].setOnAction {e->
        
        source = e.getSource().getText()
        //println(source)
        //println(buttons[0].getText())
        //println(buttons[1].getText())
        //println(size)
        int j= 255
        for (k = 0; k<size; k++){
            //println("k " + k)
            if (source == buttons[k].getText()){j=k; print "please";}else{print "NotA"}
        }
        // Update the display
        //println(j)
        //println(buttons[j].getText())
        if (names[j]) {
            print String.format('Setting measurement map: %s (%.2f - %.2f)', names[j], minValue[j], maxValue[j])
            maps[j].setDisplayMinValue(minValue[j])
            maps[j].setDisplayMaxValue(maxValue[j])
            getCurrentViewer().getOverlayOptions().setMeasurementMapper(maps[j])
        } else {
            print 'Resetting measurement map'
            getCurrentViewer().getOverlayOptions().setMeasurementMapper(null)
        }

    }
}
for (n = 0; n<size; n++){
    qupath.addToolbarButton(buttons[n]);
}
