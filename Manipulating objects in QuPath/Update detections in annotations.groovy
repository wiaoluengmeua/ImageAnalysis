//This script forces the annotations to detect whether cells are inside of it
//Useful when pasting an annotation onto a set of detections
//Added warning, this may appear to freeze the program if a lot of detections are being updated.  Be patient.
//0.1.2 0.2.0
import qupath.lib.roi.*
import qupath.lib.objects.*

selected = getSelectedObjects()
def rois = [:]

for (object in selected){
        rois.put(object.getROI(),object.getPathClass())
}
removeObjects(selected, true)



rois.each{r,c-> addObject(new PathAnnotationObject(r, c))}


//Locked is not absolutely necessary, but good practice so you don't "jiggle" your updated animation
for (annotation in getAnnotationObjects())
    annotation.setLocked(true)
    
    