/*
 * Copyright (C) 2019 LitterBox contributors
 *
 * This file is part of LitterBox.
 *
 * LitterBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * LitterBox is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LitterBox. If not, see <http://www.gnu.org/licenses/>.
 */
package newanalytics.bugpattern;


import static junit.framework.TestCase.fail;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.truth.Truth;
import java.io.File;
import java.io.IOException;
import newanalytics.IssueReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scratch.ast.ParsingException;
import scratch.ast.model.Program;
import scratch.ast.parser.ProgramParser;

public class MissingCloneInitializationTest {

    private static Program program;
    private static Program clicked;

    @BeforeAll
    public static void setup() {
        String path = "src/test/fixtures/bugpattern/missingCloneInitialization.json";
        File file = new File(path);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            program = ProgramParser.parseProgram("missingCloneInit", objectMapper.readTree(file));
            file = new File("src/test/fixtures/bugpattern/cloningWithClicked.json");
            clicked = ProgramParser.parseProgram("cloningWithClicked", objectMapper.readTree(file));
        } catch (IOException | ParsingException e) {
            fail();
        }
    }

    @Test
    public void testMissingPenUp() {
        MissingCloneInitialization finder = new MissingCloneInitialization();
        final IssueReport check = finder.check(program);
        Truth.assertThat(check.getCount()).isEqualTo(1);
        Truth.assertThat(check.getPosition().get(0)).isEqualTo("Anina Dance");
    }

    @Test
    public void testCloningWithClicked() {
        MissingCloneInitialization finder = new MissingCloneInitialization();
        final IssueReport check = finder.check(clicked);
        Truth.assertThat(check.getCount()).isEqualTo(0);
    }
}