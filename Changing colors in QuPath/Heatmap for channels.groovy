//Use a heatmap for a fluorescent channel
//https://forum.image.sc/t/qupath-scripting-3-apply-colorlut-to-channel/50368
import qupath.lib.display.ChannelDisplayInfo
import qupath.lib.display.DirectServerChannelInfo
import qupath.lib.display.ImageDisplay
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.tools.MeasurementMapper
import qupath.lib.gui.viewer.QuPathViewer
import qupath.lib.common.ColorTools
import java.awt.image.IndexColorModel

byte[] rb = new byte[256];
byte[] gb = new byte[256];
byte[] bb = new byte[256];

// LUT fire
int[][] lutFire = [[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,4,7,10,13,16,19,22,25,28,31,34,37,40,43,46,49,52,55,58,61,64,67,70,73,76,79,82,85,88,91,94,98,101,104,107,110,113,116,119,122,125,128,131,134,137,140,143,146,148,150,152,154,156,158,160,162,163,164,166,167,168,170,171,173,174,175,177,178,179,181,182,184,185,186,188,189,190,192,193,195,196,198,199,201,202,204,205,207,208,209,210,212,213,214,215,217,218,220,221,223,224,226,227,229,230,231,233,234,235,237,238,240,241,243,244,246,247,249,250,252,252,252,253,253,253,254,254,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255],
                   [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,3,5,7,8,10,12,14,16,19,21,24,27,29,32,35,37,40,43,46,48,51,54,57,59,62,65,68,70,73,76,79,81,84,87,90,92,95,98,101,103,105,107,109,111,113,115,117,119,121,123,125,127,129,131,133,134,136,138,140,141,143,145,147,148,150,152,154,155,157,159,161,162,164,166,168,169,171,173,175,176,178,180,182,184,186,188,190,191,193,195,197,199,201,203,205,206,208,210,212,213,215,217,219,220,222,224,226,228,230,232,234,235,237,239,241,242,244,246,248,248,249,250,251,252,253,254,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255],
                   [0,7,15,22,30,38,45,53,61,65,69,74,78,82,87,91,96,100,104,108,113,117,121,125,130,134,138,143,147,151,156,160,165,168,171,175,178,181,185,188,192,195,199,202,206,209,213,216,220,220,221,222,223,224,225,226,227,224,222,220,218,216,214,212,210,206,202,199,195,191,188,184,181,177,173,169,166,162,158,154,151,147,143,140,136,132,129,125,122,118,114,111,107,103,100,96,93,89,85,82,78,74,71,67,64,60,56,53,49,45,42,38,35,31,27,23,20,16,12,8,5,4,3,3,2,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,8,13,17,21,26,30,35,42,50,58,66,74,82,90,98,105,113,121,129,136,144,152,160,167,175,183,191,199,207,215,223,227,231,235,239,243,247,251,255,255,255,255,255,255,255,255]]

QuPathViewer viewer = QuPathGUI.getInstance().getViewer()

// Get current channel
ImageDisplay display = viewer.getImageDisplay()
ChannelDisplayInfo channelinfoS = display.selectedChannels().get(0)


List<MeasurementMapper.ColorMapper> colormaps = MeasurementMapper.loadColorMappers()

int nColorMaps = 0
colormaps.each { map ->
    print nColorMaps + " : " + map.getName() + '\n'
    nColorMaps++
}

// Select your ColorMap here (or set to -1 for FireLUT)
// Currently
// 0 : Viridis
// 1 : Svidro2
// 2 : Plasma
// 3 : Magma
// 4 : Inferno
// 5 : Jet
int useColorMap = 1


if (useColorMap < 0 || useColorMap >= nColorMaps) {
    // Create FireLUT
    for (int i = 0; i < 256; i++) {
        rb[i] = (byte) ColorTools.do8BitRangeCheck(lutFire[0][i])
        gb[i] = (byte) ColorTools.do8BitRangeCheck(lutFire[1][i])
        bb[i] = (byte) ColorTools.do8BitRangeCheck(lutFire[2][i])
    }
}
else{
    // Read ColorMap
    def theMap = colormaps.get(useColorMap)
    for (int i = 0; i < 256; i++) {
        rb[i] = (byte) ColorTools.do8BitRangeCheck(theMap.r[i])
        gb[i] = (byte) ColorTools.do8BitRangeCheck(theMap.g[i])
        bb[i] = (byte) ColorTools.do8BitRangeCheck(theMap.b[i])
    }
}

if (channelinfoS != null && channelinfoS instanceof DirectServerChannelInfo){
    DirectServerChannelInfo directInfo = (DirectServerChannelInfo)channelinfoS

    directInfo.cm = new IndexColorModel(8, 256, rb, gb, bb);

    // Optional
    // Assign histogram color (Brightness&Contrast dialog)
    directInfo.rgb = ColorTools.makeRGB(200, 200, 200);

}

viewer.getImageRegionStore().clearCache(false, false)
viewer.repaintEntireImage()

println('Done!')