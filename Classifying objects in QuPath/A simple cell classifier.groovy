// from http://forum.imagej.net/t/counting-double-labeled-cells-in-fiji/3832/2
//0.1.2 and 0.2.0 (though channel names have changed in 0.2.0)
positive = getPathClass('Positive')
negative = getPathClass('Negative')
for (cell in getCellObjects()) {
    ch1 = measurement(cell, 'Cell: Channel 1 mean')
    ch2 = measurement(cell, 'Cell: Channel 2 mean')
    if (ch1 > 100 && ch2 > 200)
        cell.setPathClass(positive)
    else
        cell.setPathClass(negative)
}
fireHierarchyUpdate()