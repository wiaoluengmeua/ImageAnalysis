import qupath.lib.roi.RoiTools
import qupath.lib.objects.PathObjects
import static qupath.lib.gui.scripting.QPEx.*

// Get all annotations
def annotations = getAnnotationObjects()

// Find the parent annotation "DAPI +15"
def dapiParent = annotations.find { annotation ->
    annotation.getName() == "DAPI +15"
}

// Find the parent annotation "Opal 620"
def opalParent = annotations.find { annotation ->
    annotation.getName() == "Opal 620"
}

// Ensure both parent annotations exist
if (dapiParent && opalParent) {
    // Get child annotations under "DAPI +15"
    def dapiChildren = dapiParent.getChildObjects().findAll { it.isAnnotation() }
    
    // Get the child annotation under "Opal 620"
    def opalChild = opalParent.getChildObjects().find { it.isAnnotation() }
    
    if (opalChild) {
        // Iterate over each DAPI child and compute overlap with the Opal child
        dapiChildren.each { dapiChild ->
            def intersectionROI = RoiTools.intersection(dapiChild.getROI(), opalChild.getROI())
            
            if (intersectionROI) {
                // Create a new annotation for the overlap
                def overlapAnnotation = PathObjects.createAnnotationObject(intersectionROI)
                
                // Optionally set a name or class for the overlap annotation
                overlapAnnotation.setName("Overlap: ${dapiChild.getName()} ∩ ${opalChild.getName()}")
                overlapAnnotation.setPathClass(getPathClass("Overlap"))
                
                // Add the new annotation to the hierarchy
                getCurrentHierarchy().addObject(overlapAnnotation)
            }
        }
        
        fireHierarchyUpdate()
        println("Overlap annotations created successfully!")
    } else {
        println("No child annotation found under 'Opal 620'!")
    }
} else {
    println("Parent annotation 'DAPI +15' or 'Opal 620' not found!")
}
