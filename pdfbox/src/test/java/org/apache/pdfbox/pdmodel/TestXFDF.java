/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.pdmodel;

import static java.lang.Float.parseFloat;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.jbig2.util.log.Logger;
import org.apache.pdfbox.jbig2.util.log.LoggerFactory;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.fdf.FDFAnnotation;
import org.apache.pdfbox.pdmodel.fdf.FDFDocument;
import org.apache.pdfbox.pdmodel.fdf.FDFField;
import org.apache.pdfbox.pdmodel.font.FontMapper;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationRubberStamp;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceEntry;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This will test the FDF algorithms in PDFBox.
 *
 * @author Ben Litchfield
 * @author Tilman Hausherr
 * 
 */
public class TestXFDF
{

    private static final Logger log = LoggerFactory.getLogger(TestXFDF.class);
    private static final String BASE_DIR = "./";

    public static final String PDF_FIELD_NAME_PREFIX_SIGNATURE = "signature";
    public static final String PDF_FIELD_NAME_PREFIX_CHECK = "check";
    private static final String DEFAULT_FONT_SIZE = "15";
    private static final String DEFAULT_FONT_NAME = "NanumGothic";
    private static final String PROP_NAME_FFIELD = "ffield";
    private static final String PROP_NAME_WIDGET = "widget";
    private static final String PROP_NAME_RECTANGLE = "rect";
    private static final String PROP_NAME_BORDER = "border";
    private static final String PROP_NAME_FONT = "font";
    private static final String PROP_NAME_BACKGROUND_COLOR = "background-color";
    private static final String PROP_NAME_APPEARANCES = "appearances";
    private static final String PROP_NAME_APPEARANCE = "aappearance";
    private static final String PROP_NAME_COLOR = "color";
    private static final String ATTRIBUTE_NAME_RED = "r";
    private static final String ATTRIBUTE_NAME_GREEN = "g";
    private static final String ATTRIBUTE_NAME_BLUE = "b";
    private static final String ATTRIBUTE_NAME_ALPHA = "a";
    private static final String ATTRIBUTE_NAME_NAME = "name";
    private static final String ATTRIBUTE_NAME_APPEARANCE = "appearance";
    private static final String ATTRIBUTE_NAME_FIELD = "field";
    private static final String ATTRIBUTE_NAME_PAGE = "page";
    private static final String ATTRIBUTE_NAME_SIZE = "size";
    private static final String ATTRIBUTE_NAME_TYPE = "type";
    private static final String ATTRIBUTE_NAME_WIDTH = "width";
    private static final String ATTRIBUTE_NAME_STYLE = "style";
    private static final String FFIELD_TYPE_TEXT = "Tx";
    private static final String FFIELD_TYPE_BUTTON = "Btn";
    private static final String FFIELD_TYPE_SIGNATURE = "Sig";
    private static final String PDF_CHECKBOX_MARK_CROSS = "8";
    private static final String PDF_CHECKBOX_MARK_CHECK = "4";
    private static final String VALUE_NULL = "null";
    private static final int IMAGE_DPI = 30;
    private static final String IMAGE_FORMAT = "JPG";

    private static PDAppearanceCharacteristicsDictionary getAppearanceCharacteristics(
        PDAnnotationWidget widget) {
        PDAppearanceCharacteristicsDictionary appearanceCharacteristics = widget
            .getAppearanceCharacteristics();
        if (appearanceCharacteristics == null) {
            appearanceCharacteristics = new PDAppearanceCharacteristicsDictionary(new COSDictionary());
            widget.setAppearanceCharacteristics(appearanceCharacteristics);
        }
        return appearanceCharacteristics;
    }

    private static String getNodeAttributeValue(Node node, String attributeName,
        String defaultValue) {
        Node attributeNode = node.getAttributes().getNamedItem(attributeName);
        if (attributeNode != null) {
            return attributeNode.getNodeValue();
        } else {
            return defaultValue;
        }
    }

    private static PDPage getPage(PDDocument document, Integer pageNumber) {
        PDPage page = document.getPage(pageNumber - 1);
        return page;
    }

