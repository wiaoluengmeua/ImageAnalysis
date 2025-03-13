//QuPath 0.3.0
//https://forum.image.sc/t/is-it-possible-to-modify-the-cell-expansion-parameter-after-cells-have-been-detected/61665/3

def detections = getDetectionObjects()
def cells = CellTools.detectionsToCells(detections, 10, -1)
removeObjects(detections, true)
addObjects(cells)