//0.1.2
//Overall purpose: Groups of points are a single point object, and are not recorded as measurements within annotation objects.
//This script takes a group of created points, and counts which are within certain annotation regions.
//https://forum.image.sc/t/manual-annotation-and-measurements/25051/5?u=research_associate


//Main script start
//Assumes Tumor and Peri-tumor regions have been created and classified.
//Assumes Nerve Cell objects per area have been created
//Assumes no unclassified annotations prior to creating script

pixelSize = getCurrentImageData().getServer().getPixelHeightMicrons()

stroma = getAnnotationObjects().findAll{it.getPathClass() == getPathClass("Stroma") && it.getROI().isArea()}

totalArea = 0
stroma.each{
    totalArea += it.getROI().getArea()
}
totalArea = totalArea*pixelSize*pixelSize
println("total stroma "+totalArea)

periTumorArea = 0
periTumor = getAnnotationObjects().findAll{it.getPathClass() == getPathClass("periTumor")&& it.getROI().isArea()}
periTumor.each{
    periTumorArea += it.getROI().getArea()
}
periTumorArea = periTumorArea*pixelSize*pixelSize
println("peritumor area "+periTumorArea)
tumorArea = 0
tumor = getAnnotationObjects().findAll{it.getPathClass() == getPathClass("Tumor")&& it.getROI().isArea()}
tumor.each{
    tumorArea += it.getROI().getArea()
}
tumorArea = tumorArea*pixelSize*pixelSize
println("tumor area "+tumorArea)

totalPeriTumorArea = periTumorArea - tumorArea
println("adjusted peritumor area "+totalPeriTumorArea)
totalStromalArea = totalArea - periTumorArea
println("adjusted stroma area"+ totalStromalArea)

points = getAnnotationObjects().findAll{it.isPoint() }

createSelectAllObject(true);
resultsSummary = getAnnotationObjects().findAll{it.getPathClass() == null}
resultsSummary[0].setPathClass(getPathClass("Results"))
resultsSummary[0].getMeasurementList().putMeasurement("Stroma Area um^2", totalStromalArea)
resultsSummary[0].getMeasurementList().putMeasurement("Tumor Area um^2", tumorArea)
resultsSummary[0].getMeasurementList().putMeasurement("Peri-Tumor Area um^2",totalPeriTumorArea)

tumorPoints = points.findAll{it.getPathClass() == getPathClass("Tumor")}

totalTumorPoints = 0
tumorPoints.each{totalTumorPoints += it.getROI().getPointList().size()}
println("tumor nerves"+totalTumorPoints)

stromaPoints = points.findAll{it.getPathClass() == getPathClass("Stroma")}
totalStromaPoints = 0
stromaPoints.each{totalStromaPoints += it.getROI().getPointList().size()}
println("stroma nerves"+totalStromaPoints)
periTumorPoints = points.findAll{it.getPathClass() == getPathClass("periTumor")}
totalPeriTumorPoints = 0
periTumorPoints.each{totalPeriTumorPoints += it.getROI().getPointList().size()}
println("peritumor nerves"+totalPeriTumorPoints)
resultsSummary[0].getMeasurementList().putMeasurement("Stroma Nerves per mm^2",1000000*totalStromaPoints/totalStromalArea)
resultsSummary[0].getMeasurementList().putMeasurement("Tumor Nerves per mm^2",1000000*totalTumorPoints/tumorArea)
resultsSummary[0].getMeasurementList().putMeasurement("Peri-Tumor Nerves per mm^2",1000000*totalPeriTumorPoints/totalPeriTumorArea)
getAnnotationObjects().each{it.setLocked(true)}
print "Done!"
