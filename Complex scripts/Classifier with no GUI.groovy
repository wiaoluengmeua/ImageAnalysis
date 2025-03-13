//V3 Corrected classification over-write error on classifiers with more than 3 parts

import qupath.lib.gui.helpers.ColorToolsFX;
import javafx.scene.paint.Color;

//Hopefully you can simply replace the fileName with your classifier, and include this is a script.
fileName = "MyClassifier"
positive = []
path = buildFilePath(PROJECT_BASE_DIR, "classifiers",fileName)
    new File(path).withObjectInputStream {
        cObj = it.readObject()
    }
        //Create an arraylist with the same number of entries as classes
    CHANNELS = cObj.size()
    //println(cObj)

        //set up for classifier
        def cells = getCellObjects()
        
        cells.each {it.setPathClass(getPathClass('Negative'))}

        //start classifier with all cells negative

        for (def i=0; i<CHANNELS; i++){
            def lower = Float.parseFloat(cObj[i][1])
            def upper = Float.parseFloat(cObj[i][3])
            //create lists for each measurement, classify cells based off of those measurements
            positive[i] = cells.findAll {measurement(it, cObj[i][0]) >= lower && measurement(it, cObj[i][0]) <= upper}
            positive[i].each {it.setPathClass(getPathClass(cObj[i][2]+' positive')); it.getMeasurementList().putMeasurement("ClassDepth", 1)}
            c = Color.web(cObj[i][4])
            currentPathClass = getPathClass(cObj[i][2]+' positive')
            //for some reason setColor needs to be used here instead of setColorRGB which applies to objects and not classes?
            currentPathClass.setColor(ColorToolsFX.getRGBA(c))
        }
        for (def i=0; i<(CHANNELS-1); i++){
            //println(i)
            int remaining = 0
            for (def j = i+1; j<CHANNELS; j++){
                remaining +=1
            }
            depth = 2
            classifier(cObj[i][2], positive[i], remaining, i)

        }

        fireHierarchyUpdate()
def classifier (listAName, listA, remainingListSize, position){
    //current point in the list of lists, allows access to the measurements needed to figure out what from the current class is also part of the next class
    for (def y=0; y <remainingListSize; y++){
        k = (position+y+1).intValue()
    // get the measurements needed to determine if a cell is a member of the next class (from listOfLists)
        def lower = Float.parseFloat(cObj[k][1])
        def upper = Float.parseFloat(cObj[k][3])
    //intersect the listA with the first of the listOfLists
    //on the first run, this would take all of Class 1, and compare it with measurements that determine Class 2, resulting in a subset of 
    //Class 1 that meet both criteria
        def passList = listA.findAll {measurement(it, cObj[k][0]) >= lower && measurement(it, cObj[k][0]) <= upper}
        newName = cObj[k][2]

    //Create a new name based off of the current name and the newly compared class
    // on the first runthrough this would give "Class 1,Class 2 positive"
        def mergeName = listAName+","+newName
        passList.each{
            if (it.getMeasurementList().getMeasurementValue("ClassDepth") < depth) {
                it.setPathClass(getPathClass(mergeName+' positive')); 
                it.getMeasurementList().putMeasurement("ClassDepth", depth)
            }
        }
         if (k == (positive.size()-1)){ 
        
            //println(passList.size()+"number of "+mergeName+" cells passed")
            for (def z=0; z<CHANNELS; z++){
                //println("before"+positive[z].size())
                
                positive[z] = positive[z].minus(passList)
                
                //println(z+" after "+positive[z].size())
            }
            depth -=1
            return;
        } else{ 
            def passAlong = remainingListSize-1
            //println("passAlong "+passAlong.size())
            //println("name for next " +mergeName)
            depth +=1
            classifier(mergeName, passList, passAlong, k)
        }
    }
}