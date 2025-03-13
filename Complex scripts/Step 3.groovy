//BEFORE running this script, draw your cytoplasmic areas with the annotation drawing tools in QuPath.  Once you are set, you should
//be able to run this script in order to merge the cytoplasms with the nuclei to create cells.  This will not work if the
//cytoplasms cross outside of the area defined by the largest annotation object.

//At the end it generates some cell shape measurements.

import qupath.lib.objects.PathCellObject

// Get the current hierarchy
def hierarchy = getCurrentHierarchy()

// Get the select objects

def targets = getObjects{return it.getLevel()!=1 && it.isAnnotation()}


// Check we have anything to work with
if ( targets.isEmpty()) {
    print("No objects selected!")
    return
}

// Loop through objects
def newDetections = new ArrayList<>()
for (def cellAnnotation in  targets) {

    // Unlikely to happen... but skip any objects not having a ROI
    if (!cellAnnotation.hasROI()) {
        print("Skipping object without ROI: " + cellAnnotation)
        continue
    }
    def nucleus = hierarchy.getDescendantObjects(cellAnnotation, null, null)
    def roiNuc = nucleus[0].getROI()
    def roiCyto =  cellAnnotation.getROI()
    def nucMeasure = nucleus[0].getMeasurementList()
    def cell = new PathCellObject(roiCyto,roiNuc,cellAnnotation.getPathClass(),nucMeasure)
    newDetections.add(cell)
    print("Adding " + cell)

    
    //remove stand alone nucleus
    removeObject(nucleus[0], true)
}
removeObjects( targets, true)
// Actually add the objects
hierarchy.addPathObjects(newDetections, false)

fireHierarchyUpdate()
if (newDetections.size() > 1)
    print("Added " + newDetections.size() + " detections(s)")
    
selectDetections()
runPlugin('qupath.lib.plugins.objects.ShapeFeaturesPlugin', '{"area": true,  "perimeter": true,  "circularity": true,  "useMicrons": true}');

//Recreate your whole image annotation.
createSelectAllObject(true);

