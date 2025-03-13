// from https://groups.google.com/forum/#!searchin/qupath-users/rotate$20%7Csort:date/qupath-users/UvkNb54fYco/ri_4K6tiCwAJ
//0.1.2

import qupath.lib.objects.*
import qupath.lib.roi.*

//As usual, change this to target the annotations you want to rotate
def annotations = getAnnotationObjects()
//Set the number of degrees you wish to rotate your objects
double deg = -60
double degrees = Math.toRadians(deg)       // sets the number of degrees to rotate


for (j = 0; j < annotations.size; j++)
{
    def roi = annotations[j].getROI()
    def roiPoints = annotations[j].getROI().getPolygonPoints()
def roiPointsArx = roiPoints.x.toArray() //convert each point to an array
def roiPointsAry = roiPoints.y.toArray() //for x and y coordinates

//the centroid of the roi - which will be used to center the shape around (0,0)
double centroidX = roi.getCentroidX()
double centroidY = roi.getCentroidY()

for (i= 0; i<  roiPointsAry.length; i++)
{
     // correct the center to 0
      roiPointsArx[i] = roiPointsArx[i] - centroidX
      roiPointsAry[i] = roiPointsAry[i] -  centroidY
     
      //Makes prime placeholders, which allows the calculations x'=xcos(theta)-ysin(theta), y'=ycos(theta)+xsin(theta) to be performed
      double newPointX  = roiPointsArx[i]
      double newPointY = roiPointsAry[i]
     
      // then rotate
     
      roiPointsArx[i] = (newPointX * Math.cos(degrees)) - (newPointY * Math.sin(degrees))
     
      roiPointsAry[i] = (newPointY * Math.cos(degrees)) + (newPointX * Math.sin(degrees))
     
      // then move it back
      roiPointsArx[i] = roiPointsArx[i] + centroidX
      roiPointsAry[i] = roiPointsAry[i] +  centroidY
     
}


// then to convert it back into an object
def xFloat =  roiPointsArx as float[]
def yFloat =  roiPointsAry as float[]

def roiNew = new PolygonROI(xFloat, yFloat, -1, 0, 0)
def pathObjectNew = new PathAnnotationObject(roiNew)
addObject(pathObjectNew)
   
}
removeObjects(annotations, true)