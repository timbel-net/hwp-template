package net.timbel.hwptemplate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Sampling {
    private String index;
    private String name;
    private String text;
    private double begin;
    private double close;


    static String timeFormatWithMillis(double seconds) {
        return String.format("%02d:%02d:%02d.%d",
                (int) seconds / 3600,
                (int) seconds / 60,
                (int) seconds % 60,
                (int) ((seconds - (int) seconds) * 100)
        );
    }

}
