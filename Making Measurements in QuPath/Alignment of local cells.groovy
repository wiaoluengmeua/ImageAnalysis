//Uses Ben Pearson's script from https://groups.google.com/forum/#!searchin/qupath-users/rotate%7Csort:date/qupath-users/UvkNb54fYco/ri_4K6tiCwAJ
//Creates an area defined by the ortho and paradist measurements (orthogonal to the cell's orientation or parallel)
//Calculates how similarly the cells with centroids inside of that area are aligned.
//Requires "Angles for cells.groovy" script below to be run first.
//0.2.0
//Edit below for 0.1.2


print "running, please wait, Very Slow Process."

def server = getCurrentImageData().getServer()
//For 0.1.2
//def sizePixels = server.getAveragedPixelSizeMicrons()
//For 0.2.0
sizePixels = server.getPixelCalibration().getAveragedPixelSizeMicrons()

//EDIT SHAPE OF REGION TO CHECK FOR ALIGNMENT HERE
def orthoDist = 40/sizePixels
def paraDist = 30/sizePixels

def triangleArea(double Ax,double Ay,double Bx,double By,double Cx,double Cy) {
    return (((Ax*(By- Cy)  +  Bx*(Cy-Ay)  +  Cx*(Ay-By))/2).abs())
}

//def DIST = 10
def cellList = getCellObjects()

//scan through all cells
getCellObjects().each{
    //get some values for th current cell
    def originalAngle = it.getMeasurementList().getMeasurementValue("Cell angle")
    def radians = Math.toRadians(-originalAngle)
    def cellX = it.getNucleusROI().getCentroidX()
    def cellY = it.getNucleusROI().getCentroidY()
    //create a list of nearby cells
/* SIMPLE VERSION, A BOX
    def nearbyCells = cellList.findAll{ c->
        DIST > server.getAveragedPixelSizeMicrons()*Math.sqrt(( c.getROI().getCentroidX() - cellX)*(c.getROI().getCentroidX() - cellX)+( c.getROI().getCentroidY() - cellY)*(c.getROI().getCentroidY() - cellY));
    } */

/*
    def roi = new RectangleROI(cellX-orthoDist/2, cellY-paraDist/2, orthoDist, paraDist)
    def points = roi.getPolygonPoints()
    def roiPointsArx = points.x.toArray()
    def roiPointsAry = points.y.toArray()
*/
    def roiPointsArx = [cellX-paraDist/2, cellX+paraDist/2, cellX+paraDist/2, cellX-paraDist/2 ]
    def roiPointsAry = [cellY+orthoDist/2, cellY+orthoDist/2, cellY-orthoDist/2, cellY-orthoDist/2 ]


    for (i= 0; i<  roiPointsAry.size(); i++)
        {
         // correct the center to 0
          roiPointsArx[i] = roiPointsArx[i] - cellX
          roiPointsAry[i] = roiPointsAry[i] - cellY

          //Makes prime placeholders, which allows the calculations x'=xcos(theta)-ysin(theta), y'=ycos(theta)+xsin(theta) to be performed
          double newPointX  = roiPointsArx[i]
          double newPointY = roiPointsAry[i]

          // then rotate

          roiPointsArx[i] = (newPointX * Math.cos(radians)) - (newPointY * Math.sin(radians))

          roiPointsAry[i] = (newPointY * Math.cos(radians)) + (newPointX * Math.sin(radians))

          // then move it back
          roiPointsArx[i] = roiPointsArx[i] + cellX
          roiPointsAry[i] = roiPointsAry[i] +  cellY

    }
//addObject(new PathAnnotationObject(new PolygonROI(roiPointsArx as float[], roiPointsAry as float[], -1, 0, 0)))
    def nearbyCells = cellList.findAll{ orthoDist*paraDist-5 < ( triangleArea(roiPointsArx[0], roiPointsAry[0],roiPointsArx[1] ,roiPointsAry[1], it.getNucleusROI().getCentroidX(),it.getNucleusROI().getCentroidY())
        +triangleArea(roiPointsArx[1], roiPointsAry[1],roiPointsArx[2] ,roiPointsAry[2], it.getNucleusROI().getCentroidX(),it.getNucleusROI().getCentroidY())
        +triangleArea(roiPointsArx[2], roiPointsAry[2],roiPointsArx[3] ,roiPointsAry[3], it.getNucleusROI().getCentroidX(),it.getNucleusROI().getCentroidY())
        +triangleArea(roiPointsArx[3], roiPointsAry[3],roiPointsArx[0] ,roiPointsAry[0], it.getNucleusROI().getCentroidX(),it.getNucleusROI().getCentroidY())) &&
        orthoDist*paraDist+5 >( triangleArea(roiPointsArx[0], roiPointsAry[0],roiPointsArx[1] ,roiPointsAry[1], it.getNucleusROI().getCentroidX(),it.getNucleusROI().getCentroidY())
        +triangleArea(roiPointsArx[1], roiPointsAry[1],roiPointsArx[2] ,roiPointsAry[2], it.getNucleusROI().getCentroidX(),it.getNucleusROI().getCentroidY())
        +triangleArea(roiPointsArx[2], roiPointsAry[2],roiPointsArx[3] ,roiPointsAry[3], it.getNucleusROI().getCentroidX(),it.getNucleusROI().getCentroidY())
        +triangleArea(roiPointsArx[3], roiPointsAry[3],roiPointsArx[0] ,roiPointsAry[0], it.getNucleusROI().getCentroidX(),it.getNucleusROI().getCentroidY()))
    }

    //print(nearbyCells)

    //prevent divide by zero errors
    if (nearbyCells.size() < 2){ it.getMeasurementList().putMeasurement("LMADSD", 90); return;}
    def angleList = []

    //within the local cells, find the differences in angle
    for (cell in nearbyCells){
        def currentAngle = cell.getMeasurementList().getMeasurementValue("Cell angle")
        def angleDifference = (currentAngle - originalAngle).abs()
        //angles between two objects should be at most 90 degrees, or perpendicular
        if (angleDifference > 90){
            angleList << (180 - Math.max(currentAngle, originalAngle)+Math.min(currentAngle,originalAngle))
        } else {angleList << angleDifference}
    }
    //complete the list with the original data point
    //angleList << 0

    //calculate the standard deviation of the angular differences
    def localAngleDifferenceMean = angleList.sum()/angleList.size()

    def variance = 0
    angleList.each{v-> variance += (v-localAngleDifferenceMean)*(v-localAngleDifferenceMean)}

    def stdDev = Math.sqrt(variance/(angleList.size()))
    // add measurement for local, mean angle difference, standard deviation
    //println("stddev "+stdDev)
    it.getMeasurementList().putMeasurement("LMADSD", stdDev)
}
print "done"