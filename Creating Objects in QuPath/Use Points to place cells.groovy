//0.1.2 only, create detections from points.
//See 0.2.3 below

import qupath.lib.roi.EllipseROI;
import qupath.lib.objects.PathDetectionObject

points = getAnnotationObjects().findAll{it.isPoint() }
//Cycle through each points object (which is a collection of points)
points.each{ 
    //Cycle through all points within a points object
    it.getROI().getPointList().each{ 
        //for each point, create a circle on top of it that is "size" pixels in diameter
        x = it.getX()
        y = it.getY()
        size = 5
        def roi = new EllipseROI(x-size/2,y-size/2,size,size, 0,0,0)
        pathClass = getPathClass("FakeCell")
        def aCell = new PathDetectionObject(roi, pathClass)
        addObject(aCell)
    }
}
//remove points if desired.
removeObjects(points, false)


//0.2.3 version
import qupath.lib.objects.PathDetectionObject

points = getAnnotationObjects().findAll{it.getROI().isPoint() }
//Cycle through each points object (which is a collection of points)
points.each{
    plane = it.getROI().getImagePlane()
    pathClass = it.getPathClass() 
    //Cycle through all points within a points object
    it.getROI().getAllPoints().each{ 
        //for each point, create a circle on top of it that is "size" pixels in diameter
        x = it.getX()
        y = it.getY()
        size = 5
        def roi = ROIs.createEllipseROI(x-size/2,y-size/2,size,size, plane)
        //pathClass = getPathClass("FakeCell")
        def aCell = new PathDetectionObject(roi, pathClass)
        addObject(aCell)
    }
}
resolveHierarchy()
//remove points if desired.
removeObjects(points, false)