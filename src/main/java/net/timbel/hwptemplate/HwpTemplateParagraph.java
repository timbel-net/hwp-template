package net.timbel.hwptemplate;

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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

@Getter
@Slf4j
public class HwpTemplateParagraph {

    private final List<Interpolation> interpolations;
    private final Paragraph originParagraph;


    public HwpTemplateParagraph(Paragraph originParagraph) {
        this.originParagraph = originParagraph;
        this.interpolations = Interpolation.builder(originParagraph);
    }

    protected <T> Paragraph interpolate(HwpTemplateData<T> data) {
        var paragraph = originParagraph.clone();
        if (paragraph.getText() == null) return paragraph;

        val text = paragraph.getText();
        for (LineSegItem lineSegItem : paragraph.getLineSeg().getLineSegItemList()) {
            lineSegItem.setSegmentWidth(lineSegItem.getSegmentWidth() + 1);
        }

        int position = 0;
        for (final Interpolation interpolation : interpolations) {
            position += interpolation.getPosition();

            try {
                val name = interpolation.getName();
                val value = data.value(name);
                val replaceRange = name.length() + HwpTemplate.INTERPOLATION_CHARS_SIZE;
                text.getCharList().subList(position, position + replaceRange).clear();
                text.insertString(position, value);

                val gap = value.length() - replaceRange;
                resetCharShape(paragraph.getCharShape().getPositonShapeIdPairList(), position, gap);
                position += gap;
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }

        return paragraph;
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
                if (HwpTemplate.INTERPOLATION_START_CHAR == code && HwpTemplate.INTERPOLATION_START_CHAR == nextCode) {
                    start = i + 2;
                } else if (start > -1 && HwpTemplate.INTERPOLATION_CLOSE_CHAR == code && HwpTemplate.INTERPOLATION_CLOSE_CHAR == nextCode) {
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

}
