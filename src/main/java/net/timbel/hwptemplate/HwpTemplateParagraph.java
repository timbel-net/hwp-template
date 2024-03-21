package net.timbel.hwptemplate;

import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.charshape.CharPositionShapeIdPair;
import kr.dogfoot.hwplib.object.bodytext.paragraph.lineseg.LineSegItem;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.ParaText;
import lombok.Data;
import lombok.Getter;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static net.timbel.hwptemplate.HwpTemplate.INTERPOLATION_CHARS_SIZE;
import static net.timbel.hwptemplate.HwpTemplate.INTERPOLATION_CLOSE_CHAR;
import static net.timbel.hwptemplate.HwpTemplate.INTERPOLATION_START_CHAR;

@Getter
public class HwpTemplateParagraph {

    private final List<Interpolation> interpolations;
    private final Paragraph originParagraph;


    public HwpTemplateParagraph(Paragraph originParagraph) {
        this.originParagraph = originParagraph;
        this.interpolations = Interpolation.builder(originParagraph);
    }

    public Paragraph interpolate(HwpTemplateData segment) {
        final Paragraph paragraph = this.originParagraph.clone();
        if (paragraph.getText() == null) return paragraph;

        final List<LineSegItem> lineSegItems = paragraph.getLineSeg().getLineSegItemList();
        for (LineSegItem lineSegItem : lineSegItems) {
            lineSegItem.setSegmentWidth(lineSegItem.getSegmentWidth() + 1);
        }

        int index = 0;
        for (final Interpolation interpolation : interpolations) {
            final ParaText text = paragraph.getText();
            index += interpolation.getIndex();

            try {
                final String itemValue = interpolatedValue(segment, interpolation.getVariable());
                final int removeRange = interpolation.getVariable().length() + INTERPOLATION_CHARS_SIZE * 2;
                text.getCharList().subList(index, index + removeRange).clear();
                text.insertString(index, itemValue);

                final int gap = itemValue.length() - removeRange;
                resetCharShape(paragraph.getCharShape().getPositonShapeIdPairList(), index, gap);
                index += gap;
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }

        return paragraph;
    }

    public String interpolatedValue(HwpTemplateData segment, String variable) {
        return segment.value(variable);
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

    public long getItemTextShapeId() {
        int index = 0;
        for (Interpolation interpolation : interpolations) {
            index += interpolation.getIndex();

//            if (HwpTemplate.HwpTemplateItem.title == interpolation.getItem()) {
//                for (CharPositionShapeIdPair pair : originParagraph.getCharShape().getPositonShapeIdPairList()) {
//                    if (index <= pair.getPosition())
//                        return pair.getShapeId();
//                }
//
//            }
        }

        return -1;
    }


    @Data
    public static class Interpolation {
        private final int index;
        private final String variable;

        private Interpolation(int index, String variable) {
            this.index = index;
            this.variable = variable;
        }

        public static List<Interpolation> builder(Paragraph paragraph) {
            final ParaText text = paragraph.getText();
            if (text == null) return emptyList();

            final List<Interpolation> result = new ArrayList<>();
            final List<HWPChar> hwpChars = text.getCharList();

            int limit = hwpChars.size();
            int j = 0;
            for (int i = 0; i < limit - 1; i++, j++) {
                final int code = hwpChars.get(i).getCode();
                final int nextCode = hwpChars.get(i + 1).getCode();
                if (INTERPOLATION_START_CHAR == code && INTERPOLATION_START_CHAR == nextCode) {
                    result.add(findInterpolation(i, j, hwpChars));
                    limit = hwpChars.size();
                    j = 0;
                }
            }

            return result;
        }

        private static Interpolation findInterpolation(int index, int distance, List<HWPChar> hwpChars) {
            final StringBuilder name = new StringBuilder();
            for (int j = index + INTERPOLATION_CHARS_SIZE; j < hwpChars.size() - 1; j++) {
                final HWPChar chr = hwpChars.get(j);
                if (INTERPOLATION_CLOSE_CHAR == chr.getCode() && INTERPOLATION_CLOSE_CHAR == hwpChars.get(j + 1).getCode()) {
                    return new Interpolation(distance, name.toString());
                } else {
                    try {
                        name.append(new HWPCharNormal(chr.getCode()).getCh());
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
            }

            throw new UnsupportedOperationException("malformed");
        }
    }

}
