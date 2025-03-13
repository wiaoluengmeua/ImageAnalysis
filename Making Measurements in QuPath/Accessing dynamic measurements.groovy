//Courtesy of Olivier Burri on QuPath Gitter
//For 0.1.2
//import qupath.lib.gui.models.ObservableMeasurementTableData

//For 0.2.0
import qupath.lib.gui.measure.ObservableMeasurementTableData


def ob = new ObservableMeasurementTableData();

def annotations = getAnnotationObjects()
 // This line creates all the measurements
 ob.setImageData(getCurrentImageData(),  annotations);


annotations.each { annotation->println( ob.getNumericValue(annotation, "H-score") )
}
/*
Using this script to access the X and Y coordinates per cell
cells = getCellObjects()
ob = new ObservableMeasurementTableData();
ob.setImageData(getCurrentImageData(),  cells);

cells.each{
    print ob.getNumericValue(it, "Centroid X µm")
    
}

import qupath.lib.gui.measure.ObservableMeasurementTableData



/*
Using this script to calculate circularity for annotations

import qupath.lib.gui.models.ObservableMeasurementTableData
def ob = new ObservableMeasurementTableData();

def annotations = getAnnotationObjects()
 // This line creates all the measurements
ob.setImageData(getCurrentImageData(),  annotations);


annotations.each { 
    area=ob.getNumericValue(it, "Area µm^2")
    perimeter=ob.getNumericValue(it, "Perimeter µm")
    circularity = 4*3.14159*area/(perimeter*perimeter)
    it.getMeasurementList().putMeasurement("Circularity", circularity)
}

*/