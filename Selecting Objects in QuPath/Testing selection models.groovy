//This is just a set of tests for figuring out how selection models work.
//0.1.2

//Select Tumor annotations within named Organ annotations
hierarchy = getCurrentHierarchy()
hierarchy.getSelectionModel().clearSelection();

def organs = getAnnotationObjects().findAll {it.getDisplayedName().equalsIgnoreCase("Organ")}

organs.each{hierarchy.getSelectionModel().selectObjects(hierarchy.getDescendantObjects(it, null, null).findAll{it.getPathClass() == getPathClass("Tumor")})}

//Another variant
def hierarchy = getCurrentHierarchy()
hierarchy.getSelectionModel().clearSelection()
hierarchy.getSelectionModel().selectObjects(getAnnotationObjects().findAll {it.getPathClass() == getPathClass("Tumor") &&
 it.getParent().getDisplayedName().equalsIgnoreCase("Organ") && it.getLevel() != 1})