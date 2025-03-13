//0.2.0
// https://forum.image.sc/t/qupath-toggle-show-hide-detections-annotations-by-scripting/45916/4
def overlayOptions = getCurrentViewer().getOverlayOptions()
overlayOptions.hiddenClassesProperty().addAll(
    getPathClass('Tumor'),
    getPathClass('Stroma')    
)