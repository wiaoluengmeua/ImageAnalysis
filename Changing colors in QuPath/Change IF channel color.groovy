// Get access to the display info for each channel
//An even better script from https://forum.image.sc/t/qupath-scripts-dont-detect-updated-colormaps/63618/9?u=research_associate

setImageType('FLUORESCENCE')

// You can replace the names with your stainings if you want

// Define minimum display values, maximum display values and channel names in order
def mins = [ 0, 0, 0, 0 ]
def maxs = [ 8000, 750, 2000, 2500 ]
def names = ['DAPI', 'FITC', 'TRITC',  'CY3' ]

// Define colors
def color1 = getColorRGB( 0, 128, 255 )
def color2 = getColorRGB( 0, 255, 128 )
def color3 = getColorRGB( 255, 0, 128 )
def color4 = getColorRGB( 255, 128, 255 )

// Build color array
def colors = [ color1, color2, color3, color4 ]

//Finally set everything for the current image
setChannelNames( *names )

setChannelColors( *colors )

[mins, maxs].transpose().eachWithIndex{ mima, i -> setChannelDisplayRange(i, mima[0], mima[1]) }

////////////////////////////////////////////
///////////////////////////////////////////


//For 0.2.0+ use: https://forum.image.sc/t/a-bug-in-batch-processing/40956/2?u=research_associate
setChannelColors(
    getColorRGB(0, 0, 255),
    getColorRGB(0, 255, 0),
    getColorRGB(255, 0, 255),
    getColorRGB(255, 0, 0)
)
//https://forum.image.sc/t/script-to-rename-channels-and-adjust-brightness-contrast/40984/4?u=research_associate
//setChannelColor
//setChannelNames
//setChannelDisplayRange






//This function changed between 0.1.2 and 0.2.0, in 0.2.0 use viewer.getImageDisplay().availableChannels()
def viewer = getCurrentViewer()
def channels = viewer.getImageDisplay().getAvailableChannels()

// Set the LUT color for the first channel & repaint
channels[0].setLUTColor(0, 0, 255)
channels[1].setLUTColor(255, 255, 255)
channels[2].setLUTColor(0, 255, 0)
channels[3].setLUTColor(255, 0, 0)

// Ensure the updates are visible
viewer.repaintEntireImage()

// Usually a good idea to print something, so we know it finished
print 'Done!'