    private static void parseFFields(PDAcroForm form, NodeList ffieldNodeList) throws IOException {

        for (int i = 0; i < ffieldNodeList.getLength(); i++) {
            Node ffieldNode = ffieldNodeList.item(i);
            String type = getNodeAttributeValue(ffieldNode, ATTRIBUTE_NAME_TYPE, null);
            String name = getNodeAttributeValue(ffieldNode, ATTRIBUTE_NAME_NAME, null);
            log.debug(type + ":" + name);
            PDField currentField = null;
            if (FFIELD_TYPE_TEXT.equalsIgnoreCase(type)) {
                PDTextField textField = new PDTextField(form);
                textField.setPartialName(name);
                currentField = textField;
                form.getFields().add(textField);
            } else if (FFIELD_TYPE_BUTTON.equalsIgnoreCase(type)) {
                PDCheckBox checkBoxField = new PDCheckBox(form);
                checkBoxField.setPartialName(name);
                currentField = checkBoxField;
                form.getFields().add(checkBoxField);
            } else if (FFIELD_TYPE_SIGNATURE.equalsIgnoreCase(type)) {
                PDSignatureField signatureField = new PDSignatureField(form);
                signatureField.setPartialName(name);
                currentField = signatureField;
                form.getFields().add(signatureField);
            }
            for (int j = 0; j < ffieldNode.getChildNodes().getLength(); j++) {
                Node ffieldChild = ffieldNode.getChildNodes().item(j);
                if (PROP_NAME_FONT.equalsIgnoreCase(ffieldChild.getNodeName())) {
                    String fontSize = getNodeAttributeValue(ffieldChild, ATTRIBUTE_NAME_SIZE,
                        DEFAULT_FONT_SIZE);
                    String fontName = getNodeAttributeValue(ffieldChild, ATTRIBUTE_NAME_NAME,
                        DEFAULT_FONT_NAME);
//                    currentField.getCOSObject().setString(COSName.DA, "/" +fontName +" " + fontSize + " Tf 0 g");
                }
            }
        }
    }


    private static float getLineWidth(PDAnnotationWidget widget) {
        PDBorderStyleDictionary bs = widget.getBorderStyle();
        if (bs != null) {
            return bs.getWidth();
        }
        return 0;
    }

    private static PDColor createColor(PDDocument document, PDPage page, String rValue, String gValue,
        String bValue, String aValue) throws IOException {
        float r = -1f;
        float g = -1f;
        float b = -1f;
        float a = -1f;

        if (rValue != null && !rValue.isEmpty()) {
            r = parseFloat(rValue);
        }
        if (gValue != null && !gValue.isEmpty()) {
            g = parseFloat(gValue);
        }
        if (bValue != null && !bValue.isEmpty()) {
            b = parseFloat(bValue);
        }
        if (aValue != null && !aValue.isEmpty()) {
            a = parseFloat(aValue);
        }
        return createColor(document, page, r, g, b, a);
    }

    private static PDColor createColor(PDDocument document, PDPage page, float r, float g, float b,
        float a) throws IOException {
        PDColor color = null;
        if (r > -1 || g > -1 || b > -1) {
            float[] rgb = new float[]{(r == -1 ? 0 : r),
                (g == -1 ? 0 : g),
                (b == -1 ? 0 : b)};
            color = new PDColor(rgb, PDDeviceRGB.INSTANCE);
        }
        return color;
    }

