//The information in this script could be generalized to alter the color for any object list, or any individual object (see Selecting things Gist)
//Another way to use it might be to create a list of names ["blue","green","red"] along with a list/map of groups of three values
// [[0,0,200],[0,200,0],[200,0,0]] and pull from each, which would create a predetermined rainbow of named objects
//0.1.2 and 0.2.0
def annotations = getAnnotationObjects()
for (i = 0; i<annotations.size(); i++){

        def j = i.mod(255)
        //modulus used to keep the RGB values in the 0-255 range
        annotations[i].setColorRGB(getColorRGB(255-j,j, j))
}
print "done"