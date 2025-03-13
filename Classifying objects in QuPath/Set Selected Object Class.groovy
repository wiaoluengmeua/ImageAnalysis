//Select an object or several objects before running.
//0.1.2 and 0.2.0
//Change Tumor to the class you want to add
def Class = getPathClass('Tumor')


selected = getSelectedObjects()
for (def detection in selected){
detection.setPathClass(Class)
}
fireHierarchyUpdate()
println("Done!")