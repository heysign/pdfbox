/*
 * Copyright 2018 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pdfbox.pdmodel.interactive.annotation.handlers;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDAppearanceContentStream;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Handler to generate the ink annotations appearance.
 */
public class PDInkAppearanceHandler extends PDAbstractAppearanceHandler {
    private static final Log LOG = LogFactory.getLog(PDInkAppearanceHandler.class);

    public PDInkAppearanceHandler(PDAnnotation annotation) {
        super(annotation);
    }

    public PDInkAppearanceHandler(PDAnnotation annotation, PDDocument document) {
        super(annotation, document);
    }

    @Override
    public void generateAppearanceStreams() {
        generateNormalAppearance();
        generateRolloverAppearance();
        generateDownAppearance();
    }

    @Override
    public void generateNormalAppearance() {
        PDAnnotationMarkup ink = (PDAnnotationMarkup) getAnnotation();
        // PDF spec does not mention /Border for ink annotations, but it is used if /BS is not available
        AnnotationBorder ab = AnnotationBorder.getAnnotationBorder(ink, ink.getBorderStyle());
        PDColor color = ink.getColor();
        if (color == null || color.getComponents().length == 0 || Float.compare(ab.width, 0) == 0) {
            return;
        }

        PDAppearanceContentStream cs = null;

        try {
            cs = getNormalAppearanceAsContentStream();

            setOpacity(cs, ink.getConstantOpacity());

            cs.setStrokingColor(color);
            if (ab.dashArray != null) {
                cs.setLineDashPattern(ab.dashArray, 0);
            }
            cs.setLineWidth(ab.width);

            List<Point2D.Double> pointList = new ArrayList<Point2D.Double>();
            List<CubicSpline> splineList  = new ArrayList<CubicSpline>();
            int currentSplineIndex = -1;
            for (float[] pathArray : ink.getInkList()) {
                int nPoints = pathArray.length / 2;
                // "When drawn, the points shall be connected by straight lines or curves
                // in an implementation-dependent way" - we do lines.
                for (int i = 0; i < nPoints; ++i) {
                    float x = pathArray[i * 2];
                    float y = pathArray[i * 2 + 1];

                    if (i == 0)
                    {
                        CubicSpline spline = new CubicSpline();
                        splineList.add(spline);
                        currentSplineIndex++;
                    }
                    Point2D.Double point = new Point2D.Double(x, y);
                    splineList.get(currentSplineIndex).addPoint(point);
                }

                for (CubicSpline spline : splineList){
                    spline.calcSpline();
                    Point2D.Double previousPoint1 = null;
                    Point2D.Double previousPoint2 = null;
                    if (spline.getPointsSize() > 1) {
                        for (float f = 0; f <= 1; f += 0.01) {
                            Point2D.Double p = spline.getPoint(f);
                            if (f > 0) {
                                if (previousPoint1 != null && previousPoint2 != null) {
                                    double originalDiffX1 = p.getX() - previousPoint1.getX();
                                    double originalDiffY1 = p.getY() - previousPoint1.getY();
                                    double originalDiffX2 =
                                        previousPoint1.getX() - previousPoint2.getX();
                                    double originalDiffY2 =
                                        previousPoint1.getY() - previousPoint2.getY();
                                    double diffX1 = originalDiffX1;
                                    double diffY1 = originalDiffY1;
                                    if (diffX1 < 0) {
                                        diffX1 *= -1;
                                    }
                                    if (diffY1 < 0) {
                                        diffY1 *= -1;
                                    }
                                    if (diffX1 > 5 && diffY1 > 5) {
                                        Point2D.Double tempPoint = new Point2D.Double(0, 0);
                                        double nextOffsetX = originalDiffX1 / 5.0;
                                        double nextOffsetY = originalDiffY1 / 5.0;
                                        if (originalDiffX1 < 0 && originalDiffX2 > 0) {
                                            tempPoint.x = previousPoint1.getX() + nextOffsetX;
                                        } else if (originalDiffX1 > 0 && originalDiffX2 < 0) {
                                            tempPoint.x = previousPoint1.getX() + nextOffsetX;
                                        } else if (originalDiffX1 > 0 && originalDiffX2 > 0) {
                                            tempPoint.x = previousPoint1.getX() + nextOffsetX;
                                        } else if (originalDiffX1 < 0 && originalDiffX2 < 0) {
                                            tempPoint.x = previousPoint1.getX() + nextOffsetX;
                                        }

                                        if (originalDiffY1 < 0 && originalDiffY2 > 0) {
                                            tempPoint.y = previousPoint1.getY() + nextOffsetY;
                                        } else if (originalDiffY1 > 0 && originalDiffY2 < 0) {
                                            tempPoint.y = previousPoint1.getY() + nextOffsetY;
                                        } else if (originalDiffY1 > 0 && originalDiffY2 > 0) {
                                            tempPoint.y = previousPoint1.getY() + nextOffsetY;
                                        } else if (originalDiffY1 < 0 && originalDiffY2 < 0) {
                                            tempPoint.y = previousPoint1.getY() + nextOffsetY;
                                        }
                                        if (tempPoint.getX() > 0) {
                                            cs.lineTo((float) tempPoint.getX(),
                                                (float) tempPoint.getY());
                                            LOG.debug(f + ":lineTo:" + tempPoint.toString());
                                        }
                                    }
                                }
                                cs.lineTo((float) p.getX(), (float) p.getY());
                                LOG.debug(f + ":lineTo:" + p.toString());
                            } else {
                                cs.moveTo((float) p.getX(), (float) p.getY());
                                LOG.debug("moveTo:" + p.toString());
                            }
                            if (previousPoint1 != null) {
                                previousPoint2 = previousPoint1;
                            }
                            previousPoint1 = p;
                        }
                    } else if (spline.getPointsSize() == 1) {
                        Point2D.Double p = spline.getOriginalPoint(0);
                        cs.lineTo((float) p.getX(), (float) p.getY());
                    }
                    cs.stroke();
                }
            }
        } catch (IOException ex) {
            LOG.error(ex);
        } finally {
            IOUtils.closeQuietly(cs);
        }
    }

    @Override
    public void generateRolloverAppearance() {
        // No rollover appearance generated
    }

    @Override
    public void generateDownAppearance() {
        // No down appearance generated
    }
}