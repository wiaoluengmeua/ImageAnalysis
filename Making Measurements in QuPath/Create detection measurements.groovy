//Generating measurements in detections from other measurements created in QuPath
//0.1.2 and 0.2.0
detections = getDetectionObjects()

detections.each{
    relativeDistribution2 = measurement(it, "ROI: 2.00 µm per pixel: Channel 2:  Mean")/measurement(it, "ROI: 2.00 µm per pixel: Channel 2:  Median")
    it.getMeasurementList().putMeasurement("RelativeCh2", relativeDistribution2)
}
println("done")