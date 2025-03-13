//accesses the highest level objects without cycling through all objects or annotations
//Important to note that this list is DYNAMIC and will adjust as objects are created or destroyed.
//0.1.2 or 0.2.0
//may want to resolveHierarchy() before running this in 0.2.0
topLevel = getCurrentHierarchy().getRootObject().getChildObjects()