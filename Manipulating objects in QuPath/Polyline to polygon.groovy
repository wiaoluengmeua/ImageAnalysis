// https://forum.image.sc/t/converting-a-polyline-annotation-into-a-polygon/36307/2?u=research_associate
//0.2.0
def lineObjects = getAnnotationObjects().findAll {it.getROI().isLine()}
def polygonObjects = []
for (lineObject in lineObjects) {
    def line = lineObject.getROI()
    def polygon = ROIs.createPolygonROI(line.getAllPoints(), line.getImagePlane())
    def polygonObject = PathObjects.createAnnotationObject(polygon, lineObject.getPathClass())
    polygonObject.setName(lineObject.getName())
    polygonObject.setColorRGB(lineObject.getColorRGB())
    polygonObjects << polygonObject
}
removeObjects(lineObjects, true)
addObjects(polygonObjects)