    private static PDAppearanceStream createAppearanceStream(
        final PDDocument document, PDAnnotationWidget widget, boolean on) throws IOException {
        PDRectangle rect = widget.getRectangle();
        PDAppearanceStream yesAP = new PDAppearanceStream(document);
        yesAP.setBBox(new PDRectangle(rect.getWidth(), rect.getHeight()));
        yesAP.setResources(new PDResources());
        PDPageContentStream yesAPCS = new PDPageContentStream(document, yesAP);
        PDAppearanceCharacteristicsDictionary appearanceCharacteristics = getAppearanceCharacteristics(
            widget);
        PDColor backgroundColor = appearanceCharacteristics.getBackground();
        PDColor borderColor = appearanceCharacteristics.getBorderColour();
        float lineWidth = getLineWidth(widget);
        log.debug("createAppearanceStream:lineWidth:" + lineWidth);
        yesAPCS.setLineWidth(lineWidth); // border style (dash) ignored
        if (backgroundColor != null) {
            yesAPCS.setNonStrokingColor(backgroundColor);
        }
        yesAPCS.addRect(0, 0, rect.getWidth(), rect.getHeight());
        yesAPCS.fill();
        if (borderColor != null) {
            yesAPCS.setStrokingColor(borderColor);
        }
        if (lineWidth > 0) {
            yesAPCS.addRect(lineWidth / 2, lineWidth / 2, rect.getWidth() - lineWidth,
                rect.getHeight() - lineWidth);
            yesAPCS.stroke();
        }
        if (!on) {
            yesAPCS.close();
            return yesAP;
        }
        if (lineWidth > 0) {
            yesAPCS.addRect(lineWidth, lineWidth, rect.getWidth() - lineWidth * 2,
                rect.getHeight() - lineWidth * 2);
            yesAPCS.clip();
        }
        String normalCaption = appearanceCharacteristics.getNormalCaption();
        if (normalCaption == null) {
            normalCaption = PDF_CHECKBOX_MARK_CHECK; // Adobe behaviour
        }
        if (PDF_CHECKBOX_MARK_CROSS.equals(normalCaption)) {
            // Adobe paints a cross instead of using the Zapf Dingbats cross symbol
            yesAPCS.setStrokingColor(0f);
            yesAPCS.moveTo(lineWidth * 2, rect.getHeight() - lineWidth * 2);
            yesAPCS.lineTo(rect.getWidth() - lineWidth * 2, lineWidth * 2);
            yesAPCS.moveTo(rect.getWidth() - lineWidth * 2, rect.getHeight() - lineWidth * 2);
            yesAPCS.lineTo(lineWidth * 2, lineWidth * 2);
            yesAPCS.stroke();
        } else {
            // The caption is not unicode, but the Zapf Dingbats code in the PDF
            // Thus convert it back to unicode
            // Assume that only the first character is used.
            String name = PDType1Font.ZAPF_DINGBATS.codeToName(normalCaption.codePointAt(0));
            String unicode = PDType1Font.ZAPF_DINGBATS.getGlyphList().toUnicode(name);
            Rectangle2D bounds = PDType1Font.ZAPF_DINGBATS.getPath(name).getBounds2D();
            float size = (float) Math.min(bounds.getWidth(), bounds.getHeight()) / 1000;
            // assume that checkmark has square size
            // the calculations approximate what Adobe is doing, i.e. put the glyph in the middle
            float fontSize = (rect.getWidth() - lineWidth * 2) / size * 0.6666f;
            float xOffset = (float) (rect.getWidth() - (bounds.getWidth()) / 1000 * fontSize) / 2;
            xOffset -= bounds.getX() / 1000 * fontSize;
            float yOffset = (float) (rect.getHeight() - (bounds.getHeight()) / 1000 * fontSize) / 2;
            yOffset -= bounds.getY() / 1000 * fontSize;
            yesAPCS.setNonStrokingColor(0f);
            yesAPCS.beginText();
            yesAPCS.setFont(PDType1Font.ZAPF_DINGBATS, fontSize);
            yesAPCS.newLineAtOffset(xOffset, yOffset);
            yesAPCS.showText(unicode);
            yesAPCS.endText();
        }
        yesAPCS.close();
        return yesAP;
    }

