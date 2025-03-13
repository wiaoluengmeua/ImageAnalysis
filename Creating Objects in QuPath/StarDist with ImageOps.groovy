// https://forum.image.sc/t/nuclear-dab-disrupts-stardist-detection-of-haematoxylin-in-qupath/50156/2

import qupath.opencv.ops.ImageOp
import qupath.opencv.tools.OpenCVTools
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.global.opencv_core

import static qupath.lib.gui.scripting.QPEx.*

import qupath.tensorflow.stardist.StarDist2D
import qupath.lib.images.servers.*

// Specify the model directory (you will need to change this!)
def pathModel = '/path/to/dsb2018_heavy_augment'
double originalPixelSize = getCurrentImageData().getServer().getPixelCalibration().getAveragedPixelSizeMicrons();

def stardist = StarDist2D.builder(pathModel)
        .threshold(0.5) // Probability (detection) threshold
        .channels(
                ColorTransforms.createColorDeconvolvedChannel(getCurrentImageData().getColorDeconvolutionStains(), 1),
                ColorTransforms.createColorDeconvolvedChannel(getCurrentImageData().getColorDeconvolutionStains(), 2)
        ) // Select detection channel
        .preprocess(new AddChannelsOp())
        .normalizePercentiles(1, 99) // Percentile normalization
        .pixelSize(originalPixelSize) // Resolution for detection
        .cellExpansion(3.0) // Approximate cells based upon nucleus expansion
        .cellConstrainScale(1.5) // Constrain cell expansion using nucleus size
        .measureShape() // Add shape measurements
        .measureIntensity() // Add cell measurements (in all compartments)
        .includeProbability(true) // Add probability as a measurement (enables later filtering)
        .build()

// Run detection for the selected objects
def imageData = getCurrentImageData()
def pathObjects = getSelectedObjects()
if (pathObjects.isEmpty()) {
    Dialogs.showErrorMessage("StarDist", "Please select a parent object!")
    return
}
stardist.detectObjects(imageData, pathObjects)
println 'Done!'



class AddChannelsOp implements ImageOp {

    @Override
    public Mat apply(Mat input) {
        def channels = OpenCVTools.splitChannels(input)
        if (channels.size() == 1)
            return input
        def sum = opencv_core.add(channels[0], channels[1])
        for (int i = 2; i < channels.size(); i++)
            sum = opencv_core.add(sum, channels[i])
        return sum.asMat()
    }

}