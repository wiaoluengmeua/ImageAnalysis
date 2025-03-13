//https://forum.image.sc/t/selecting-slic-tiles/34942/12
//0.2.0

def imageData = getCurrentImageData()
def classifier = loadObjectClassifier('Other tiles')

def tiles = getDetectionObjects().findAll {it.isTile()}
classifier.classifyObjects(imageData, tiles, true)
fireHierarchyUpdate()