    private static void parseWidgets(PDDocument document, PDAcroForm form, NodeList widgetNodeList,
        List<PDField> signatureFieldList) throws IOException {
        for (int i = 0; i < widgetNodeList.getLength(); i++) {
            Node widgetNode = widgetNodeList.item(i);
            String pageNumber = getNodeAttributeValue(widgetNode, ATTRIBUTE_NAME_PAGE, null);
            String name = getNodeAttributeValue(widgetNode, ATTRIBUTE_NAME_FIELD, null);
            String nameLowerCase = name.toLowerCase();
            String attributeAppearance = getNodeAttributeValue(widgetNode, ATTRIBUTE_NAME_APPEARANCE,
                null);
            for (PDField field : form.getFields()) {
                log.debug(field.toString());
            }
            PDField inputField = form.getField(name);
            if (inputField != null) {
                if (nameLowerCase.contains(PDF_FIELD_NAME_PREFIX_SIGNATURE)) {
                    signatureFieldList.add(inputField);
                }
                if (nameLowerCase.contains(PDF_FIELD_NAME_PREFIX_CHECK) && attributeAppearance != null) {
                    //  set default background color
                    PDAnnotationWidget widget = inputField.getWidgets().get(0);
                    PDAppearanceCharacteristicsDictionary fieldAppearance = getAppearanceCharacteristics(
                        widget);
                    fieldAppearance.setBackground(new PDColor(new float[]{1, 1, 1}, PDDeviceRGB.INSTANCE));
                    widget.setAppearanceCharacteristics(fieldAppearance);
                }
                PDAnnotationWidget widget = inputField.getWidgets().get(0);
                int pages = document.getNumberOfPages();
                PDColor borderColor = null;
                log.debug(name + ":" + pageNumber);
                PDPage page = getPage(document, Integer.parseInt(pageNumber));
                widget.setPage(page);
                for (int k = 0; k < widgetNode.getChildNodes().getLength(); k++) {
                    Node widgetChild = widgetNode.getChildNodes().item(k);
                    if (PROP_NAME_RECTANGLE.equalsIgnoreCase(widgetChild.getNodeName())) {
                        double x1 = Double.parseDouble(getNodeAttributeValue(widgetChild, "x1", "0"));
                        double x2 = Double.parseDouble(getNodeAttributeValue(widgetChild, "x2", "0"));
                        double y1 = Double.parseDouble(getNodeAttributeValue(widgetChild, "y1", "0"));
                        double y2 = Double.parseDouble(getNodeAttributeValue(widgetChild, "y2", "0"));
                        PDRectangle rectangle = new PDRectangle((float) x1, (float) y1, (float) (x2 - x1),
                            (float) (y2 - y1));
                        widget.setRectangle(rectangle);
                    } else if (PROP_NAME_BORDER.equalsIgnoreCase(widgetChild.getNodeName())) {
                        for (int p = 0; p < widgetChild.getChildNodes().getLength(); p++) {
                            Node borderChild = widgetChild.getChildNodes().item(p);
                            if (PROP_NAME_COLOR.equalsIgnoreCase(borderChild.getNodeName())) {
                                String r = getNodeAttributeValue(widgetChild, ATTRIBUTE_NAME_WIDTH, "255");
                                String g = getNodeAttributeValue(widgetChild, ATTRIBUTE_NAME_WIDTH, "255");
                                String b = getNodeAttributeValue(widgetChild, ATTRIBUTE_NAME_WIDTH, "255");
                                String a = getNodeAttributeValue(widgetChild, ATTRIBUTE_NAME_WIDTH, "1.0");
                                borderColor = createColor(document, page, r, g, b, a);
                            }
                        }
                        String width = getNodeAttributeValue(widgetChild, ATTRIBUTE_NAME_WIDTH, "0");
                        String style = getNodeAttributeValue(widgetChild, ATTRIBUTE_NAME_STYLE, "");
                        if (width != null && !width.isEmpty()) {
                            float tempValue = parseFloat(width);
                            if (tempValue > 0) {
                                PDBorderStyleDictionary borderStyleDictionary = new PDBorderStyleDictionary();
                                borderStyleDictionary.setWidth(tempValue);
                                if (!VALUE_NULL.equals(style)) {
                                    borderStyleDictionary.setStyle(style);
                                }
                                widget.setBorderStyle(borderStyleDictionary);
                                if (borderColor != null) {
                                    PDAppearanceCharacteristicsDictionary appearanceCharacteristics = getAppearanceCharacteristics(
                                        widget);
                                    appearanceCharacteristics.setBorderColour(borderColor);
                                }
                            }
                        }
//                        }
                    } else if (PROP_NAME_BACKGROUND_COLOR.equalsIgnoreCase(widgetChild.getNodeName())) {
                        String r = getNodeAttributeValue(widgetChild, ATTRIBUTE_NAME_RED, "255");
                        String g = getNodeAttributeValue(widgetChild, ATTRIBUTE_NAME_GREEN, "255");
                        String b = getNodeAttributeValue(widgetChild, ATTRIBUTE_NAME_BLUE, "255");
                        PDColor color = createColor(document, page, r, g, b, "1.0");
                        if (color != null) {
                            PDAppearanceCharacteristicsDictionary fieldAppearance = getAppearanceCharacteristics(
                                widget);
                            fieldAppearance.setBackground(color);
                        }
                    } else if (inputField instanceof PDCheckBox &&
                        PROP_NAME_APPEARANCES.equalsIgnoreCase(widgetChild.getNodeName())) {
                        PDCheckBox inputCheckBox = (PDCheckBox) inputField;
                        PDAppearanceDictionary apDictionary = widget.getAppearance();
                        if (apDictionary == null) {
                            apDictionary = new PDAppearanceDictionary();
                            widget.setAppearance(apDictionary);
                        }

                        // Set checkbox border to default width
//            PDBorderStyleDictionary borderStyleDictionary = new PDBorderStyleDictionary();
//            borderStyleDictionary.setWidth(DEFAULT_CHECKBOX_BORDER_WIDTH);
//            widget.setBorderStyle(borderStyleDictionary);

                        List<String> onValues = new ArrayList<String>();
                        for (int l = 0; l < widgetChild.getChildNodes().getLength(); l++) {
                            Node appearanceChild = widgetChild.getChildNodes().item(l);
                            if (PROP_NAME_APPEARANCE.equalsIgnoreCase(appearanceChild.getNodeName())) {
                                String appearanceName = getNodeAttributeValue(appearanceChild, ATTRIBUTE_NAME_NAME,
                                    null);
                                if (appearanceName != null && !appearanceName.isEmpty() && !COSName.OFF.getName()
                                    .equalsIgnoreCase(appearanceName)) {
                                    onValues.add(COSName.ON.getName());
                                }
                            }
                        }
                        if (!onValues.isEmpty()) {
                            inputCheckBox.setExportValues(onValues);
                            PDAppearanceCharacteristicsDictionary appearanceCharacteristics = getAppearanceCharacteristics(
                                widget);
                            // 8 = cross; 4 = checkmark; H = star; u = diamond; n = square, l = dot
                            appearanceCharacteristics.setNormalCaption(PDF_CHECKBOX_MARK_CROSS);
                            widget.setAppearanceCharacteristics(appearanceCharacteristics);

                            PDAppearanceEntry normalAppearance = apDictionary.getNormalAppearance();
                            COSDictionary normalAppearanceDict = (COSDictionary) normalAppearance.getCOSObject();
                            normalAppearanceDict
                                .setItem(COSName.Off, createAppearanceStream(document, widget, false));
                            normalAppearanceDict
                                .setItem(COSName.ON, createAppearanceStream(document, widget, true));

                        }
                    }
                }
//                widget.constructAppearances();
                widget.setPrinted(true);
                page.getAnnotations().add(widget);
            } else {
                log.warn(name + " field not found");
            }

        }
    }

