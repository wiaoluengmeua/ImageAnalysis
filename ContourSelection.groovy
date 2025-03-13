//Courtesy of Sara McArdle from the La Jolla Institute
//Create multiple bands within tissue, with the distance from the tissue surface (um) determined by the variables below
//Adjust class names within the script
//0.2.0

double firstRadius = -750
double secondRadius = -850

import qupath.lib.roi.*
import qupath.lib.objects.*
  
def imageData = getCurrentImageData()
def hierarchy = imageData.getHierarchy()
//EDIT THIS LINE TO YOUR TISSUE DETECTION SETTINGS
//runPlugin('qupath.imagej.detect.tissue.SimpleTissueDetection2', '{"threshold": 155,  "requestedPixelSizeMicrons": 20.0,  "minAreaMicrons": 10000.0,  "maxHoleAreaMicrons": 1000000.0,  "darkBackground": false,  "smoothImage": true,  "medianCleanup": true,  "dilateBoundaries": false,  "smoothCoordinates": true,  "excludeOnBoundary": false,  "singleAnnotation": true}');
selectAnnotations()
def outer=getAnnotationObjects().find{p -> (p.getLevel()==1) && (p.isAnnotation() == true)}
runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": '+firstRadius+',  "removeInterior": false,  "constrainToParent": true}')
def middle=getAnnotationObjects().find{p -> (p.getLevel()==2) && (p.isAnnotation() == true)}

//Reselecting the outer object turns out to be very important before running the DilateAnnotationPlugin a second (or third) time.
getCurrentHierarchy().getSelectionModel().setSelectedObject(outer)
runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": '+secondRadius+',  "removeInterior": false,  "constrainToParent": true}')
//write some stuff if you have more than 1 outer object for odd shapes
def inner=getAnnotationObjects().find{p -> (p.getLevel()==3) && (p.isAnnotation() == true)}
inner.setName("Medulla")

def toAdd = []
def toRemove = []
outerRing = PathROIToolsAwt.combineROIs(outer.getROI(), middle.getROI(), PathROIToolsAwt.CombineOp.SUBTRACT)
toRemove << middle
def orObject=new PathAnnotationObject(outerRing, outer.getPathClass())
orObject.setName("Cortex")
toAdd << orObject
innerRing = PathROIToolsAwt.combineROIs(middle.getROI(), inner.getROI(), PathROIToolsAwt.CombineOp.SUBTRACT)
def irObject = new PathAnnotationObject(innerRing, outer.getPathClass())
irObject.setName("Middle Zone")
toAdd << irObject
toRemove << outer

hierarchy.addPathObjects(toAdd,false)
hierarchy.removeObjects(toRemove,true)

print "Done"