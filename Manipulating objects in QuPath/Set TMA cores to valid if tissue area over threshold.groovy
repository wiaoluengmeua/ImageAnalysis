//REPLACE TISSUE DETECTION LINE BELOW WITH YOUR OWN

//Set TMA cores where the tissue area is below a threshold to Missing so as to avoid inclusion in future calculations.
//Any annotations below theMINIMUM_AREA_um2 threshold will also be deleted.

MINIMUM_AREA_um2 = 30000
def imageData = getCurrentImageData()
def server = imageData.getServer()
def pixelSize = server.getPixelHeightMicrons()

getTMACoreList().each{
  it.setMissing(false)
}

selectTMACores();

///////////////////////REPLACE WITH YOUR OWN
runPlugin('qupath.imagej.detect.tissue.SimpleTissueDetection2', '{"threshold": 233,  "requestedPixelSizeMicrons": 5.0,  "minAreaMicrons": 10000.0,  "maxHoleAreaMicrons": 1000000.0,  "darkBackground": false,  "smoothImage": true,  "medianCleanup": true,  "dilateBoundaries": false,  "smoothCoordinates": true,  "excludeOnBoundary": false,  "singleAnnotation": true}');
////////////////////////////////////////

//Potentially alter script to run off of mean intensity in core?
//runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 2.0,  "region": "ROI",  "tileSizeMicrons": 25.0,  "colorOD": true,  "colorStain1": false,  "colorStain2": false,  "colorStain3": false,  "colorRed": false,  "colorGreen": false,  "colorBlue": false,  "colorHue": false,  "colorSaturation": false,  "colorBrightness": false,  "doMean": false,  "doStdDev": false,  "doMinMax": false,  "doMedian": false,  "doHaralick": false,  "haralickDistance": 1,  "haralickBins": 32}');
getTMACoreList().each{
  it.setMissing(true)
  list = it.getChildObjects()
  println(list)
  if ( list.size() > 0){
      double total = 0
      for (object in list) {
          total = total+object.getROI().getArea()
         
      }
      total = total*pixelSize*pixelSize
      println(it.getName()+ " list "+list.size()+ "total "+total)
      if ( total > MINIMUM_AREA_um2 ){
          it.setMissing(false)
      } else {removeObjects(list, true)}
  }
}
fireHierarchyUpdate()
println("done")