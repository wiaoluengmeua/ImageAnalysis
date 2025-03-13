def hierarchy = getCurrentHierarchy()
def root = hierarchy.getRootObject()

def renameHierarchy(annotation, parentPath) {
    // Skip renaming for root's direct children (level 1)
    if (!parentPath.isEmpty()) {
        // Construct name: ParentName-{last segment of parentPath}
        def baseName = annotation.getParent().getName()
        def suffix = parentPath.last()
        annotation.setName("${baseName}-${suffix}")
    }
    
    // Process children with updated path
    annotation.getChildObjects().eachWithIndex { child, index ->
        def childPath = parentPath + (index + 1)
        renameHierarchy(child, childPath)
    }
}

// Start with root's children (level 1), passing empty path
root.getChildObjects().eachWithIndex { child, index ->
    renameHierarchy(child, [])
}