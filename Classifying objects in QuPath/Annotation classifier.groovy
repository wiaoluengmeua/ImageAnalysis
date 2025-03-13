//Here I used optical density only.  You will need to add any intensity features you want to the classifier
//0.1.2 and 0.2.0

import qupath.lib.objects.classes.PathClassFactory

//Use add intensity features to add whatever values you need to determine a class
selectAnnotations();

//If you have already added features to your annotations, you will not need this line
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 2.0,  "region": "ROI",  "tileSizeMicrons": 25.0,  "colorOD": true,  "colorStain1": false,  "colorStain2": false,  "colorStain3": false,  "colorRed": false,  "colorGreen": false,  "colorBlue": false,  "colorHue": false,  "colorSaturation": false,  "colorBrightness": false,  "doMean": true,  "doStdDev": false,  "doMinMax": false,  "doMedian": false,  "doHaralick": false,  "haralickDistance": 1,  "haralickBins": 32}');


def ClassA = PathClassFactory.getPathClass("ClassA")
def ClassB = PathClassFactory.getPathClass("ClassB")

//feature has to be exact, including spaces.  This can be tricky
//for the above you would need: "ROI: 2.00 " + qupath.lib.common.GeneralTools.micrometerSymbol() + " per pixel: OD Sum:  Mean"
def feature = "ROI: 2.00 " + qupath.lib.common.GeneralTools.micrometerSymbol() + " per pixel: OD Sum:  Mean"
//get all annotations in the image
Annotations = getAnnotationObjects();
Annotations.each {
    double value = it.getMeasurementList().getMeasurementValue(feature)
    print(it.getMeasurementList())
    //use logic here to determine whether each "it" or annotation will be a given class
    //this can be as complicated as you want, or as simple as a single if statement.
    if (value > 0.45) {it.setPathClass(ClassA)}
    else { it.setPathClass(ClassB)}
}
println("Annotation classification done")