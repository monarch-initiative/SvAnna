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


}
