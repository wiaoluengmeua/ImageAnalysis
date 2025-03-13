// Script further modified from Pete Bankhead's post https://forum.image.sc/t/qupath-distance-between-annotations/47960/2
// Calculate the distances from each object to the nearest object of each other class. Saved to the object's measurement list.
// Distances recorded should stay 0 if there are no other objects, but it should find a non-zero distance if there are multiple of the same class

// Does NOT work *between* multiple points in a Points object, though Points objects will count as a single object.
/////////////////////////////////////////////////////
// DOES NOT WORK/CHECK FOR ZSTACKS AND TIME SERIES //
/////////////////////////////////////////////////////
print "Caution, this may take some time for large numbers of objects."
print "If the time is excessive for your project, you may want to consider size thresholding some objects, or adjusting the objectsToCheck"
objectsToCheck = getAllObjects().findAll{it.isDetection() || it.isAnnotation()}
PrecisionModel PM = new PrecisionModel(PrecisionModel.FIXED)
classList = objectsToCheck.collect{it.getPathClass()} as Set
def cal = getCurrentServer().getPixelCalibration()
if (cal.pixelWidth != cal.pixelHeight) {
    println "Pixel width != pixel height ($cal.pixelWidth vs. $cal.pixelHeight)"
    println "Distance measurements will be calibrated using the average of these"
}
Map combinedClassObjects = [:]
classList.each{c->
    currentClassObjects = getAllObjects().findAll{it.getPathClass() == c}
    geom = null
    currentClassObjects.eachWithIndex{o, i->
        if(i==0){geom = o.getROI().getGeometry()}else{
            geom = GeometryPrecisionReducer.reduce(geom.union(o.getROI().getGeometry()), PM)
            //geom =(geom.union(o.getROI().getGeometry())).buffer(0)
        }
    }
    combinedClassObjects[c] = geom
}

objectsToCheck.each{ o ->
    //Store the shortest non-zero distance between an annotation and another class of annotation

    def g1 = o.getROI().getGeometry().buffer(0)
    //If there are multiple annotations of the same type, prevent checking distances against itself
    combinedClassObjects.each{cco->
        combinedGeometry = cco.value
        if (o.getPathClass() == cco.key){
            combinedGeometry= combinedGeometry.difference(GeometryPrecisionReducer.reduce(g1, PM))
            //combinedGeometry= (combinedGeometry.difference(g1)).buffer(0)
            //print "internal"
        }
        double distancePixels = g1.distance(combinedGeometry)
        double distanceCalibrated = distancePixels * cal.getAveragedPixelSize()
        o.getMeasurementList().putMeasurement("Distance in um to nearest "+cco.key, distanceCalibrated)
    }
}

import org.locationtech.jts.precision.GeometryPrecisionReducer
import org.locationtech.jts.geom.PrecisionModel


print "Done! Distances saved to each object's measurement list"