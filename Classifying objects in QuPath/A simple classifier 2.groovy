// From Pete's post on Gitter, another way of applying cell classifications
//0.1.2 and 0.2.0 (though channel names have changed in 0.2.0)
// Get cells & reset all the classifications
def cells = getCellObjects()
resetDetectionClassifications()

cells.each {it.setPathClass(getPathClass('Negative'))}

// Get channel 1 & 2 positives
def ch1Pos = cells.findAll {measurement(it, "Nucleus: Channel 1 mean") > 5}
ch1Pos.each {it.setPathClass(getPathClass('Ch 1 positive'))}

def ch2Pos = cells.findAll {measurement(it, "Nucleus: Channel 2 mean") > 0.2}
ch2Pos.each {it.setPathClass(getPathClass('Ch 2 positive'))}

// Overwrite classifications for double positives
def doublePos = ch1Pos.intersect(ch2Pos)
doublePos.each {it.setPathClass(getPathClass('Double positive'))}

fireHierarchyUpdate()