//Takes all non ellipses and modifies the names of those annotations
//Classifies all ellipses
// https://groups.google.com/forum/#!topic/qupath-users/rKHqWQHhaEE
// 0.1.2 0.2.0

// Get all annotations with ellipse ROIs
def annotations = getAnnotationObjects()
def ellipses = annotations.findAll {it.getROI() instanceof qupath.lib.roi.EllipseROI}

// Assign classifications to nodes

def classification = getPathClass('Node')

ellipses.each {it.setPathClass(classification)}

// Assign names to all other annotations
annotations.removeAll(ellipses)
annotations.eachWithIndex {annotation, i -> annotation.setName('Annotation ' + (i+1))}

fireHierarchyUpdate()