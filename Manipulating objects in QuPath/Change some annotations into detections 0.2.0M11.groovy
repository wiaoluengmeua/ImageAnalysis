//Script works as of 0.2.0M11 
//Purpose: to change objects from annotations to detections or vice versa
//Measurements WILL be lost.

//By default it changes annotations of class "Positive" into detections, and removes the original annotations.
//Adjust the == section to fit your desired target classes
//To change detections into annotations, swap getAnnotationObjects to getDetectionObjects in the first line
//and createDetectionObject to createAnnotationObject in the middle of the script

toChange = getAnnotationObjects().findAll {it.getPathClass() == getPathClass("Positive")}
newObjects = []
toChange.each{
    roi = it.getROI()
    annotation = PathObjects.createDetectionObject(roi, it.getPathClass())
    newObjects.add(annotation)
}

// Actually add the objects
addObjects(newObjects)
//Comment this line out if you want to keep the original objects
removeObjects(toChange,true)

resolveHierarchy()
print("Done!")