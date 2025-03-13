/** https://github.com/qupath/qupath/issues/199
 * Flip a QuPath ROI vertically or horizontally.
 *
 * This creates a new annotation & adds it to the current image hierarchy.
 *
 * This also shows the method by which any arbitrary AffineTransform may be
 * applied to a ROI by scripting.
 *
 * @author Pete Bankhead
 */


import qupath.lib.objects.PathAnnotationObject
import qupath.lib.roi.PathROIToolsAwt
import qupath.lib.scripting.QPEx

import java.awt.geom.AffineTransform

// Get selected object & its ROI
def selected = QPEx.getSelectedObject()
def roi = selected?.getROI()
if (roi == null) {
    print 'No ROI selected!'
    return
}

// Create the relevant transforms, incorporating the image dimensions
def server = QPEx.getCurrentImageData().getServer()

def flipHorizontal = AffineTransform.getScaleInstance(-1, 1)
flipHorizontal.translate(-server.getWidth(), 0)

def flipVertical = AffineTransform.getScaleInstance(1, -1)
flipVertical.translate(0, -server.getHeight())

// Get a Shape for the ROI & apply required transform
def shape = PathROIToolsAwt.getShape(roi)
def shape2 = flipHorizontal.createTransformedShape(shape)
//def shape2 = flipVertical.createTransformedShape(shape)

// Create & add a new annotation
def roi2 = PathROIToolsAwt.getShapeROI(shape2, roi.getC(), roi.getZ(), roi.getT(), 0.5)
QPEx.addObject(new PathAnnotationObject(roi2, selected.getPathClass()))