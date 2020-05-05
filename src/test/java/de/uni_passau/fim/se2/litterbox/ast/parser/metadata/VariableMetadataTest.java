package de.uni_passau.fim.se2.litterbox.ast.parser.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_passau.fim.se2.litterbox.ast.model.metadata.astLists.VariableMetadataList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static de.uni_passau.fim.se2.litterbox.ast.Constants.TARGETS_KEY;
import static de.uni_passau.fim.se2.litterbox.ast.Constants.VARIABLES_KEY;

public class VariableMetadataTest {
    private static ObjectMapper mapper = new ObjectMapper();
    private static JsonNode prog;
    private static JsonNode empty;

    @BeforeAll
    public static void setUp() throws IOException {
        File f = new File("./src/test/fixtures/emptyProject.json");
        empty = mapper.readTree(f);
        f = new File("./src/test/fixtures/listBlocks.json");
        prog = mapper.readTree(f);
    }

    @Test
    public void testEmptyProgram() {
        VariableMetadataList monitors = VariableMetadataListParser.parse(empty.get(TARGETS_KEY).get(0)
                .get(VARIABLES_KEY));
        Assertions.assertEquals(1, monitors.getList().size());
    }

    @Test
    public void testVariablesProgram() {
        VariableMetadataList monitors = VariableMetadataListParser.parse(prog.get(TARGETS_KEY).get(0)
                .get(VARIABLES_KEY));
        Assertions.assertEquals(2, monitors.getList().size());
        Assertions.assertEquals("`jEk@4|i[#Fk?(8x)AV.-my variable", monitors.getList().get(0).getVariableId());
        Assertions.assertEquals("my variable", monitors.getList().get(0).getVariableName());
        Assertions.assertEquals("0", monitors.getList().get(0).getValue());
    }
}
