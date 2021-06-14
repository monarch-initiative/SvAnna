package org.jax.svanna.cli.writer.html.svg;

public class SvgUtil {


    public static String svgbox(int xpos, int ypos, int width, int height, String stroke, String fill) {
        return svgbox(xpos, ypos, width, (double) height, stroke, fill);


    }

    public static String svgbox(double xpos, double ypos, double width, double height, String stroke, String fill) {
        return String.format("<rect x=\"%f\" y=\"%f\" width=\"%f\" height=\"%f\" " +
                        "style=\"stroke:%s; fill: %s\" />\n",
                xpos, ypos, width, height, stroke, fill);
    }

    /** write a box with no fill */
    public static String svgbox(double xpos, double ypos, double width, double height, String stroke) {
        return String.format("<rect x=\"%f\" y=\"%f\" width=\"%f\" height=\"%f\" " +
                        "style=\"stroke:%s;fill:%s\" />\n",
                xpos, ypos, width, height, stroke, SvSvgGenerator.WHITE);
    }

    public static String svgtext(double xpos, double ypos, String color, String message) {
        return String.format("<text x=\"%f\" y=\"%f\" fill=\"%s\">%s</text>\n",
                xpos, ypos, color, message);
    }

    public static String svgtext(double xpos, double ypos, String color, String message, int fontsize, int yoffset) {
        return String.format("<text x=\"%f\" y=\"%f\" style=\"sfill=:%s;font-size: %dpx\">%s</text>\n",
                xpos, ypos+yoffset, color, fontsize, message);
    }


    public static String svgline(double x1, double y1, double x2, double y2) {
        return String.format("<line x1=\"%f\" y1=\"%f\"  x2=\"%f\"  y2=\"%f\" style=\"stroke: #000000; fill:none;" +
                        " stroke-width: 1px;\" />\n",x1,y1,x2,y2);
    }


    public static String svgdashedline(double x1, double y1, double x2, double y2, int dash1, int dash2) {
        return String.format("<line x1=\"%f\" y1=\"%f\"  x2=\"%f\"  y2=\"%f\" style=\"stroke: #000000; fill:none;" +
                " stroke-width: 1px;" +
                " stroke-dasharray: %d %d\" />\n",x1,y1,x2,y2, dash1, dash2);
    }



}
