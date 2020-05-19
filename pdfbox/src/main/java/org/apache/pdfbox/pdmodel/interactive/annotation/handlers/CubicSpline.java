package org.apache.pdfbox.pdmodel.interactive.annotation.handlers;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CubicSpline {
    private List<Point2D.Double> points;
    private List<Cubic> xCubics, yCubics;

    public class Cubic {
        private double a, b, c, d;

        public Cubic(double p0, double d2, double e, double f) {
            this.d = p0;
            this.c = d2;
            this.b = e;
            this.a = f;
        }

        public double eval(double u) {
            //equals a*x^3 + b*x^2 + c*x + d
            return (((a * u) + b) * u + c) * u + d;
        }
    }

    public CubicSpline() {
        this.points = new ArrayList<Point2D.Double>();
        this.xCubics = new ArrayList<Cubic>();
        this.yCubics = new ArrayList<Cubic>();
    }

    public void addPoint(Point2D.Double point) {
        this.points.add(point);
    }

    private enum PosField {
        X, Y;
    }

    private List<Double> extractValues(List<Point2D.Double> points, PosField field) {
        List<Double> ints = new ArrayList<Double>();
        for(Point2D.Double p : points) {
            switch(field) {
                case X:
                    ints.add(p.x);
                    break;
                case Y:
                    ints.add(p.y);
                    break;
            }
        }

        return ints;
    }

    public void calcSpline() {
        calcNaturalCubic(extractValues(points, PosField.X), xCubics);
        calcNaturalCubic(extractValues(points, PosField.Y), yCubics);
    }

    public Point2D.Double getPoint(float position) {
        position = position * xCubics.size();
        int cubicNum = (int) Math.min(xCubics.size() - 1, position);
        float cubicPos = (position - cubicNum);

        return new Point2D.Double(xCubics.get(cubicNum).eval(cubicPos),
            yCubics.get(cubicNum).eval(cubicPos));
    }

    public void calcNaturalCubic(List<Double> values, Collection<Cubic> cubics) {
        int num = values.size() - 1;

        double[] gamma = new double[num + 1];
        double[] delta = new double[num + 1];
        double[] D = new double[num + 1];

        int i;
		/*
               We solve the equation
	          [2 1       ] [D[0]]   [3(x[1] - x[0])  ]
	          |1 4 1     | |D[1]|   |3(x[2] - x[0])  |
	          |  1 4 1   | | .  | = |      .         |
	          |    ..... | | .  |   |      .         |
	          |     1 4 1| | .  |   |3(x[n] - x[n-2])|
	          [       1 2] [D[n]]   [3(x[n] - x[n-1])]

	          by using row operations to convert the matrix to upper triangular
	          and then back substitution.  The D[i] are the derivatives at the knots.
		 */
        gamma[0] = 1.0f / 2.0f;
        for(i = 1; i < num; i++) {
            gamma[i] = 1.0f / (4.0f - gamma[i - 1]);
        }
        gamma[num] = 1.0f / (2.0f - gamma[num - 1]);

        double p0 = values.get(0);
        double p1 = values.get(1);

        delta[0] = 3.0f * (p1 - p0) * gamma[0];
        for(i = 1; i < num; i++) {
            p0 = values.get(i - 1);
            p1 = values.get(i + 1);
            delta[i] = (3.0f * (p1 - p0) - delta[i - 1]) * gamma[i];
        }

        p0 = values.get(num - 1);
        p1 = values.get(num);

        delta[num] = (3.0f * (p1 - p0) - delta[num - 1]) * gamma[num];

        D[num] = delta[num];
        for(i = num - 1; i >= 0; i--) {
            D[i] = delta[i] - gamma[i] * D[i + 1];
        }

        //now compute the coefficients of the cubics
        cubics.clear();

        for(i = 0; i < num; i++) {
            p0 = values.get(i);
            p1 = values.get(i + 1);

            cubics.add(new Cubic(
                    p0,
                    D[i],
                    3 * (p1 - p0) - 2 * D[i] - D[i + 1],
                    2 * (p0 - p1) + D[i] + D[i + 1]
                )
            );
        }
    }
}
