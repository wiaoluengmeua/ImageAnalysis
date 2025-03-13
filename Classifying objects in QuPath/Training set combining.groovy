//https://github.com/qupath/qupath/issues/256
//0.1.2
// Paths to training files (here, both relative to the current project)
paths = [
    buildFilePath(PROJECT_BASE_DIR, 'training', 'my_training.qptrain'),
    buildFilePath(PROJECT_BASE_DIR, 'training', 'my_training2.qptrain'),
]

// Path to output training file
pathOutput = buildFilePath(PROJECT_BASE_DIR, 'training', 'merged.qptrain')

// Count mostly helps to ensure we're adding with unique keys
count = 0

// Loop through training files
def result = null
for (path in paths) {
    // .qptrain files just have one object but class isn't public, so 
    // we take the first one that is deserialized
    new File(path).withObjectInputStream {
        saved = it.readObject()
    }
    // Add the training objects, appending an extra number which 
    // (probably, unless very unfortunate with image names?) means they are unique
    map = new HashMap<>(saved.getMap())
    if (result == null) {
        result = saved
        result.clear()
    }
    for (entry in map.entrySet())
        result.put(entry.getKey() + '-' + count, entry.getValue())
    count++
}

// Check how big the map is & what it contains
print result.size()
print result.getMap().keySet().each { println it }

// Write out a new training file
new File(pathOutput).withObjectOutputStream {
    it.writeObject(result)
}