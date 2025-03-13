// @petebankhead 0.2.3+
// https://forum.image.sc/t/qupath-scripting-1-using-clupath-to-save-smoothed-image-regions/49525/4

import qupath.lib.images.writers.ome.OMEPyramidWriter

def tilesize = 512
def outputDownsample = 1
def pyramidscaling = 2
def compression = OMEPyramidWriter.CompressionType.J2K_LOSSY     //J2K //UNCOMPRESSED //LZW

def imageData = getCurrentImageData()

def op = ImageOps.buildImageDataOp()
    .appendOps(ImageOps.Filters.gaussianBlur(10.0))
    
def serverSmooth = ImageOps.buildServer(imageData, op, imageData.getServer().getPixelCalibration())
print serverSmooth.getPreferredDownsamples()

def pathOutput = buildFilePath(PROJECT_BASE_DIR, "smoothed.ome.tif")

new OMEPyramidWriter.Builder(serverSmooth)
				.compression(compression)
				.parallelize()
				.tileSize(tilesize)
				.channelsInterleaved() // Usually faster
				.scaledDownsampling(outputDownsample, pyramidscaling)
				.build()
				.writePyramid(pathOutput)