//Clean up bad objects
removal = getCellObjects().findAll{it.getPathClass().toString().contains("Trash")}
removeObjects(removal, true)