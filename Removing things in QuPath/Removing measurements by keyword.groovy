//Replace the text in it.contains("keyword") in order to remove all measurements that contain that text.
//This can be useful after Smoothing in order to remove some of the large numbers of measurements you probably do not need

import qupath.lib.classifiers.PathClassificationLabellingHelper

def toRemove = PathClassificationLabellingHelper.getAvailableFeatures(getDetectionObjects()).findAll { it.contains("keyword") }
print toRemove
//PathTileObject for SLICs, PathCellObject for cells, TMACoreObject for TMAs
removeMeasurements(qupath.lib.objects.PathTileObject, toRemove as String[])
fireHierarchyUpdate()