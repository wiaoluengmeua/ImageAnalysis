//Useful if you want to reset your classifier but do NOT want to reset other classifications, such as subcellular detections
//0.1.2 and 0.2.0

for (def cell : getCellObjects())
	cell.setPathClass(null)
fireHierarchyUpdate()
println("Done!")