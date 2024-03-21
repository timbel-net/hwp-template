package net.timbel.hwptemplate;

import org.junit.jupiter.api.Test;

class RunnerTest {

    @Test
    void shouldRunNoProblem() throws Exception {

        Runner.main("src/test/resources/runner/template.hwp", "src/test/resources/runner/sample.json", "build/test.hwp");
    }

}