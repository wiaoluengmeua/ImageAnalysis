//0.1.2
//should work for Zstacks OR time series
//Copies ALL existing annotations to ALL other T or Z slices. Use getAnnotationObjects().findAll{it-> if(something)} to limit this
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.roi.PathROIToolsAwt


hierarchy = getCurrentHierarchy()
def imageData = getCurrentImageData()
def server = imageData.getServer()
def xdist = server.getWidth()
def ydist = server.getHeight()
def annotations = getAnnotationObjects()
if (server.nZSlices() >0){
    0.upto(server.nZSlices()-1){
        for (annotation in annotations){
            
            def roi = annotation.getROI()
            def shape = PathROIToolsAwt.getShape(roi)
            // There is a method to create a ROI from a shape which allows us to (finally) set the Z (or T)
            def roi2 = PathROIToolsAwt.getShapeROI(shape, -1, it, roi.getT(), 0.5)

            // We can now make a new annotation
            def annotation2 = new PathAnnotationObject(roi2)
            
            // Add it to the current hierarchy. When we move in Z to the desired slice, we should see the annotation
            if (roi2.getZ() != roi.getZ())
                hierarchy.addPathObject(annotation2, false);
        }
    }
}
if (server.nTimepoints() >1){
    0.upto(server.nTimepoints()-1){
        for (annotation in annotations){
            
            def roi = annotation.getROI()
            def shape = PathROIToolsAwt.getShape(roi)
            // There is a method to create a ROI from a shape which allows us to (finally) set the Z (or T)
            def roi2 = PathROIToolsAwt.getShapeROI(shape, -1, roi.getZ(), it)

            // We can now make a new annotation
            def annotation2 = new PathAnnotationObject(roi2)
            
            // Add it to the current hierarchy. When we move in T to the desired slice, we should see the annotation
            if (roi2.getT() != roi.getT())
                hierarchy.addPathObject(annotation2, false);
        }


    }
}