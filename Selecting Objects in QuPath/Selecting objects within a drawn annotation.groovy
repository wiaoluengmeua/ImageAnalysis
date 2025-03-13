//This script allows you to draw a quick annotation, and then select all objects within it for... whatever reason
//It also deletes the drawn annotation by design with "clearSelectedObjects()" so comment out that line if you 
//want to keep your annotation
// 0.1.2

// Get the current selected object & hierarchy
selected = getSelectedObject()
hierarchy = getCurrentHierarchy()
// Get all the objects inside the current selection
objectsToSelect = hierarchy.getDescendantObjects(selected, null, null)
clearSelectedObjects()
if (objectsToSelect != null) {
    // Remove the current selected object
    hierarchy.removeObject(selected, true)
    // Update the selection
    hierarchy.getSelectionModel().selectObjects(objectsToSelect)
}