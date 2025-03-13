//This script forces the annotation to detect whether cells are inside of it
//Useful when pasting an annotation onto a set of detections
//Added warning, this may appear to freeze the program if a lot of detections are being updated.  Be patient.


selected = getSelectedObject()
removeObject(selected, true)
addObject(selected)

//Locked is not absolutely necessary, but good practice so you don't "jiggle" your updated animation
selected.setLocked(true)