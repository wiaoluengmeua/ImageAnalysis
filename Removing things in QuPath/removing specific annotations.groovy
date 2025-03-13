//removing annotations of class 1 that do not have children of class 2
//use of .any

// Define classes
class1 = getPathClass('class1')
class2 = getPathClass('class2')


// Get all the class1 annotations that don't contain a class2 object as a direct child
toRemove = getAnnotationObjects().findAll {
    if (it.getPathClass() != class1)
        return false
    children = it.getChildObjects()
    return !children.any {it.getPathClass() == class2}
}


// Remove annotations meeting that criteria
removeObjects(toRemove, true)