    private static void parseFdfFields(PDAcroForm form, List<FDFField> fdfFields) throws IOException {
        if (fdfFields == null || fdfFields.isEmpty()) {
            return;
        }

        for (FDFField field : fdfFields) {
            Object value = field.getValue();
            String fieldName = field.getPartialFieldName();
            PDField inputField = form.getField(fieldName);
            if (inputField != null && value != null && !value.toString().isEmpty()) {
                if (inputField instanceof PDCheckBox) {
                    if (COSName.ON.getName().equalsIgnoreCase(value.toString()) ||
                        COSName.YES.getName().equalsIgnoreCase(value.toString())
                    ) {
                        ((PDCheckBox) inputField).check();
                    } else {
                        ((PDCheckBox) inputField).unCheck();
                    }
                } else if (inputField instanceof PDSignatureField) {
                    //inputField.setValue(value.toString());
                } else {
                    inputField.setValue(value.toString());
                }
            }
        }
    }

    private static void parseFdfAnnotations(PDDocument document, PDAcroForm form,
        List<FDFAnnotation> fdfAnnots, List<PDField> signatureFieldList) throws IOException {
        int currentSignatureIndex = -1;
        for (FDFAnnotation annot : fdfAnnots) {
            String subject = annot.getSubject();
            Integer pageNumber = annot.getPage();
//      PDBorderStyleDictionary borderStyle = annot.getBorderStyle();

            log.info("---"+pageNumber+"--"+document.getNumberOfPages()+"---"+annot.getContents());
            // FIXME : bug pdftron export
            PDPage page = document.getPage(pageNumber);
            //PDPage page = getPage(document, pageNumber);

//      COSDictionary pageCosObject = page.getCOSObject();
//      Color color = annot.getColor();
            String title = annot.getTitle();
            String name = annot.getName();
            log.debug(title);
            log.debug(name);
            COSDictionary dictionary = annot.getCOSObject().getCOSDictionary(COSName.AP);
            String subtype = annot.getCOSObject().getNameAsString(COSName.SUBTYPE);

           if (PDAnnotationMarkup.SUB_TYPE_INK.equals(subtype)) {
//        currentSignatureIndex++;
//        PDTerminalField inputField = (PDTerminalField) signatureFieldList
//            .get(currentSignatureIndex);
//        List<PDAnnotationWidget> widgets = inputField.getWidgets();
                PDAnnotationMarkup newAnnot = (PDAnnotationMarkup) PDAnnotation
                    .createAnnotation(annot.getCOSObject());
                newAnnot.setPage(page);
//        PDAnnotationWidget tempWidget = new PDAnnotationWidget(newAnnot.getCOSObject());
//        tempWidget.setPage(page);
//        widgets.add(tempWidget);
//        inputField.setWidgets(widgets);
//        page.getAnnotations().add(tempWidget);
                PDRectangle rect = newAnnot.getRectangle();
                if (rect != null) {
                    log.debug(rect.toString());
                    float incrementValue = 15.0f;

                    float x = rect.getLowerLeftX();
                    float y = rect.getLowerLeftY();
                    float width = rect.getUpperRightX();
                    float height = rect.getUpperRightY();
                    rect.setLowerLeftX(x - incrementValue);
                    rect.setLowerLeftY(y - incrementValue);
                    rect.setUpperRightX(width + (incrementValue * 2));
                    rect.setUpperRightY(height + (incrementValue * 2));
                    log.debug(rect.toString());
                    newAnnot.setRectangle(rect);
                }
                newAnnot.constructAppearances();
                page.getAnnotations().add(newAnnot);
            } else {
                PDAnnotation newAnnot = PDAnnotation.createAnnotation(annot.getCOSObject());
                COSArray border = new COSArray();
                border.add(COSInteger.ZERO);
                border.add(COSInteger.ZERO);
                border.add(COSInteger.ZERO);
                newAnnot.setBorder(border);
                newAnnot.constructAppearances();
                page.getAnnotations().add(newAnnot);
                newAnnot.setPage(page);
            }
        }
    }
    public static void loadXFDFAnnotationsAndFields(
        PDDocument document,
        PDAcroForm form, InputStream annotFile) throws IOException, URISyntaxException {

        org.w3c.dom.Document xmlDocument = org.apache.pdfbox.util.XMLUtil.parse(annotFile);
        FDFDocument fdfDoc = new FDFDocument(xmlDocument);
        List<FDFAnnotation> fdfAnnots = fdfDoc.getCatalog().getFDF().getAnnotations();
        List<FDFField> fdfFields = fdfDoc.getCatalog().getFDF().getFields();

        Element xfdfElement = xmlDocument.getDocumentElement();
        NodeList ffieldNodeList = xfdfElement.getElementsByTagName(PROP_NAME_FFIELD);
        NodeList widgetNodeList = xfdfElement.getElementsByTagName(PROP_NAME_WIDGET);

        List<PDField> signatureFieldList = new ArrayList<PDField>();
        parseFFields(form, ffieldNodeList);
        parseWidgets(document, form, widgetNodeList, signatureFieldList);
        parseFdfFields(form, fdfFields);
        parseFdfAnnotations(document, form, fdfAnnots, signatureFieldList);
    }

