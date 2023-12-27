package io.github.greennlab.hwptemplate;

import lombok.Data;

@Data
@SuppressWarnings("ALL")
public class Sampling {
    private String 순번;
    private String 이름;
    private String 내용;
    private double 시작시간;
    private double 종료시간;


    static String timeFormatWithMillis(double seconds) {
        return String.format("%02d:%02d:%02d.%d",
                (int) seconds / 3600,
                (int) seconds / 60,
                (int) seconds % 60,
                (int) ((seconds - (int) seconds) * 100)
        );
    }

}
