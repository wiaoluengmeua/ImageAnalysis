guiscript=true
//Script demonstrates the use of guiscript=true to prevent threading errors (try turning it off!)
//and using subtraction and selection in combination with built in functions like dilation

//Loop starting from one annotation object with no other objects present to contstraing it
//Create bands

//0.1.2

import qupath.lib.roi.*
import qupath.lib.objects.*

bands = 5

first = getAnnotationObjects()
first[0].setName("0")
firstROI = first[0].getROI()
firstROIClass = first[0].getPathClass()
//test = []
for (i=1; i<=bands; i++){
    j=i-1
    print j
    currentObjects = getAnnotationObjects().findAll{it.getName() == j.toString()}
    currentObjects[0].setName(i.toString())
    selectObjects{it.getName() == i.toString()}
    //Thread.sleep(2000); 
    runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": 100.0,  "removeInterior": false,  "constrainToParent": true}');
    
    fireHierarchyUpdate()
    j = j.toString()
    print(currentObjects)
    currentObjects[0].setName(j.toString())

}

for (i=bands; i>0; i--){
    toRingify = getAnnotationObjects().findAll{it.getName() == i.toString()}
    j=i-1
    ringHole = getAnnotationObjects().findAll{it.getName() == j.toString()}
    
    ring = PathROIToolsAwt.combineROIs(toRingify[0].getROI(), ringHole[0].getROI(), PathROIToolsAwt.CombineOp.SUBTRACT)
    addObjects(new PathAnnotationObject(ring, toRingify[0].getPathClass()))
    ring = getAnnotationObjects().findAll{it.getName() == null}
    ring[0].setName(i.toString())
    removeObjects(toRingify,true)
    fireHierarchyUpdate()
}