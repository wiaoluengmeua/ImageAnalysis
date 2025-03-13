//0.2.0
//Update to M5 measurements calculator for Multiplex analysis
// Initial script by Mike Nelson @Mike_Nelson on image.sc
// Project checking from Sara McArdle @smcardle on image.sc
// Additional information at link below.
// https://forum.image.sc/t/m9-multiplex-classifier-script-updates-cell-summary-measurements-visualization/34663

import qupath.lib.plugins.parameters.ParameterList
import qupath.lib.objects.PathCellObject
import qupath.lib.objects.PathObjectTools

imageData = getCurrentImageData()
server = imageData.getServer()
def cal = getCurrentServer().getPixelCalibration()
hierarchy = getCurrentHierarchy()

separatorsForBaseClass = "[.-_,:]+" //add an extra symbol between the brackets if you need to split on a different character

boolean fullClassesB = true
boolean baseClassesB = true
boolean percentages = true
boolean densities = false
boolean removeOld = false
boolean checkProject = true

/*************************************************************************/
//////////// REMOVE BELOW THIS SECTION IF RUNNING FOR PROJECT////////////////
//EDIT THE CORRECT BOOLEAN VALUES ABOVE MANUALLY

def params = new ParameterList()
        //Yes, I am bad at dialog boxes.
        .addBooleanParameter("fullClassesB", "Measurements for full classes                                                                                                                      ", fullClassesB, "E.g. CD68:PDL1 cells would be calculated on their own")
        .addBooleanParameter("baseClassesB", "Measurements for individual base classes                                                                                                           ", baseClassesB, "Measurements show up as 'All' plus the base class name")
        .addBooleanParameter("percentages", "Percentages?                                                                                                                                        ", percentages, "")
        .addBooleanParameter("densities", "Cell densities?                                                                                                                                       ", densities, "Not recommended without pixel size metadata or some sort of annotation outline")
        .addBooleanParameter("removeOld", "Clear old measurements, can be slow                                                                                                                   ", removeOld, "Slow, but needed when the classifier changes. Otherwise classes that no longer exist will not be overwritten with 0 resulting in extra incorrect measurements")
        .addBooleanParameter("checkProject", "Use class names from entire project, can be slow!                                                                                                  ", checkProject, "Needed for projects where all images are analyzed and compared")
if (!Dialogs.showParameterDialog("Cell summary measurements: M9 VALUES DO NOT AUTOMATICALLY UPDATE", params))
    return



fullClassesB = params.getBooleanParameterValue("fullClassesB")
baseClassesB = params.getBooleanParameterValue("baseClassesB")
percentages = params.getBooleanParameterValue("percentages")
densities = params.getBooleanParameterValue("densities")
removeOld = params.getBooleanParameterValue("removeOld")
checkProject = params.getBooleanParameterValue("checkProject")


//////////// REMOVE ABOVE THIS SECTION IF RUNNING FOR PROJECT ///////////////////
/*************************************************************************/



if (removeOld){
    Set annotationMeasurements = []
    getAnnotationObjects().each{it.getMeasurementList().getMeasurementNames().each{annotationMeasurements << it}}

    List remove =[]
    annotationMeasurements.each{ if(it.contains("%") || it.contains("^")) {removeMeasurements(qupath.lib.objects.PathAnnotationObject, it);}}
}
Set baseClasses = []
Set classNames = []
if (checkProject){

    getProject().getImageList().each{
        def objs = it.readHierarchy().getDetectionObjects()
        classes = objs.collect{it?.getPathClass()?.toString()}
        classNames.addAll(classes)
    }

    classNames.each{
        it?.tokenize(separatorsForBaseClass).each{str->
           baseClasses << str.trim()
        }
    }
    println("Classifications: "+classNames)
    println("Base Classes: "+baseClasses)
}else{

    classNames.addAll(getDetectionObjects().collect{it?.getPathClass()?.toString()} as Set)


    classNames.each{
        it?.tokenize(separatorsForBaseClass).each{str->
           baseClasses << str.trim()
        }
    }
    println("Classifications: "+classNames)
    println("Base Classes: "+baseClasses)
}
//This section calculates measurements for the full classes (all combinations of base classes)

if (fullClassesB){

for (annotation in getAnnotationObjects()){
    totalCells = []
    qupath.lib.objects.PathObjectTools.getDescendantObjects(annotation,totalCells, PathCellObject)


    for (aClass in classNames){
        if (aClass){
            if (totalCells.size() > 0){
                cells = totalCells.findAll{it.getPathClass().toString() == aClass}
                print cells.size()
                if (percentages) {annotation.getMeasurementList().putMeasurement(aClass.toString()+" %", cells.size()*100/totalCells.size())}
                annotationArea = annotation.getROI().getScaledArea(cal.pixelWidth, cal.pixelHeight)
                if (densities) {annotation.getMeasurementList().putMeasurement(aClass.toString()+" cells/mm^2", cells.size()/(annotationArea/1000000))}
            } else {
                if (percentages) {annotation.getMeasurementList().putMeasurement(aClass.toString()+" %", 0)}
                if (densities) {annotation.getMeasurementList().putMeasurement(aClass.toString()+" cells/mm^2", 0)}
            }
        }
    }

}
}

//This section only calculates densities of the base class types, regardless of other class combinations.
//So all PDL1 positive cells would counted for a PDL1 sub class, even if the cells had a variety of other sub classes.

if (baseClassesB){

    for (annotation in getAnnotationObjects()){
        totalCells = []
        qupath.lib.objects.PathObjectTools.getDescendantObjects(annotation,totalCells, PathCellObject)


        for (aClass in baseClasses){

            if (totalCells.size() > 0){
                cells = totalCells.findAll{it.getPathClass().toString().contains(aClass)}
                
                if (percentages) {annotation.getMeasurementList().putMeasurement("All "+aClass+" %", cells.size()*100/totalCells.size())}
                annotationArea = annotation.getROI().getScaledArea(cal.pixelWidth, cal.pixelHeight)
                if (densities) {annotation.getMeasurementList().putMeasurement("All "+aClass+" cells/mm^2", cells.size()/(annotationArea/1000000))}
            } else {
                if (percentages) {annotation.getMeasurementList().putMeasurement("All "+aClass+" %", 0)}
                if (densities) {annotation.getMeasurementList().putMeasurement("All "+aClass+" cells/mm^2", 0)}
            }

        }

    }
}

