//Remove all clusters 0.2.0m5+
// def subcellular = getDetectionObjects().findAll {it.getParent()?.isDetection()}
//def subcellular2 = getCellObjects().collect({it.getChildObjects()}).flatten()
removeObjects(getDetectionObjects().findAll{it.getPathClass().toString().contains("Subcellular")},true)