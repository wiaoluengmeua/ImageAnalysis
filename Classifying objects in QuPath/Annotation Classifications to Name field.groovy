//Remove annotation classification and rename the annotation to the original class
//0.1.2 and 0.2.0

for (annotation in getAnnotationObjects()) {
    def pathClass = annotation.getPathClass()
    if (pathClass == null)
        continue
    annotation.setName(pathClass.getName())
//    annotation.setColorRGB(pathClass.getColor())
    annotation.setPathClass(null)
}
fireHierarchyUpdate()

//Restore the classification based on the annotation name (reverse the above effects)
for (annotation in getAnnotationObjects()) {
    def name = annotation.getName()
    if (name == null)
        continue
    def pathClass = getPathClass(name)
    annotation.setPathClass(pathClass)
}
fireHierarchyUpdate()