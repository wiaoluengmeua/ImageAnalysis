//Script from komzy here: https://forum.image.sc/t/button-setonaction-does-not-print-anything-when-scripting/25443

guiscript=true

//import javafx.application.Platform
import javafx.scene.control.Button
import javafx.scene.control.Tooltip
import qupath.lib.gui.QuPathGUI

def qupath = QuPathGUI.getInstance()

def button = new Button('CT')
button.setPrefSize(40, QuPathGUI.iconSize)
button.setTooltip(new Tooltip("Cursor Tracker"));
button.setOnAction {
   print("Test Print # 2: Button Clicked")
}

qupath.addToolbarButton(button);
print("Test Print # 1")