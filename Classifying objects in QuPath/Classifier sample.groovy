//Simplified script for classifying cells based on their values.  Can easily be dramatically expanded as much as you may like
//by adding features and thresholds
//If all of your results are showing up as one class, it is probably because the feature variable is not exaaaactly correct.
//It has to be character for character the same as what the program uses, sorry!  Some of the scripts in Coding Helper Functions
//might help with this if you are having trouble.

//0.1.2 and 0.2.0

import qupath.lib.objects.classes.PathClass
import qupath.lib.objects.classes.PathClassFactory


//This part resets the classifier so that you can run it again.  Clearing detection classifications would also clear your subcellular detections

for (def cell : getCellObjects())
	cell.setPathClass(null)
fireHierarchyUpdate()


// Parameters to modify
def feature = "Subcellular: Channel 2: Num spots estimated"

//Not using these in this example, but they could be used to further expand the classifier below
//def threshold = 1
//def feature2 = "Cytoplasm: Channel 2 mean"
//def threshold2 = 30


// Check what base classifications we should be worried about
// It's possible to specify 'All', or select specific classes and exclude others

def Negative = PathClassFactory.getPathClass("Negative")
def Lowest = PathClassFactory.getPathClass("1-3 spots")
def Low = PathClassFactory.getPathClass("4-9 spots")
def Medium = PathClassFactory.getPathClass("10-15 spots")
def High = PathClassFactory.getPathClass("15+ spots")


// Loop through all cells
for (def cell : getCellObjects()) {
        //Assign the measurement for "feature" from above for this cell to a variable.  In this case I named it spots
        double spots = cell.getMeasurementList().getMeasurementValue(feature)
  
        //If you need further measurements for your classifier, get them here
        //double val2 = pathObject.getMeasurementList().getMeasurementValue(feature2)

        // Set class based on the value(s) obtained
        if ( spots > 15){
            cell.setPathClass(High)
        }else if ( spots > 9 ){
            cell.setPathClass(Medium)
        }else if ( spots > 3 ){
            cell.setPathClass(Low)
        }else if ( spots > 1 ){
            cell.setPathClass(Lowest)
        }else cell.setPathClass(Negative)

}

// Fire update event
fireHierarchyUpdate()

// Make sure we know we're done
println("Done!")

