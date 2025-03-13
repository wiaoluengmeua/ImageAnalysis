//QUPATH VERSION 1.3- Does NOT work with 1.2
//Creating tiled areas and summing them for area based measurements applied to the original tissue annotation.  Assumes 1 annotation, but could be expanded to handle multiple.

//runPlugin('qupath.imagej.detect.tissue.SimpleTissueDetection2', '{"threshold": 204,  "requestedPixelSizeMicrons": 3.0,  "minAreaMicrons": 5000000.0,  "maxHoleAreaMicrons": 500.0,  "darkBackground": false,  "smoothImage": false,  "medianCleanup": false,  "dilateBoundaries": false,  "smoothCoordinates": false,  "excludeOnBoundary": false,  "singleAnnotation": true}');
//This line should almost always be run first, and then manually checked for accuracy of stain/artifacts.

server = getCurrentImageData().getServer()

//Choose your color deconvolutions here, I named them RED and BROWN for simplicity
//For this script, THE STAIN YOU ARE LOOKING FOR SHOULD COME FIRST - Stain 1 is Red for a PicroSirius Red detection
setColorDeconvolutionStains('{"Name" : "Red with background", "Stain 1" : "RED", "Values 1" : "0.25052 0.76455 0.59388 ", "Stain 2" : "BROWN", "Values 2" : "0.22949 0.5595 0.79642 ", "Background" : " 255 255 255 "}')

//Subdivide your annotation area into tiles
selectAnnotations();
runPlugin('qupath.lib.algorithms.TilerPlugin', '{"tileSizeMicrons": 1000.0,  "trimToROI": true,  "makeAnnotations": true,  "removeParentAnnotation": false}');

//Perform the positive pixel detection on the tiles, but not the larger annotation
def tiles = getAnnotationObjects().findAll {it.getDisplayedName().toString().contains('Tile') == true}
getCurrentHierarchy().getSelectionModel().setSelectedObjects(tiles, null)
runPlugin('qupath.imagej.detect.tissue.PositivePixelCounterIJ', '{"downsampleFactor": 1,  "gaussianSigmaMicrons": 0.3,  "thresholdStain1": 0.2,  "thresholdStain2": 0.1,  "addSummaryMeasurements": true}');

//Calculate the percentage of "negative" positive pixels, and apply that to the original tissue annotation
def total_Negative = 0
def total_Positive = 0
//Sum the areas of each tile
for (tile in tiles){
    total_Negative += tile.getMeasurementList().getMeasurementValue("Negative pixel area µm^2")
    total_Positive += tile.getMeasurementList().getMeasurementValue("Positive pixel area µm^2")
}
//summary[0] should be the original annotation, this assumes that there was only one original annotation

def summary = getAnnotationObjects().findAll {it.getDisplayedName().toString().contains('Tile') != true}

//Math
summary[0].getMeasurementList().putMeasurement("Negative Area Sum", total_Negative)
def total_area = summary[0].getROI().getArea()*server.getPixelHeightMicrons()*server.getPixelWidthMicrons()
summary[0].getMeasurementList().putMeasurement("Percentage PSR Positive", total_Negative/total_area*100)
summary[0].getMeasurementList().putMeasurement("Percentage Too Dark", total_Positive/total_area*100)

//Remove all of the tile annotations which would result in less readable output than a single tissue value
removeObjects(tiles,true)

/*
 * QuPath v0.1.2 has some bugs that make exporting annotations a bit annoying, specifically it doesn't include the 'dot' 
 * needed in the filename if you run it in batch, and it might put the 'slashes' the wrong way on Windows.
 * Manually fixing these afterwards is not very fun.
 * 
 * Anyhow, until this is fixed you could try the following script with Run -> Run for Project.
 * It should create a new subdirectory in the project, and write text files containing results there.
 *
 * @author Pete Bankhead
 */

def name = getProjectEntry().getImageName() + '.txt'
def path = buildFilePath(PROJECT_BASE_DIR, 'annotation results')
mkdirs(path)
path = buildFilePath(path, name)
saveAnnotationMeasurements(path)
print 'Results exported to ' + path
