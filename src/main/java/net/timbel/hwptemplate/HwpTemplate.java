package net.timbel.hwptemplate;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.writer.HWPWriter;
import lombok.Getter;
import lombok.extern.java.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Getter
@Log
public class HwpTemplate<T> {

    public static final String FORM_BEGIN = System.getProperty("hwp.template.begin", "<꼬락서니>");
    public static final String FORM_CLOSE = System.getProperty("hwp.template.close", "</꼬락서니>");
    public static final int INTERPOLATION_START_CHAR = System.getProperty("hwp.interpolate.begin", "{").charAt(0);
    public static final int INTERPOLATION_CLOSE_CHAR = System.getProperty("hwp.interpolate.close", "}").charAt(0);
    public static final int INTERPOLATION_CHARS_SIZE = 2;

    private final HWPFile hwp;
    private final List<HwpTemplateParagraph> templateParagraphs = new ArrayList<>();

    private Section section;
    private int startLine = 0;
    private int closeLine;

    public HwpTemplate(HWPFile hwp) {
        this.hwp = hwp;

        boolean formBegin = false;
        boolean formClose = false;

        try {
            for (Section formSection : hwp.getBodyText().getSectionList()) {
                final Paragraph[] paragraphs = formSection.getParagraphs();
                int line = 0;
                for (Paragraph paragraph : paragraphs) {
                    final String normalString = paragraph.getNormalString();

                    if (!formBegin && normalString.contains(FORM_BEGIN)) {
                        formBegin = true;
                        this.section = formSection;
                        this.startLine = line;
                    } else if (normalString.contains(FORM_CLOSE)) {
                        formClose = true;
                        this.closeLine = line;
                        break;
                    } else if (formBegin) {
                        this.templateParagraphs.add(new HwpTemplateParagraph(paragraph));
                    }

                    line++;
                }

                if (formBegin && formClose) break;
                else throw new IllegalArgumentException(FORM_BEGIN + " 으로 시작해서 " + FORM_CLOSE + " 으로 끝나는 양식 구성이 필요해요.");
            }

            if (this.section == null || templateParagraphs.isEmpty()) {
                throw new IllegalStateException("양식 구성이 없는 파일 이에요.");
            }
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public void toFile(String filepath) throws IOException {
        try {
            HWPWriter.toFile(hwp, filepath);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void toStream(OutputStream out) throws IOException {
        try {
            HWPWriter.toStream(hwp, out);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void write(List<T> data, Function<HwpTemplateData, Function<Paragraph, Paragraph>> decorator) {
        final AtomicInteger line = new AtomicInteger(startLine);
        final AtomicInteger index = new AtomicInteger(-1);

        data.stream().map(HwpTemplateData::wrap)
                .forEach(item -> {
                    item.setIndex(index.incrementAndGet());

                    for (HwpTemplateParagraph templateParagraph : templateParagraphs) {
                        section.insertParagraph(
                                line.getAndIncrement(),
                                decorator.apply(item).apply(templateParagraph.interpolate(item))
                        );
                    }
                });

        cleanUpLeftOverTemplates(line.intValue());
    }

    public void write(List<T> data) {
        write(data, s -> p -> p);
    }

    private void cleanUpLeftOverTemplates(int line) {
        int limit = line + (closeLine - startLine);

        try {
            for (int i = limit; i >= line; i--) {
                section.deleteParagraph(i);
            }
        } catch (IndexOutOfBoundsException e) {
            log.warning("clean up left over templates: " + e.getMessage());
        }
    }

}