    @Test
    public void createPdfAnnotation() {
        try {
            String annotationAddedPdf = BASE_DIR + "annotationAdded.pdf";
            String flattenedPdf = BASE_DIR + "flattened.pdf";
//            String xfdf = "sample/acrobat_annot.xfdf";
//            String xfdf = "sample/express_annot.xfdf";
//            String xfdf = "sample/acroform.xfdf";
            InputStream xfdfIs = this.getClass().getClassLoader().getResourceAsStream("acroform2.xfdf");
//      String xfdf = "sample/acroform2.xfdf";
//      File xfdfFile = new File(xfdf);
            InputStream pdfIs = this.getClass().getClassLoader().getResourceAsStream("test.pdf");
            PDDocument document = PDDocument.load(pdfIs);

            String ttfName = "NanumGothic.ttf";
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(ttfName);
            PDFont font = PDType0Font.load(new PDDocument(), is, false);

            PDPageTree pageTree = document.getPages();
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                acroForm = new PDAcroForm(document);
                String defaultAppearanceString = "/" + font.getName() + " 20 Tf 0 g";
                acroForm.setDefaultAppearance(defaultAppearanceString);
                document.getDocumentCatalog().setAcroForm(acroForm);
            }

            PDResources dr = acroForm.getDefaultResources();
            if (dr == null) {
                dr = new PDResources();
                //dr.put(COSName.getPDFName("Helv"), PDType1Font.HELVETICA);
                dr.put(COSName.getPDFName("NanumGothic"), font);
                acroForm.setDefaultResources(dr);
            }

            loadXFDFAnnotationsAndFields(document, acroForm, xfdfIs);

            List<PDField> fieldList = acroForm.getFields();
            if (fieldList == null) {
                fieldList = new ArrayList<PDField>();
            }
            if (fieldList.isEmpty()) {
                PDSignatureField tempField = new PDSignatureField(acroForm);
                tempField.setPartialName("TempField");
                fieldList.add(tempField);
            }
            acroForm.setFields(fieldList);

//            for(PDPage page : pageTree) {
//                List<PDAnnotation> annotations = page.getAnnotations();
//                for (PDAnnotation annot : xfdfAnnotationList) {
//
//                    if (PDAnnotationRubberStamp.SUB_TYPE.equals(annot.getSubtype())) {
//                        PDAppearanceStream appearanceStream = annot.getNormalAppearanceStream();
//                        PDRectangle rect = appearanceStream.getBBox();
////                        log.debug(annot.getAnnotationName());
////                        log.debug(rect);
//                        rect.setLowerLeftX(0);
//                        rect.setLowerLeftY(0);
//                        appearanceStream.setBBox(rect);
//                        annot.constructAppearances();
//                    } else if (PDAnnotationMarkup.SUB_TYPE_INK.equals(annot.getSubtype())) {
//                        annot.constructAppearances();
//                    } else {
//                        COSArray border = new COSArray();
//                        border.add(COSInteger.ZERO);
//                        border.add(COSInteger.ZERO);
//                        border.add(COSInteger.ZERO);
//                        annot.setBorder(border);
//                        annot.constructAppearances();
//                    }
////                    log.debug(annot.getBorder());
////                    log.debug(annot.getAnnotationName());
////                    log.debug(annot.getSubtype());
////                    annotations.add(annot);
//                }
//                page.setAnnotations(annotations);
//
//            }

            document.save(annotationAddedPdf);
            document.getDocumentCatalog().getAcroForm().flatten();
            document.save(flattenedPdf);
            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
