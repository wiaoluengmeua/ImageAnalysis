// label cells within an annotation within a TMA core by the TMA core, not the annotation.
// Remove one getParent if there is no tissue annotation.
// 0.1.2 and 0.2.0
getDetectionObjects() each {detection -> detection.setName(detection.getParent().getParent().getName())}
fireHierarchyUpdate()
