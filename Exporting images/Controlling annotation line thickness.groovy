//From this post: https://forum.image.sc/t/macro-image-displaying-annotations/30214/14?u=research_associate

double downsample = 100.0

def server = getCurrentServer()
def request = RegionRequest.createInstance(server, downsample)
def img = server.readBufferedImage(request)

float thickness = 2

def g2d = img.createGraphics()
g2d.setColor(java.awt.Color.BLACK)
g2d.scale(1.0/downsample, 1.0/downsample)
g2d.setStroke(new java.awt.BasicStroke((float)(thickness * downsample)))
getAnnotationObjects().each { g2d.draw(it.getROI().getShape()) } 
g2d.dispose()

def path = buildFilePath(PROJECT_BASE_DIR, 'something.png')
writeImage(img, path)