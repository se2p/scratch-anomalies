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
package scratch.ast.parser.stmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scratch.ast.ParsingException;
import scratch.ast.model.Program;
import scratch.ast.model.statement.UnspecifiedStmt;
import scratch.ast.parser.ProgramParser;

public class UnspecifiedStmtTest {

    private static Program unusedProc;
    private static ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    public static void setUp() throws IOException, ParsingException {

        File f = new File("./src/test/fixtures/stmtParser/textToSpeech.json");
        unusedProc = ProgramParser.parseProgram(f.getName(), mapper.readTree(f));
    }

    @Test
    public void parseTextToSpeechTest() {
        Assertions.assertTrue(unusedProc.getActorDefinitionList().getDefintions().get(1).getScripts().getScriptList().get(0).getStmtList().getStmts().getListOfStmt().get(0) instanceof UnspecifiedStmt);
    }
}
