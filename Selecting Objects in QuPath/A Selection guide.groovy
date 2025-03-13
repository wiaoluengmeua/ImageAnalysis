//Warning, some functions like getArea() will not work on some types of objects, like points, and will cause the script to crash
//if you are searching across both area based annotations and points. getArea() also returns measurements IN PIXELS.

//Select all of a type
selectAnnotations();
selectDetections();
//get as complicated as you want in the selection here, goes through all objects
selectObjects { p -> p.getPathClass() == getPathClass("Stroma") && p.isAnnotation() }
//or from https://groups.google.com/forum/#!topic/qupath-users/PxrCwx5ttXI
selectObjects {
   //Some criteria here
   return it.isAnnotation() && it.getPathClass() == getPathClass('Tumor')
}


//These do not select, but each can be used to generate a list of objects, which can then be used...
getAnnotationObjects()
getDetectionObjects()
getSelectedObject()
getSelectedObjects()

//... in this type of structure
def Annotations = getAnnotationObjects()
getCurrentHierarchy().getSelectionModel().setSelectedObjects(Annotations, null)
//The total of these two lines is the same as selectAnnotations(), but is capable of being modified by:
def smallAnnotations = getAnnotationObjects().findAll {it.getROI().getArea() < 400000}
//similar to the selectObjects code above
//The 400000 in this case is in pixels, and would need to be modified by a metadata call (which varies depending on your version of QuPath)

//Selecting multiple single object in a row
resetSelection()
getCellObjects().each{
    getCurrentHierarchy().getSelectionModel().setSelectedObject(it, true);
    threshold = measurement(it, "Nucleus: Channel 2 mean")+measurement(it, "Nucleus: Channel 2 std dev")
    runPlugin('qupath.imagej.detect.cells.SubcellularDetection', '{"detection[Channel 1]": -1.0,  "detection[Channel 2]": '+threshold+',  "detection[Channel 3]": -1.0,  "doSmoothing": true,  "splitByIntensity": false,  "splitByShape": true,  "spotSizeMicrons": 1.0,  "minSpotSizeMicrons": 1,  "maxSpotSizeMicrons": 2.0,  "includeClusters": true}');
    resetSelection();
    fireHierarchyUpdate()
}

//In 0.2.0 many things can be replaced by simple thresholding and then using scripts like:
selectObjectsByClassification("Islet");
selectObjectsByClassification("Islet", "Stroma");
//Which directly selects objects in one line.

//Selecting objects within other objects, that may not match up with the hierarchy
//https://forum.image.sc/t/qupath-getting-detectionsobjects-within-non-parent-annotation/41784/2
def hierarchy = getCurrentHierarchy()
def parent = getSelectedObject()
def objects = hierarchy.getObjectsForROI(null, parent.getROI())
    .findAll { it.isDetection() }
hierarchy.getObjectsForROI(qupath.lib.objects.PathDetectionObject, parent.getROI())
