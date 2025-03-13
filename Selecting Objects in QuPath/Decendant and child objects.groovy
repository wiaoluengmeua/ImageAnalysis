//Finding decendant and child objects of annotations
//0.1.2

// Define classes
class1 = getPathClass('class1')
class2 = getPathClass('class2')


// Loop through and add child/descendant counts
hierarchy = getCurrentHierarchy()
getAnnotationObjects().each {

    if (it.getPathClass() != class1)
        return false

    def children = it.getChildObjects()
    def nClass2Children = children.count {it.getPathClass() == class2}
    def descendants = hierarchy.getDescendantObjects(it, null, null)
    def nClass2Descendants = descendants.count {it.getPathClass() == class2}
    it.getMeasurementList().putMeasurement('Number of ' + class2 + ' child objects', nClass2Children)
    it.getMeasurementList().putMeasurement('Number of ' + class2 + ' descendant objects', nClass2Descendants)
    it.getMeasurementList().closeList()
}