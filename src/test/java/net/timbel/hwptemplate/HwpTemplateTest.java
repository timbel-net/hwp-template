package net.timbel.hwptemplate;

import kr.dogfoot.hwplib.reader.HWPReader;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class HwpTemplateTest {

    @Test
    void shouldCreateHwpFile() throws Exception {
        val given = Arrays.asList(
                Sampling.builder().name("Kane").text("FW").begin(12.123).close(23.234).build(),
                Sampling.builder().name("Davies").text("MF").begin(12.123).close(23.234).build()
        );

        val in = ClassLoader.getSystemResourceAsStream("hwp-template.hwp");
        assert in != null;

        val hwp = HWPReader.fromInputStream(in);
        val template = new HwpTemplate<Sampling>(hwp);

        template.write(given);

        template.toFile("test.hwp");
    }

}