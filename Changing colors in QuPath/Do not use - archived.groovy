//From https://github.com/qupath/qupath/issues/191
//https://groups.google.com/forum/#!searchin/qupath-users/viewer%7Csort:date/qupath-users/uBMxJ_3JnBM/GkDahJw7EAAJ
// Get access to the display info for each channel
//0.1.2 
//For 0.2.0 use viewer.getImageDisplay().availableChannels()

/*def viewer = getCurrentViewer()
def channels = viewer.getImageDisplay().getAvailableChannels()


// Set the range for the first two channels
channels[0].setMinDisplay(0)
channels[0].setMaxDisplay(100)
channels[1].setMinDisplay(0)
channels[1].setMaxDisplay(500)

// Ensure the updates are visible
viewer.repaintEntireImage()*/

// Usually a good idea to print something, so we know it finished
print 'Done!'