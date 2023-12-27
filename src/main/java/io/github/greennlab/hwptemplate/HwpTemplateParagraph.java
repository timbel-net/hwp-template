package io.github.greennlab.hwptemplate;

import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.charshape.CharPositionShapeIdPair;
import kr.dogfoot.hwplib.object.bodytext.paragraph.lineseg.LineSegItem;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.ParaText;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static io.github.greennlab.hwptemplate.HwpTemplate.INTERPOLATION_CHARS_SIZE;
import static io.github.greennlab.hwptemplate.HwpTemplate.INTERPOLATION_CLOSE_CHAR;
import static io.github.greennlab.hwptemplate.HwpTemplate.INTERPOLATION_START_CHAR;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

@Slf4j
public class HwpTemplateParagraph {

    @Getter
    private final List<Interpolation> interpolations;
    private final Paragraph originParagraph;


    public HwpTemplateParagraph(Paragraph originParagraph) {
        this.originParagraph = originParagraph;
        this.interpolations = Interpolation.builder(originParagraph);
    }

    public Paragraph interpolate(HwpTemplateData segment) {
        val paragraph = originParagraph.clone();
        if (paragraph.getText() == null) return paragraph;

        val text = paragraph.getText();
        for (LineSegItem lineSegItem : paragraph.getLineSeg().getLineSegItemList()) {
            lineSegItem.setSegmentWidth(lineSegItem.getSegmentWidth() + 1);
        }

        int index = 0;
        for (final Interpolation interpolation : interpolations) {
            index += interpolation.getIndex();

            try {
                val itemValue = interpolatedValue(segment, interpolation.getItem());
                val removeRange = interpolation.getItem().length() + INTERPOLATION_CHARS_SIZE; // 4 is interpolation characters "{{}}"
                text.getCharList().subList(index, index + removeRange).clear();
                text.insertString(index, itemValue);

                val gap = itemValue.length() - removeRange;
                resetCharShape(paragraph.getCharShape().getPositonShapeIdPairList(), index, gap);
                index += gap;
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }

        return paragraph;
    }

    public String interpolatedValue(HwpTemplateData<T> segment, String item) {
        try {
            val field = segment.getData().getClass().getField(item);
            val value = field.get(segment);
            return value == null ? "" : value.toString();
        } catch (IllegalAccessException | IllegalStateException | NoSuchFieldException e) {
            return "";
        }
    }

    @SuppressWarnings("java:S127")
    private void resetCharShape(ArrayList<CharPositionShapeIdPair> pairs, int start, int length) {
        for (CharPositionShapeIdPair pair : pairs) {
            long position = pair.getPosition();
            if (position > start) {
                pair.setPosition(position + length);
            }
        }
    }

    @Data
    public static class Interpolation {
        private final String name;
        private final int position;

        public static List<Interpolation> builder(Paragraph paragraph) {
            final ParaText text = paragraph.getText();
            if (text == null) return emptyList();

            final List<Interpolation> result = new ArrayList<>();
            final List<HWPChar> hwpChars = text.getCharList();

            int start = -1;

            for (int i = 0, l = hwpChars.size(); i < l - 1; i++) {
                final int code = hwpChars.get(i).getCode();
                final int nextCode = hwpChars.get(i + 1).getCode();
                if (INTERPOLATION_START_CHAR == code && INTERPOLATION_START_CHAR == nextCode) {
                    start = i + 2;
                } else if (start > -1 && INTERPOLATION_CLOSE_CHAR == code && INTERPOLATION_CLOSE_CHAR == nextCode) {
                    final String name = hwpChars.subList(start, i - 1).stream().map(hwpChar -> {
                        try {
                            return new HWPCharNormal(hwpChar.getCode()).getCh();
                        } catch (UnsupportedEncodingException e) {
                            throw new IllegalStateException(e);
                        }
                    }).collect(joining());

                    result.add(new Interpolation(name, start - 2));
                    start = -1;
                }
            }

            return result;
        }
    }

//    public long getItemTextShapeId() {
//        int index = 0;
//        for (Interpolation interpolation : interpolations) {
//            index += interpolation.getIndex();
//
//            if (HwpTemplate.HwpTemplateItem.text == interpolation.getItem()) {
//                for (CharPositionShapeIdPair pair : originParagraph.getCharShape().getPositonShapeIdPairList()) {
//                    if (index <= pair.getPosition())
//                        return pair.getShapeId();
//                }
//
//            }
//        }
//
//        return -1;
//    }

}
