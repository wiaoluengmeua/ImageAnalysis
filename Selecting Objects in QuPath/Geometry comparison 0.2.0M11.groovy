//Sara McArdle
// 0.2.0 assigns a pathClass to detections that have an exactly matching annotation.
import static qupath.lib.gui.scripting.QPEx.*

def annots=getAnnotationObjects()
def annotgeos=annots.collect{it.getROI().getGeometry()}
def dets=getDetectionObjects()
def detgeos=dets.collect{it.getROI().getGeometry()}

annotgeos.eachWithIndex{a, idx->
    def equality=detgeos.collect{it.equalsTopo(a)}
    int match=equality.findIndexOf {it==true}
    if (match>=0){
        dets[match].setPathClass(annots[idx].getPathClass())
    }
}
