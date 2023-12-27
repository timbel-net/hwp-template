package net.timbel.hwptemplate;

import kr.dogfoot.hwplib.reader.HWPReader;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
class HwpTemplateTest {

    @Test
    void shouldCreateHwpFile() throws Exception {
        val given = List.of(
                Sampling.builder().name("Kane").index("9").text("FW").begin(12.123).close(23.234).build(),
                Sampling.builder().name("Davies").index("9").text("MF").begin(12.123).close(23.234).build()
        );

        val in = ClassLoader.getSystemResourceAsStream("hwp-template.hwp");
        assert in != null;

        val hwp = HWPReader.fromInputStream(in);
        val template = new HwpTemplate<Sampling>(hwp);

        template.write(given, data -> paragraph -> {
            System.out.println(data);
            data.put("시작시간", Sampling.timeFormatWithMillis((double) data.get("begin")));
            data.put("종료시간", Sampling.timeFormatWithMillis((double) data.get("close")));
            return paragraph;
        });

        template.toFile("test.hwp");
    }

}