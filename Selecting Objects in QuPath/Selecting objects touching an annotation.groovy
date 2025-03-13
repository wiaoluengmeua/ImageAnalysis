//Sara McCardle

import static qupath.lib.gui.scripting.QPEx.*

def islets=getDetectionObjects()
def isletsgeos=islets.collect{it.getROI().getGeometry()}
def outsidegeo=getAnnotationObjects().find{it.getPathClass()==getPathClass("Tissue")}.getROI().getGeometry()

def intersections=[]
isletsgeos.eachWithIndex{entry,idx->
    if (entry.intersects(outsidegeo)){
        intersections<<idx
    }
}

print(intersections)
getCurrentHierarchy().getSelectionModel().selectObjects(islets[intersections])