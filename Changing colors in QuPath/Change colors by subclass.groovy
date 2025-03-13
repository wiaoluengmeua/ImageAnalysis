// https://groups.google.com/forum/#!topic/qupath-users/rBCRysCZEzM
//0.1.2 and 0.2.0
// Access the 'Stroma: Positive' sub-classification
stroma = getPathClass('Stroma')
stromaPositive = getDerivedPathClass(stroma, 'Positive')

// Set the color, using a packed RGB value
color = getColorRGB(200, 0, 0)
stromaPositive.setColor(color)

// Update the GUI
fireHierarchyUpdate()