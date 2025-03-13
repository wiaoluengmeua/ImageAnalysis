/** 0.1.2
 * Create a QuPath classifier by scripting, rather than the 'standard' way with annotations.
 *
 * This selects training regions according to a specified criterion based on staining,
 * and then creates a classifier that uses other features.
 *
 * The main aim is to show the general idea of creating a classifier by scripting.
 *
 * @author Pete Bankhead
 */

import qupath.lib.classifiers.Normalization
import qupath.lib.classifiers.PathClassificationLabellingHelper
import qupath.lib.objects.PathObject
import qupath.lib.objects.classes.PathClassFactory
import qupath.lib.scripting.QPEx

// Optionally check what will be used for training -
// setting the training classification for each cell & not actually building the classifier
// (i.e. just do a sanity check)
boolean checkTraining = false

// Get all cells
def cells = QPEx.getCellObjects()

// Split by some kind of DAB measurement
def isTumor = {PathObject cell -> return cell.getMeasurementList().getMeasurementValue('Cell: DAB OD mean') > 0.2}
def tumorCells = cells.findAll {isTumor(it)}
def nonTumorCells = cells.findAll {!isTumor(it)}
print 'Number of tumor cells: ' + tumorCells.size()
print 'Number of non-tumor cells: ' + nonTumorCells.size()

// Create a relevant map for training
def map = [:]
map.put(PathClassFactory.getPathClass('Tumor'), tumorCells)
map.put(PathClassFactory.getPathClass('Stroma'), nonTumorCells)

// Check training... if necessary
if (checkTraining) {
    print 'Showing training classifications (not building a classifier!)'
    map.each {classification, list ->
        list.each {it.setPathClass(classification)}
    }
    QPEx.fireHierarchyUpdate()
    return
}

// Get features & filter out the ones that shouldn't be used (here, any connected to intensities)
def features = PathClassificationLabellingHelper.getAvailableFeatures(cells)
features = features.findAll {!it.toLowerCase().contains(': dab') && !it.toLowerCase().contains(': hematoxylin')}

// Print the features
print Integer.toString(features.size()) + ' features: \n\t' + String.join('\n\t', features)

// Create a new random trees classifier with default settings & no normalization
print 'Training classifier...'
// This would show available parameters
// print classifier.getParameterList().getParameters().keySet()
def classifier = new qupath.opencv.classify.RTreesClassifier()
classifier.updateClassifier(map, features as List, Normalization.NONE)

// Actually run the trained classifier
print 'Applying classifier...'
classifier.classifyPathObjects(cells)
QPEx.fireHierarchyUpdate()
print 'Done!'