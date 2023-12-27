package io.github.greennlab.hwptemplate;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

class HwpTemplateTest {

    @Test
    void shouldCreateHwpFile() throws Exception {
        val in = ClassLoader.getSystemResourceAsStream("hwp-template.hwp");
        assert in != null;

        val hwp = HWPReader.fromInputStream(in);
        val template = new HwpTemplate<Sampling>(hwp);

        template.write(Collections.singletonList(new Sampling()), segment -> paragraph -> {
            return paragraph;
        });
    }

}