//Calculate the Rsquared value to look for linear relationships between two measurements.
//See complex scripts for a GUI and plots
//0.1.2 and 0.2.0

//Use the findAll statement to select specific classes of cells
//it.getPathClass() == getPathClass("Tumor")

cells = getCellObjects().findAll{it}
def points = new double [cells.size()-1][2]

for(i=0;i < cells.size()-1; i++){ 
   points[i][0] = measurement(cells[i], "Nucleus: Area");
   points[i][1] = measurement(cells[i], "Nucleus: Perimeter")
}
line = bestFit(points)


//bestFit snagged from
//https://blog.kenweiner.com/2008/12/groovy-best-fit-line.html
def bestFit(pts) {
  // Find sums of x, y, xy, x^2
  n = pts.size()
  xSum = pts.collect() {p -> p[0]}.sum()
  ySum = pts.collect() {p -> p[1]}.sum()
  xySum = pts.collect() {p -> p[0]*p[1]}.sum()
  xSqSum = pts.collect() {p -> p[0]*p[0]}.sum()

  // Find m and b such that y = mx + b
  // m is the slope of the line and b is the y-intercept
  m = (n*xySum - xSum*ySum) / (n*xSqSum - xSum*xSum)
  b = (ySum - m*xSum) / n

  // Find start and end points based on the left-most and right-most points
  x1 = pts.collect() {p -> p[0]}.min()
  y1 = m*x1 + b
  x2 = pts.collect() {p -> p[0]}.max()
  y2 = m*x2 + b

  [[x1, y1], [x2, y2]]
  println("slope :"+m+" intercept :"+b)
  line = [m,b,ySum]
  return (line)
}

meanY = line[2]/points.size()


pointError = []
lineError = []
for (i=0; i<cells.size()-1; i++){
    pointError << (points[i][1]-meanY)*(points[i][1]-meanY)
    lineError << (line[0]*points[i][0]+line[1] - meanY)*(line[0]*points[i][0]+line[1] - meanY)
}

println("R^2 = "+ lineError.sum()/pointError.sum())

