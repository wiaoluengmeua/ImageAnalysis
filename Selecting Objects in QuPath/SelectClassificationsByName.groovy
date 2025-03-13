/**
 * Source https://gist.github.com/petebankhead/abb3f5a3a2b55ddc22f709c99964857e
 * Helper script for QuPath to find objects with one or more classifications.
 *
 Mostly replaced by
 selectObjectsByClassification("Islet");
 in 0.2.0
 * @author Pete Bankhead
 */

// Insert as many or few classifications here as required
selectObjects {checkForClassifications(it.getPathClass(), 'CD8', 'FoxP3')}
print 'Selected ' + getSelectedObjects().size()


boolean checkForSingleClassification(def pathClass, classificationName) {
    if (pathClass == null)
        return false
    if (pathClass.getName() == classificationName)
        return true
    return checkForSingleClassification(pathClass.getParentClass(), classificationName)
}

boolean checkForClassifications(def pathClass, String...classificationNames) {
    if (classificationNames.length == 0)
        return false
    for (String name : classificationNames) {
        if (!checkForSingleClassification(pathClass, name))
            return false
    }
    return true
}