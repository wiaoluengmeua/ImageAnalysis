//Based off of script by melvingelbard 
//https://forum.image.sc/t/setcellintensityclassifications-for-a-certain-class/33347/3
//0.2.0
def measurementName = "Nucleus: Hematoxylin OD min";
def thresholds = [0.05, 0.2] as double[];

classCells = getCellObjects().findAll{it.getPathClass() == getPathClass("Tumor")}

setIntensityClassifications(classCells, measurementName, thresholds);