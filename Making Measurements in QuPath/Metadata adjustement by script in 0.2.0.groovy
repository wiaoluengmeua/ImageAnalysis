//Sometimes you need to set the metadata for a group of images, like TIFF files.
//0.2.0
//Other script is shorter!
import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.images.servers.ImageServerMetadata

def imageData = getCurrentImageData()
def server = imageData.getServer()

def oldMetadata = server.getMetadata()
def newMetadata = new ImageServerMetadata.Builder(oldMetadata)
    .magnification(10.0)
    .pixelSizeMicrons(1.25, 1.25)
    .build()
imageData.updateServerMetadata(newMetadata)