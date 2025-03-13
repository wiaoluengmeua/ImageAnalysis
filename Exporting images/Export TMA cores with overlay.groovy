//Tested for 0.2.3

// Write the full image, displaying objects according to how they are currently shown in the viewer
double downsample = 10.0
def server = getCurrentServer()
def name = getProjectEntry().getImageName()
def viewer = getCurrentViewer()

getCurrentHierarchy().getTMAGrid().getTMACoreList().each{
    mkdirs(buildFilePath(PROJECT_BASE_DIR,'export'))
    def path = buildFilePath(PROJECT_BASE_DIR,'export', name+" "+ it.getName()+".tif")
    def request = RegionRequest.createInstance(server.getPath(), downsample, it.getROI())
    writeRenderedImageRegion(viewer,request, path)
}
print "Done"
