/////////////////////////////////////
//To obtain the metadata
/////////////////////////////////////

import static qupath.lib.gui.scripting.QPEx.*

// Get the current image data
def imageData = getCurrentImageData()
if (imageData == null) {
    println("No image loaded!")
    return
}

// Get the metadata
def server = imageData.getServer()

// Retrieve pixel size in micrometers
def pixelWidth = server.getPixelCalibration().getPixelWidthMicrons()
def pixelHeight = server.getPixelCalibration().getPixelHeightMicrons()

// Retrieve the image size in pixels
def imageWidth = server.getWidth()
def imageHeight = server.getHeight()

// Print the metadata
println "Image metadata:"
println "  Pixel size: ${pixelWidth} µm/pixel (width) x ${pixelHeight} µm/pixel (height)"
println "  Image size: ${imageWidth} pixels (width) x ${imageHeight} pixels (height)"


/////////////////////////////////////
//To get the annotation names
/////////////////////////////////////
// Grab the PathObjectHierarchy
def hierarchy = getCurrentHierarchy()

// Get the root object (often the whole image or project entry point)
def rootObject = hierarchy.getRootObject()

/**
 * Recursive function to print annotation objects in a tree structure.
 * 
 * @param pathObject  The current PathObject
 * @param level       Indentation level used for printing
 */
def printAnnotationHierarchy(pathObject, int level = 0) {
    // If this object is an annotation, print it
    if (pathObject.isAnnotation()) {
        // Create indentation
        def indent = '  ' * level
        // Use getName() if it exists, falling back to something else if no name is assigned
        def annotationName = pathObject.getName() ?: '(unnamed annotation)'
        println("${indent}- ${annotationName}")
    }
    
    // Recurse through children
    for (child in pathObject.getChildObjects()) {
        printAnnotationHierarchy(child, level + 1)
    }
}

// Start at the root and recursively print all child annotations
printAnnotationHierarchy(rootObject, 0)

///////////////////////////////////////////////////////////
//alternatively, you can use this 
//////////////////////////////////////////////////////////
def hierarchy = getCurrentHierarchy()
def root = hierarchy.getRootObject() // Correct method name

def printHierarchy(annotation, level) {
    println("${'  ' * level}${annotation.getName()}")
    annotation.getChildObjects().each { child -> // Use getChildObjects()
        printHierarchy(child, level + 1)
    }
}

// Start with the root's children (top-level annotations)
root.getChildObjects().each { child -> // Use getChildObjects()
    printHierarchy(child, 0)
}


/////////////////////////////////////
//To get all channel names
/////////////////////////////////////
// Print all available channels in the current image
def imageData = getCurrentImageData()
if (imageData == null) {
    println "No image is open!"
    return
}

def server = imageData.getServer()
def imageType = imageData.getImageType()
println "Image type: ${imageType}"

// For fluorescence images
if (imageType.toString() == "Fluorescence" || imageType.toString().contains("Fluorescence")) {
    def channels = server.getMetadata().getChannels()
    println "Available channels (${channels.size()}):"
    channels.eachWithIndex { channel, i ->
        println "${i}: ${channel.getName()}"
    }
} 
// For brightfield images
else if (imageType.toString().contains("Brightfield")) {
    println "Brightfield image - standard channels available:"
    println "- HTX (Hematoxylin)"
    println "- DAB"
    println "- Residual"
    println "- Average"
} 
else {
    println "Unknown image type: ${imageType}"
    println "Raw channel count: ${server.nChannels()}"
    // Try to still extract some channel info
    try {
        def channels = server.getMetadata().getChannels()
        if (channels) {
            println "Channels found:"
            channels.each { println "- ${it.getName()}" }
        }
    } catch (Exception e) {
        println "Could not extract channel information"
    }
}


/////////////////////////////////////
//To get all channel names (alternatively)
/////////////////////////////////////
def channelNames = getCurrentServer().getMetadata().getChannels().collect { c -> c.name }
println channelNames





