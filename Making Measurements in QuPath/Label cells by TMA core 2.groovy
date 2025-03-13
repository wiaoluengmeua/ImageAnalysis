//better way to label cells by TMA core
//0.1.2

hierarchy = getCurrentHierarchy()

hierarchy.getTMAGrid().getTMACoreList().each{
    coreName = it.getName()
    hierarchy.getDescendantObjects(it, null, qupath.lib.objects.PathCellObject).each{ c->
        c.setName(coreName)
    }
}

/* Version to specifically rename objects in annotations one level below the TMA.

hierarchy = getCurrentHierarchy()

hierarchy.getTMAGrid().getTMACoreList().each{
    coreName = it.getName()
    hierarchy.getDescendantObjects(it, null, qupath.lib.objects.PathCellObject).each{ c->
        if (c.getLevel() == 3){
            cellName = c.getPathClass().toString()
            print cellName
            c.setName(coreName+" - "+cellName)
        }
    }
}
*/