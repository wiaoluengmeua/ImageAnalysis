// Ensure the selected object is an annotation
def selectedObjects = getSelectedObjects()
if (selectedObjects.isEmpty()) {
    println("No annotation selected!")
    return
}

// Duplicate the selected annotation twice
duplicateSelectedAnnotations() // First duplication
duplicateSelectedAnnotations() // Second duplication

// Get the annotation objects (assuming the original and its duplicates are the only annotations in the image)
def annotations = getAnnotationObjects()

if (annotations.size() == 3) {
    // Assign annotations to variables
    def annotation1 = annotations[0] // Original annotation
    def annotation2 = annotations[1] // First duplicate
    def annotation3 = annotations[2] // Second duplicate

    // Rename the annotations
    annotation1.setName("DAPI")
    annotation2.setName("Opal 620")
    annotation3.setName("Opal 520")

    // Change the color of the second annotation (Opal 620) to red
    annotation2.setColor(255, 0, 0) // RGB for red

    // Print the areas of each annotation
    println("Area of Annotation 1 (DAPI): " + annotation1.getROI().getArea())
    println("Area of Annotation 2 (Opal 620): " + annotation2.getROI().getArea())
    println("Area of Annotation 3 (Opal 520): " + annotation3.getROI().getArea())
} else {
    println("Expected 3 annotations, found " + annotations.size())
}

// Trigger a hierarchy update to reflect the changes in the UI
fireHierarchyUpdate()
