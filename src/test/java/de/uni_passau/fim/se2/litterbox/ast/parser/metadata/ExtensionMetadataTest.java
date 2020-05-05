package de.uni_passau.fim.se2.litterbox.ast.parser.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_passau.fim.se2.litterbox.ast.model.metadata.ExtensionMetadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static de.uni_passau.fim.se2.litterbox.ast.Constants.EXTENSIONS_KEY;

public class ExtensionMetadataTest {
    private static ObjectMapper mapper = new ObjectMapper();
    private static JsonNode prog;
    private static JsonNode empty;

    @BeforeAll
    public static void setUp() throws IOException {
        File f = new File("./src/test/fixtures/emptyProject.json");
        empty = mapper.readTree(f);
        f = new File("./src/test/fixtures/metadata/metaExtensionMonitorData.json");
        prog = mapper.readTree(f);
    }

    @Test
    public void testEmptyProgram() {
        ExtensionMetadata meta = ExtensionMetadataParser.parse(empty.get(EXTENSIONS_KEY));
        Assertions.assertEquals(0, meta.getExtensionNames().size());
    }

    @Test
    public void testTwoExtensions() {
        ExtensionMetadata meta = ExtensionMetadataParser.parse(prog.get(EXTENSIONS_KEY));
        Assertions.assertEquals(2, meta.getExtensionNames().size());
        Assertions.assertEquals("pen", meta.getExtensionNames().get(0));
        Assertions.assertEquals("music", meta.getExtensionNames().get(1));
    }
}
