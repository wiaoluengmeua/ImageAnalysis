/*
 * Identify annotations touching the image boundary
 https://forum.image.sc/t/filter-out-annotations-touching-image-border/37570/2?u=research_associate
 */

def server = getCurrentServer()

// To get the annotations
def annotations = getAnnotationObjects().findAll { touchingBoundary(server, it.getROI()) }
removeObjects(annotations, false)

// To select the annotations
//selectObjects {it.isAnnotation() && it.hasROI() && touchingBoundary(server, it.getROI())}

// Using full lambda to get the annotations (just a different syntax)
//def annotations2 = getAnnotationObjects().findAll(annotation -> touchingBoundary(server, annotation.getROI()))


boolean touchingBoundary(server, roi) {
    return roi.getBoundsX() <= 0 ||
           roi.getBoundsY() <= 0 ||
           roi.getBoundsX() + roi.getBoundsWidth() >= server.getWidth()-1 ||
           roi.getBoundsY() + roi.getBoundsHeight() >= server.getHeight()-1
}
