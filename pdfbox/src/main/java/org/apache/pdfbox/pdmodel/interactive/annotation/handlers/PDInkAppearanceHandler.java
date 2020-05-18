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
 *
 */
public class PDInkAppearanceHandler extends PDAbstractAppearanceHandler
{
    private static final Log LOG = LogFactory.getLog(PDInkAppearanceHandler.class);

    public PDInkAppearanceHandler(PDAnnotation annotation)
    {
        super(annotation);
    }

    public PDInkAppearanceHandler(PDAnnotation annotation, PDDocument document)
    {
        super(annotation, document);
    }

    @Override
    public void generateAppearanceStreams()
    {
        generateNormalAppearance();
        generateRolloverAppearance();
        generateDownAppearance();
    }

    @Override
    public void generateNormalAppearance()
    {
        PDAnnotationMarkup ink = (PDAnnotationMarkup) getAnnotation();
        // PDF spec does not mention /Border for ink annotations, but it is used if /BS is not available
        AnnotationBorder ab = AnnotationBorder.getAnnotationBorder(ink, ink.getBorderStyle());
        PDColor color = ink.getColor();
        if (color == null || color.getComponents().length == 0 || Float.compare(ab.width, 0) == 0)
        {
            return;
        }

        PDAppearanceContentStream cs = null;

        try
        {
            cs = getNormalAppearanceAsContentStream();

            setOpacity(cs, ink.getConstantOpacity());

            cs.setStrokingColor(color);
            if (ab.dashArray != null)
            {
                cs.setLineDashPattern(ab.dashArray, 0);
            }
            cs.setLineWidth(ab.width);

            for (float[] pathArray : ink.getInkList())
            {

                int nPoints = pathArray.length / 2;
                Point2D[] pathPoints = new Point2D.Double[nPoints];

                // "When drawn, the points shall be connected by straight lines or curves
                // in an implementation-dependent way" - we do lines.
                for (int i = 0; i < nPoints; ++i)
                {
                    int index = i * 2;

                    float x1 = pathArray[index];
                    float y1 = pathArray[index + 1];

                    pathPoints[i] = new Point2D.Double(x1, y1);

                }
                if (pathPoints.length > 0) {
                    BezierCurve bezierCurve = new BezierCurve(pathPoints);
                    Point2D firstPathPoint = pathPoints[0];
                    cs.moveTo((float) firstPathPoint.getX(), (float) firstPathPoint.getY());

                    if (pathPoints.length > 1) {
                        Point2D secondPathPoint = pathPoints[1];
                        Point2D lastPathPoint = pathPoints[pathPoints.length - 1];
                        Point2D firstControlPoint = bezierCurve.getPoint(0);
                        Point2D lastControlPoint = bezierCurve.getPoint(bezierCurve.getPoints().length - 1);

                        cs.curveTo2((float) firstControlPoint.getX(), (float) firstControlPoint.getY(), (float) secondPathPoint.getX(), (float) secondPathPoint.getY());

                        for (int i = 2; i < pathPoints.length - 1; i++) {
                            Point2D currentPathPoint = pathPoints[i];
                            Point2D b0 = bezierCurve.getPoint(2 * i - 3);
                            Point2D b1 = bezierCurve.getPoint(2 * i - 2);
                            cs.curveTo((float) b0.getX(), (float) b0.getY(), (float) b1.getX(), (float) b1.getY(), (float) currentPathPoint.getX(), (float) currentPathPoint.getY());
                        }

                        cs.curveTo2((float) lastControlPoint.getX(), (float) lastControlPoint.getY(), (float) lastPathPoint.getX(), (float) lastPathPoint.getY());
                    }
                }
                cs.stroke();
            }
        }
        catch (IOException ex)
        {
            LOG.error(ex);
        }
        finally
        {
            IOUtils.closeQuietly(cs);
        }
    }

    @Override
    public void generateRolloverAppearance()
    {
        // No rollover appearance generated
    }

    @Override
    public void generateDownAppearance()
    {
        // No down appearance generated
    }
}