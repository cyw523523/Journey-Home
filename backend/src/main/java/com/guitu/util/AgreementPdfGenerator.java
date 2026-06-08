package com.guitu.util;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class AgreementPdfGenerator {
    private static final int PAGE_WIDTH_PX = 1240;
    private static final int PAGE_HEIGHT_PX = 1754;
    private static final float PAGE_WIDTH_PT = 595f;
    private static final float PAGE_HEIGHT_PT = 842f;
    private static final int MARGIN_X = 96;
    private static final int MARGIN_TOP = 112;
    private static final int MARGIN_BOTTOM = 96;
    private static final int TITLE_SIZE = 34;
    private static final int BODY_SIZE = 22;
    private static final int LINE_GAP = 12;

    private AgreementPdfGenerator() {
    }

    public static byte[] generate(String title, String content) {
        try {
            List<BufferedImage> pages = renderPages(title, content);
            return buildPdfFromImages(pages);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate agreement PDF", ex);
        }
    }

    private static List<BufferedImage> renderPages(String title, String content) {
        Font titleFont = pickFont(Font.BOLD, TITLE_SIZE);
        Font bodyFont = pickFont(Font.PLAIN, BODY_SIZE);
        Font bodyBoldFont = pickFont(Font.BOLD, BODY_SIZE);

        BufferedImage measureCanvas = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D measureGraphics = measureCanvas.createGraphics();
        FontMetrics titleMetrics = measureGraphics.getFontMetrics(titleFont);
        FontMetrics bodyMetrics = measureGraphics.getFontMetrics(bodyFont);
        FontMetrics bodyBoldMetrics = measureGraphics.getFontMetrics(bodyBoldFont);

        List<TextLine> lines = new ArrayList<>();
        lines.add(new TextLine(title, true));
        lines.add(new TextLine("", false));
        for (String rawParagraph : content.replace("\r\n", "\n").split("\n")) {
            if (rawParagraph.isBlank()) {
                lines.add(new TextLine("", false));
                continue;
            }
            boolean heading = rawParagraph.matches("^\\d+\\..*") || rawParagraph.startsWith("协议编号") || rawParagraph.startsWith("签署时间");
            lines.addAll(wrap(rawParagraph, heading ? bodyBoldMetrics : bodyMetrics, PAGE_WIDTH_PX - (MARGIN_X * 2), heading));
        }
        measureGraphics.dispose();

        List<BufferedImage> pages = new ArrayList<>();
        BufferedImage page = createPageCanvas();
        Graphics2D graphics = initGraphics(page);
        int y = MARGIN_TOP;

        for (TextLine line : lines) {
            FontMetrics metrics = line.bold ? bodyBoldMetrics : bodyMetrics;
            int lineHeight = metrics.getHeight() + LINE_GAP;
            if (line.bold && line.text.equals(title)) {
                metrics = titleMetrics;
                lineHeight = metrics.getHeight() + 20;
            }
            if (y + lineHeight > PAGE_HEIGHT_PX - MARGIN_BOTTOM) {
                graphics.dispose();
                pages.add(page);
                page = createPageCanvas();
                graphics = initGraphics(page);
                y = MARGIN_TOP;
            }
            if (line.text.isBlank()) {
                y += Math.max(lineHeight / 2, 18);
                continue;
            }
            Font fontToUse = line.text.equals(title) ? titleFont : (line.bold ? bodyBoldFont : bodyFont);
            graphics.setFont(fontToUse);
            graphics.drawString(line.text, MARGIN_X, y + metrics.getAscent());
            y += lineHeight;
        }

        graphics.dispose();
        pages.add(page);
        return pages;
    }

    private static List<TextLine> wrap(String text, FontMetrics metrics, int maxWidth, boolean bold) {
        List<TextLine> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (char ch : text.toCharArray()) {
            current.append(ch);
            if (metrics.stringWidth(current.toString()) > maxWidth) {
                current.deleteCharAt(current.length() - 1);
                if (current.isEmpty()) {
                    result.add(new TextLine(String.valueOf(ch), bold));
                } else {
                    result.add(new TextLine(current.toString(), bold));
                    current.setLength(0);
                    current.append(ch);
                }
            }
        }
        if (!current.isEmpty()) {
            result.add(new TextLine(current.toString(), bold));
        }
        return result;
    }

    private static BufferedImage createPageCanvas() {
        BufferedImage page = new BufferedImage(PAGE_WIDTH_PX, PAGE_HEIGHT_PX, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = page.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, PAGE_WIDTH_PX, PAGE_HEIGHT_PX);
        graphics.dispose();
        return page;
    }

    private static Graphics2D initGraphics(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(32, 37, 41));
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return graphics;
    }

    private static Font pickFont(int style, int size) {
        String[] candidates = {"Microsoft YaHei", "SimSun", "PingFang SC", "Noto Sans CJK SC", "SansSerif"};
        for (String candidate : candidates) {
            Font font = new Font(candidate, style, size);
            if (font.canDisplay('领') && font.canDisplay('养')) {
                return font;
            }
        }
        return new Font("SansSerif", style, size);
    }

    private static byte[] buildPdfFromImages(List<BufferedImage> pages) throws IOException {
        List<byte[]> imageBytes = new ArrayList<>();
        for (BufferedImage page : pages) {
            ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
            ImageIO.write(page, "jpg", imageOut);
            imageBytes.add(imageOut.toByteArray());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        write(out, "%PDF-1.4\n");

        int pageCount = imageBytes.size();
        int firstImageObject = 3;
        int firstContentObject = firstImageObject + pageCount;
        int firstPageObject = firstContentObject + pageCount;
        int catalogObject = 1;
        int pagesObject = 2;
        int objectCount = firstPageObject + pageCount - 1;

        offsets.add(out.size());
        writeObject(out, catalogObject, "<< /Type /Catalog /Pages 2 0 R >>");

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            kids.append(firstPageObject + i).append(" 0 R ");
        }
        offsets.add(out.size());
        writeObject(out, pagesObject, "<< /Type /Pages /Count " + pageCount + " /Kids [" + kids + "] >>");

        for (int i = 0; i < pageCount; i++) {
            offsets.add(out.size());
            writeStreamObject(out, firstImageObject + i,
                    "<< /Type /XObject /Subtype /Image /Width " + PAGE_WIDTH_PX + " /Height " + PAGE_HEIGHT_PX +
                            " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length " + imageBytes.get(i).length + " >>",
                    imageBytes.get(i));
        }

        for (int i = 0; i < pageCount; i++) {
            String content = "q " + PAGE_WIDTH_PT + " 0 0 " + PAGE_HEIGHT_PT + " 0 0 cm /Im" + (i + 1) + " Do Q";
            byte[] contentBytes = content.getBytes(StandardCharsets.US_ASCII);
            offsets.add(out.size());
            writeStreamObject(out, firstContentObject + i,
                    "<< /Length " + contentBytes.length + " >>",
                    contentBytes);
        }

        for (int i = 0; i < pageCount; i++) {
            offsets.add(out.size());
            String pageObject = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH_PT + " " + PAGE_HEIGHT_PT +
                    "] /Resources << /XObject << /Im" + (i + 1) + " " + (firstImageObject + i) + " 0 R >> /ProcSet [/PDF /ImageC] >> " +
                    "/Contents " + (firstContentObject + i) + " 0 R >>";
            writeObject(out, firstPageObject + i, pageObject);
        }

        int xrefOffset = out.size();
        write(out, "xref\n");
        write(out, "0 " + (objectCount + 1) + "\n");
        write(out, "0000000000 65535 f \n");
        for (int i = 1; i <= objectCount; i++) {
            write(out, String.format("%010d 00000 n %n", offsets.get(i)));
        }
        write(out, "trailer\n");
        write(out, "<< /Size " + (objectCount + 1) + " /Root 1 0 R >>\n");
        write(out, "startxref\n");
        write(out, xrefOffset + "\n");
        write(out, "%%EOF");
        return out.toByteArray();
    }

    private static void writeObject(ByteArrayOutputStream out, int objectId, String body) throws IOException {
        write(out, objectId + " 0 obj\n");
        write(out, body + "\n");
        write(out, "endobj\n");
    }

    private static void writeStreamObject(ByteArrayOutputStream out, int objectId, String header, byte[] streamBytes) throws IOException {
        write(out, objectId + " 0 obj\n");
        write(out, header + "\n");
        write(out, "stream\n");
        out.write(streamBytes);
        write(out, "\nendstream\n");
        write(out, "endobj\n");
    }

    private static void write(ByteArrayOutputStream out, String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.US_ASCII));
    }

    private record TextLine(String text, boolean bold) {
    }
}
