//https://forum.image.sc/t/imagej-rois-and-rotated-image-server-issue-on-qupath/47019/3

import java.awt.geom.AffineTransform

def server = getCurrentServer()

def transform = AffineTransform.getRotateInstance(Math.PI)
transform.translate(-server.getWidth(), -server.getHeight())

def annotations = getAnnotationObjects()
def transformedAnnotations = annotations.collect {PathObjectTools.transformObject(it, transform, true)}

removeObjects(annotations, true)
addObjects(transformedAnnotations)