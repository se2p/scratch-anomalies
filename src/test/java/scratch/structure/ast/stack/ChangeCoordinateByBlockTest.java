package scratch.structure.ast.stack;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import scratch.structure.ast.Ast;
import scratch.structure.ast.ScratchBlock;
import scratch.structure.ast.inputs.Literal;
import utils.JsonParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ChangeCoordinateByBlockTest {

    JsonNode script;

    @Before
    public void setup() {
        script = JsonParser.getBlocksNodeFromJSON("./src/test/java/scratch/structure/ast/fixtures/changecoordinateby.json");
    }

    @Test
    public void testStructure() {
        Ast ast = new Ast();
        ast.parseScript(script);

        ScratchBlock root = ast.getRoot();
        if (!(root instanceof ChangeCoordinateByBlock)) {
            fail("Result of this fixture should be a changecoordinateby block");
        }

        ScratchBlock node = root;
        int count = 0;
        while (node.getNext() != null) {
            count++;
            node = (ScratchBlock) node.getNext();
        }
        assertEquals("Three nodes expected", 3, count);
    }

    @Test
    public void testIntInput() {
        Ast ast = new Ast();
        ast.parseScript(script);

        ScratchBlock root = ast.getRoot();
        if (!(root instanceof ChangeCoordinateByBlock)) {
            fail("Result of this fixture should be a changecoordinateby block");
        }
        ChangeCoordinateByBlock block = (ChangeCoordinateByBlock) root;
        Literal primary = (Literal) block.getSlot().getPrimary();
        String value = primary.getValue();
        assertEquals("10", value);
    }

//    @Test TODO update this test as soon as we have variable blocks
//    public void testVariableInput() {
//        Ast ast = new Ast();
//        ast.parseScript(script);
//
//        ScratchBlock root = ast.getRoot();
//        if (!(root instanceof ChangeCoordinateByBlock)) {
//            fail("Result of this fixture should be a changecoordinateby block");
//        }
//        ChangeCoordinateByBlock block = (ChangeCoordinateByBlock) ((ChangeCoordinateByBlock) (root.getNext())).getNext();
//
//        assertEquals("`jEk@4|i[#Fk?(8x)AV.-my variable", block.getInputID());
//    }

    @Test
    public void testXAndYDistinction() {
        Ast ast = new Ast();
        ast.parseScript(script);

        ScratchBlock root = ast.getRoot();
        if (!(root instanceof ChangeXCoordinateByBlock)) {
            fail("Result of this fixture should be a changexcoordinateby block");
        }

        ChangeCoordinateByBlock block = (ChangeCoordinateByBlock) root.getNext();
        if (!(block instanceof ChangeYCoordinateByBlock)) {
            fail("Result of this fixture should be a changeycoordinateby block");
        }

        block = (ChangeCoordinateByBlock) block.getNext();
        if (!(block instanceof ChangeXCoordinateByBlock)) {
            fail("Result of this fixture should be a changexcoordinateby block");
        }

        block = (ChangeCoordinateByBlock) block.getNext();
        if (!(block instanceof ChangeYCoordinateByBlock)) {
            fail("Result of this fixture should be a changeycoordinateby block");
        }
    }
}

