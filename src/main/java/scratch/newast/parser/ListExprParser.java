package scratch.newast.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import scratch.newast.ParsingException;
import scratch.newast.model.expression.list.ListExpr;
import scratch.newast.model.variable.Identifier;
import scratch.newast.model.variable.Qualified;
import scratch.newast.model.variable.Variable;
import scratch.newast.parser.symboltable.ExpressionListInfo;

import static scratch.newast.Constants.INPUTS_KEY;
import static scratch.newast.Constants.POS_BLOCK_ID;
import static scratch.newast.Constants.POS_DATA_ARRAY;
import static scratch.newast.Constants.POS_INPUT_ID;

public class ListExprParser {
    public static ListExpr parseListExpr(JsonNode block, int pos, JsonNode blocks) throws ParsingException {
        ArrayNode exprArray = ExpressionParser.getExprArrayAtPos(block.get(INPUTS_KEY), pos);

        if (ExpressionParser.getShadowIndicator(exprArray) == 1 || exprArray.get(POS_BLOCK_ID) instanceof TextNode) {
            throw new ParsingException("Block does not contain a list"); //Todo improve message
        }

        String idString = exprArray.get(POS_DATA_ARRAY).get(POS_INPUT_ID).asText();
        if (ProgramParser.symbolTable.getLists().containsKey(idString)) {
            ExpressionListInfo variableInfo = ProgramParser.symbolTable.getLists().get(idString);
            return new Qualified(new Identifier(variableInfo.getActor()),
                    new Identifier((variableInfo.getVariableName())));
        }
        throw new ParsingException("Block does not contain a list"); //Todo improve message
    }

    static Variable parseVariableFromFields(JsonNode fields) throws ParsingException {
        String identifier = fields.get("LIST").get(1).asText(); // TODO add some constants
        if (ProgramParser.symbolTable.getLists().containsKey(identifier)) {
            ExpressionListInfo variableInfo = ProgramParser.symbolTable.getLists().get(identifier);
            return new Qualified(new Identifier(variableInfo.getActor()),
                    new Identifier((variableInfo.getVariableName())));
        } else {
            throw new ParsingException("No list to parse found in fields.");
        }

    }
}
