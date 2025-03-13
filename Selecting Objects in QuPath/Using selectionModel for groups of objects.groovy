//This script is a modification of the script to find annotations of a certain size, which then uses
//getSelectionModel to take the list of annotations and have QuPath register them as selected
//getArea() returns a measurement in PIXELS not square microns.

//0.1.2 0.2.0
def smallAnnotations = getAnnotationObjects().findAll {it.getROI().getArea() < 400000}

//This line does the selecting, and you should be able to swap in any list of objects for smallAnnotations
getCurrentHierarchy().getSelectionModel().setSelectedObjects(smallAnnotations, null)