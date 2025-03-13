// From https://forum.image.sc/t/qupath-multiple-image-alignment-and-object-transfer/35521
// 0.2.0m9
//Paste matrix between brackets for 'matrix' variable
//Current image should be the destination image
// Michael Nelson 03/2020
def name = getProjectEntry().getImageName()
path = buildFilePath(PROJECT_BASE_DIR, 'Affine')
mkdirs(path)
path = buildFilePath(PROJECT_BASE_DIR, 'Affine', name)

def matrix = []

new File(path).withObjectOutputStream {
    it.writeObject(matrix)
}
print 'Done!'