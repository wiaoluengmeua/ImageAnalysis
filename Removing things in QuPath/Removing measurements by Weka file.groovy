//V1.0 attempting to read in an arff file as Strings.
//Note that you may need to change pathCellObject to pathTileObject at the end

import qupath.lib.classifiers.PathClassificationLabellingHelper

File arff = new File('C:\\filePath\\fileName.arff')
def lines = arff.readLines()
def start = false
def variableList = []

//create a list with elements that are your measurements from Weka
for (def i=0; i<lines.size()-1; i++){

    if (start == true){
        variableList.add(lines[i].trim())

    }
    if (lines[i].contains("Selected attributes:")){
        start = true
    }

}
def listTotal = []

//convert hashlinkedset into list so that the subtractions on the next line does not error out
listTotal.addAll(0,PathClassificationLabellingHelper.getAvailableFeatures(getDetectionObjects()))

//Potentially keep some necessary measurements that are not used?
variableList.addAll(['Nucleus: Area', 'Cytoplasm: Eosin OD mean'])

//subtract the list of variables from the arff file from the list of variables you want to remove
listTotal.removeAll(variableList)

//PathTileObject for SLICs PathCellObject for cells
removeMeasurements(qupath.lib.objects.PathCellObject, listTotal as String[])
fireHierarchyUpdate()

println("measurements removed")