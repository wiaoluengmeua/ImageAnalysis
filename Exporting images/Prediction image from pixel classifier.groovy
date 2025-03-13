//0.2.0
//https://forum.image.sc/t/qupath-script-with-pixel-classifier/45597/10?u=research_associate

def imageData = getCurrentImageData()
def classifier = loadPixelClassifier('Classifier')

def predictionServer = PixelClassifierTools.createPixelClassificationServer(imageData, classifier)

def path = buildFilePath(PROJECT_BASE_DIR, 'prediction.tif')

def downsample = predictionServer.getDownsampleForResolution(0)
def request = RegionRequest.createInstance(predictionServer, downsample)
writeImageRegion(predictionServer, request, path)