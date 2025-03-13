//https://forum.image.sc/t/mask-script-doesnt-work-in-qupath-m4/33259
//@thomasleb0n

import qupath.lib.regions.*
import ij.*
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

// Read RGB image & show in ImageJ (won't work for multichannel!)
double downsample = 1.0
def server = getCurrentImageData().getServer()
int w = (server.getWidth() / downsample) as int
int h = (server.getHeight() / downsample) as int
def img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY)


def g2d = img.createGraphics()
g2d.scale(1.0/downsample, 1.0/downsample)
g2d.setColor(Color.WHITE)
for (detection in getDetectionObjects()) {
  roi = detection.getROI()
  def shape = roi.getShape()
  g2d.fill(shape)

}
g2d.dispose()
new ImagePlus("Mask", img).show()
def name = getProjectEntry().getImageName() //+ '.tiff'
def path = buildFilePath(PROJECT_BASE_DIR, 'mask')
mkdirs(path)
def fileoutput = new File( path, name+ '-mask.png')
ImageIO.write(img, 'PNG', fileoutput)
println('Results exporting...')