//See below for 0.2.3 version

//0.1.2
//should work for Zstacks OR time series
//Creates a full image annotation in each frame, which can then be used to generate detections.
import qupath.lib.roi.RectangleROI
import qupath.lib.objects.PathAnnotationObject
hierarchy = getCurrentHierarchy()
def imageData = getCurrentImageData()
def server = imageData.getServer()
def xdist = server.getWidth()
def ydist = server.getHeight()
clearAllObjects()
if (server.nZSlices() >0){
    0.upto(server.nZSlices()-1){
        frame = new PathAnnotationObject(new RectangleROI(0,0,xdist,ydist,-1,it,0));
        addObject(frame);
    }
}
if (server.nTimepoints() >0){
    0.upto(server.nTimepoints()-1){
        frame = new PathAnnotationObject(new RectangleROI(0,0,xdist,ydist,-1,0,it));
        addObject(frame);
    }
}

//0.2.3 version
//should work for Zstacks OR time series
//Creates a full image annotation in each frame, which can then be used to generate detections.
import qupath.lib.roi.RectangleROI
import qupath.lib.objects.PathAnnotationObject
hierarchy = getCurrentHierarchy()
def imageData = getCurrentImageData()
def server = imageData.getServer()
def xdist = server.getWidth()
def ydist = server.getHeight()
clearAllObjects()
if (server.nZSlices() >0){
    0.upto(server.nZSlices()-1){
        frame = PathObjects.createAnnotationObject(ROIs.createRectangleROI(0,0,xdist,ydist,ImagePlane.getPlane(it,0)));
        addObject(frame);
    }
}
if (server.nTimepoints() >0){
    0.upto(server.nTimepoints()-1){
        frame = PathObjects.createAnnotationObject(ROIs.createRectangleROI(0,0,xdist,ydist,ImagePlane.getPlane(0,it)));
        addObject(frame);
    }
}

selectAnnotations()
//cell detection here