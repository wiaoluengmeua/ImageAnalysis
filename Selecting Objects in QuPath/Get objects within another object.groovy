//0.2.0 only option for ignoring the hierarchy and selecting objects within another object.
//https://forum.image.sc/t/qupath-getting-detectionsobjects-within-non-parent-annotation/41784/2
//Stores the objects you want within a variable called "objects"


//Original:
def hierarchy = getCurrentHierarchy()
def parent = getSelectedObject()
def objects = hierarchy.getObjectsForROI(null, parent.getROI())
    .findAll { it.isDetection() }

//one line version
def objects = getCurrentHierarchy().getObjectsForROI(qupath.lib.objects.PathDetectionObject, getSelectedObject().getROI())

//If you have single Annotations with different classifications, you could use something like the following
//Example, you have a Tissue annotation, and within that a specific Tumor annotation. You want the "cells" within the Tumor annotation
//[0] at the end of the next line gets the first (and only, in this example) annotation that is classified as Tumor.
tumorAnno = getAnnotationObjects().findAll{it.getPathClass() == getPathClass("Tumor")}[0]

objects = getCurrentHierarchy().getObjectsForROI(qupath.lib.objects.PathCellObject, tumorAnno.getROI())
