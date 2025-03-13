//0.2.0
import qupath.lib.gui.align.ImageServerOverlay

def overlay = getCurrentViewer().getCustomOverlayLayers().find {it instanceof ImageServerOverlay}
print overlay.getAffine()