package io.github.greennlab.hwptemplate;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

@Getter
@Slf4j
public class HwpTemplate<T> {

    public static final String FORM_BEGIN = ofNullable(System.getenv("hwp.template.begin")).orElse("<폼>");
    public static final String FORM_CLOSE = ofNullable(System.getenv("hwp.template.close")).orElse("</폼>");
    public static final int INTERPOLATION_START_CHAR = '{'; // ascii code 123
    public static final int INTERPOLATION_CLOSE_CHAR = '}'; // ascii code 125
    public static final int INTERPOLATION_CHARS_SIZE = 4;

    private static final String EMPTY_SPEAKER = "공백처리";

    private final List<HwpTemplateParagraph> templateParagraphs = new ArrayList<>();

    private Section section;
    private int startLine = 0;
    private int closeLine;

    public HwpTemplate(HWPFile hwp) {
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

    public void write(List<T> segments, Function<HwpTemplateData<T>, Function<Paragraph, Paragraph>> decorator) throws IOException {
        final AtomicInteger line = new AtomicInteger(startLine - 1);
        final AtomicInteger index = new AtomicInteger(-1);

        segments.stream().map(HwpTemplateData::wrap)
                .forEach(data -> {
                    data.setIndex(index.incrementAndGet());

                    for (HwpTemplateParagraph templateParagraph : templateParagraphs) {
                        section.insertParagraph(
                                line.incrementAndGet(),
                                decorator.apply(data).apply(templateParagraph.interpolate(data))
                        );
                    }
                });

        cleanUpLeftOverTemplates(line.intValue());
    }

    public void write(List<T> segments) throws IOException {
        write(segments, s -> p -> p);
    }

//    public void writeX(List<T> segments) throws IOException {
//        int line = startLine;
//        int index = 1;
//        for (T segment : segments) {
//            segment.setIndex(index++);
//
//            for (HwpTemplateParagraph templateParagraph : templateParagraphs) {
//                final Paragraph paragraph = templateParagraph.interpolate(segment);
//                section.insertParagraph(line++, paragraph);
//            }
//            section.insertParagraph(line++, paragraph);
//
//            // "공백처리" 항목은 내용만 표출
//            if (EMPTY_SPEAKER.equals(segment.getName())) {
//                for (HwpTemplateParagraph templateParagraph : templateParagraphs) {
//
//                    for (HwpTemplateParagraph.Interpolation interpolation : templateParagraph.getInterpolations()) {
//                        if (HwpTemplateItem.text == interpolation.getItem()) {
//                            final Paragraph interpolated = templateParagraph.interpolate(segment);
//                            final ParaCharShape charShape = interpolated.getCharShape();
//                            charShape.getPositonShapeIdPairList().clear();
//                            charShape.addParaCharShape(0, templateParagraph.getItemTextShapeId());
//                            final ParaText text = interpolated.getText();
//                            text.getCharList().clear();
//                            text.addString(segment.getText());
//
//                            section.insertParagraph(line++, interpolated);
//
//                            final Paragraph emptyLine = interpolated.clone();
//                            emptyLine.deleteText();
//                            section.insertParagraph(line++, emptyLine);
//                        }
//                    }
//                }
//            } else {
//                for (HwpTemplateParagraph<T> templateParagraph : templateParagraphs) {
//                    final Paragraph paragraph = templateParagraph.interpolate(segment);
//                    section.insertParagraph(line++, paragraph);
//                }
//            }
//        }
//
//        cleanUpLeftOverTemplates(line);
//    }

    private void cleanUpLeftOverTemplates(int line) {
        int limit = line + (closeLine - startLine);

        try {
            for (int i = limit; i >= line; i--) {
                section.deleteParagraph(i);
            }
        } catch (IndexOutOfBoundsException e) {
            log.warn("clean up left over templates. {}", e.getMessage());
        }
    }

}
