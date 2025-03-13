//0.2.0 version of https://forum.image.sc/t/assign-point-objects-to-different-rois-by-overlap/26905/2?u=research_associate
// Convert points into detections, resolve the heirarchy so they are now child objects of any annotations.

import qupath.lib.roi.EllipseROI;
import qupath.lib.objects.PathDetectionObject

points = getAnnotationObjects().findAll{it.getROI().isPoint() }
print points[0].getROI()

describe(points[0].getROI())
//Cycle through each points object (which is a collection of points)
points.each{ 
    //Cycle through all points within a points object
    pathClass = it.getPathClass()
    it.getROI().getAllPoints().each{ 
        //for each point, create a circle on top of it that is "size" pixels in diameter
        x = it.getX()
        y = it.getY()
        size = 5
        def roi = ROIs.createEllipseROI(x-size/2,y-size/2,size,size, ImagePlane.getDefaultPlane())
        
        def aCell = new PathDetectionObject(roi, pathClass)
        addObject(aCell)
    }
}
//remove points if desired.
removeObjects(points, false)
resolveHierarchy()