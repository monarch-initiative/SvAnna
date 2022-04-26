package org.monarchinitiative.svanna.cli.writer.html.svg;

import java.util.HashMap;
import java.util.Map;

/**
 * Static SVG constants.
 */
class Constants {
    /**
     * Canvas width of the SVG
     */
    static final int SVG_WIDTH = 1400;

    static final int HEIGHT_FOR_SV_DISPLAY = 200;
    static final int HEIGHT_PER_DISPLAY_ITEM = 80;

    /** Height of the symbols that represent the repeat tracks */
    static final double REPEAT_HEIGHT = 15;

    private Constants() {
        // private no-op
    }

    private static final String Aqua ="#00FFFF";
    private static final String Aquamarine = "#7FFFD4";
    private final static String Blue = "#0000FF";
    private final static String BlueViolet = "#8A2BE2";
    private final static String Brown = "#A52A2A";
    private final static String BurlyWood = "#DEB887";
    private final static String CadetBlue = "#5F9EA0";
    private final static String Chartreuse = "#7FFF00";
    private final static String Chocolate = "#D2691E";
    private final static String Coral = "#FF7F50";
    private final static String CornflowerBlue = "#6495ED";
    private final static String Crimson = "#DC143C";
    private final static String Cyan = "#00FFFF";
    private final static String DarkBlue = "#00008B";
    private final static String DarkCyan = "#008B8B";
    private final static String DarkGreen = "#006400";
    private final static String DarkKhaki = "#BDB76B";
    private final static String DarkMagenta = "#8B008B";
    private final static String DarkOliveGreen = "#556B2F";
    private final static String Darkorange = "#FF8C00";
    private final static String DarkOrchid = "#9932CC";
    private final static String DarkRed = "#8B0000";
    private final static String DarkSalmon = "#E9967A";
    private final static String DarkSeaGreen = "#8FBC8F";
    private final static String DarkSlateBlue = "#483D8B";
    private final static String DarkSlateGray = "#2F4F4F";
    private final static String DarkTurquoise = "#00CED1";
    private final static String DarkViolet = "#9400D3";
    private final static String DeepPink = "#FF1493";
    private final static String DeepSkyBlue = "#00BFFF";
    private final static String DodgerBlue = "#1E90FF";
    private final static String FireBrick = "#B22222";
    private final static String ForestGreen = "#228B22";
    private final static String Fuchsia = "#FF00FF";
    private final static String Gainsboro = "#DCDCDC";
    private final static String Gold = "#FFD700";
    private final static String GoldenRod = "#DAA520";
    
    private final static Map<String,String> repeatToColorMap;
    
    static {
        repeatToColorMap = new HashMap<>();
        repeatToColorMap.put("SIMPLE_REPEAT", Aqua);
        repeatToColorMap.put("RNA_rRNA", Aquamarine);
        repeatToColorMap.put("DNA_hAT_Ac", Blue);
        repeatToColorMap.put("LTR_ERV1", BlueViolet);
        repeatToColorMap.put("SINE_5SDeuL2", Brown);
        repeatToColorMap.put("DNA_MULE_MuDR", BurlyWood);
        repeatToColorMap.put("LINE", Coral);
        repeatToColorMap.put("LOW_COMPLEXITY", Chartreuse);
        repeatToColorMap.put("RNA_tRNA", Chocolate);
        repeatToColorMap.put("SINE_tRNA_RTE", Cyan);
        repeatToColorMap.put("DNA_TcMar_Mariner", DarkBlue);
        repeatToColorMap.put("RETROPOSON", DarkCyan);
        repeatToColorMap.put("LTR_ERVL_MaLR", Darkorange);
        repeatToColorMap.put("LTR_ERVL", DarkGreen);
        repeatToColorMap.put("UNKNOWN", DarkKhaki);
        repeatToColorMap.put("LTR_Gypsy", FireBrick);
        repeatToColorMap.put("LTR_ERVK", Fuchsia);
        repeatToColorMap.put("DNA_hAT_Tip100", ForestGreen);
        repeatToColorMap.put("DNA_TcMar_Tigger", Gainsboro);
        repeatToColorMap.put("RNA_scRNA", Gold);
        repeatToColorMap.put("RC_HELITRON", GoldenRod);
        repeatToColorMap.put("DNA_hAT", DarkSalmon);
        repeatToColorMap.put("SINE_ALU", DarkTurquoise);
        repeatToColorMap.put("DNA_hAT_Charlie", DarkViolet);
        repeatToColorMap.put("SINE_tRNA", DeepPink);
        repeatToColorMap.put("LTR", DeepSkyBlue);
        repeatToColorMap.put("DNA", DodgerBlue);
        repeatToColorMap.put("DNA_TcMar_Tc2", DarkSlateGray);
        repeatToColorMap.put("DNA_hAT_Blackjack", DarkSeaGreen);
        repeatToColorMap.put("DNA_PiggyBac", DarkRed);
        repeatToColorMap.put("SINE_MIR", DarkOrchid);
        repeatToColorMap.put("RNA_srpRNA", DarkMagenta);
        repeatToColorMap.put("RNA_snRNA", DarkOliveGreen );

    }

    /**
     * Return a color. If we cannot find the repeat, return the color for UNKNOWN.
     * @param repeat name of a DNA repeat family
     * @return corresponding color
     */
   public static String repeatToColor(String repeat) {
        return repeatToColorMap.getOrDefault(repeat, DarkKhaki);
   }

}
