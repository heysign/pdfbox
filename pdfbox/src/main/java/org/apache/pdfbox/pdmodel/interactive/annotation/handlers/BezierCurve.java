package org.apache.pdfbox.pdmodel.interactive.annotation.handlers;

import java.awt.*;
import java.awt.geom.Point2D;

public class BezierCurve {
    private static final float AP = 0.5f;
    private Point2D[] bPoints;

    /**
     * Creates a new Bezier curve.
     * @param points
     */
    public BezierCurve(Point2D[] points) {
        int n = points.length;
        if (n < 3) {
            // Cannot create bezier with less than 3 points
            return;
        }
        bPoints = new Point[2 * (n - 2)];
        double paX, paY;
        double pbX = points[0].getX();
        double pbY = points[0].getY();
        double pcX = points[1].getX();
        double pcY = points[1].getY();
        for (int i = 0; i < n - 2; i++) {
            paX = pbX;
            paY = pbY;
            pbX = pcX;
            pbY = pcY;
            pcX = points[i + 2].getX();
            pcY = points[i + 2].getY();
            double abX = pbX - paX;
            double abY = pbY - paY;
            double acX = pcX - paX;
            double acY = pcY - paY;
            double lac = Math.sqrt(acX * acX + acY * acY);
            acX = acX /lac;
            acY = acY /lac;

            double proj = abX * acX + abY * acY;
            proj = proj < 0 ? -proj : proj;
            double apX = proj * acX;
            double apY = proj * acY;

            double p1X = pbX - AP * apX;
            double p1Y = pbY - AP * apY;
            bPoints[2 * i] = new Point((int) p1X, (int) p1Y);

            acX = -acX;
            acY = -acY;
            double cbX = pbX - pcX;
            double cbY = pbY - pcY;
            proj = cbX * acX + cbY * acY;
            proj = proj < 0 ? -proj : proj;
            apX = proj * acX;
            apY = proj * acY;

            double p2X = pbX - AP * apX;
            double p2Y = pbY - AP * apY;
            bPoints[2 * i + 1] = new Point((int) p2X, (int) p2Y);
        }
    }

    /**
     * Returns the calculated bezier points.
     * @return the calculated bezier points
     */
    public Point2D[] getPoints() {
        return bPoints;
    }

    /**
     * Returns the number of bezier points.
     * @return number of bezier points
     */
    public int getPointCount() {
        return bPoints.length;
    }

    /**
     * Returns the bezier points at position i.
     * @param i
     * @return the bezier point at position i
     */
    public Point2D getPoint(int i) {
        return bPoints[i];
    }
}
