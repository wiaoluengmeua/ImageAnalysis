//Useful if you are trimming an annotation after having generated detections, and want a quick way to eliminate
//cells now outside of your annotated regions.

selectObjects{p -> (p.getLevel()==1) && (p.isAnnotation() == false)};
clearSelectedObjects(false);