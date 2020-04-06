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
package de.uni_passau.fim.se2.litterbox.ast.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import de.uni_passau.fim.se2.litterbox.ast.ParsingException;
import de.uni_passau.fim.se2.litterbox.ast.model.expression.bool.BoolExpr;
import de.uni_passau.fim.se2.litterbox.ast.model.expression.num.NumExpr;
import de.uni_passau.fim.se2.litterbox.ast.model.expression.string.*;
import de.uni_passau.fim.se2.litterbox.ast.model.expression.string.attributes.Attribute;
import de.uni_passau.fim.se2.litterbox.ast.model.expression.string.attributes.AttributeFromFixed;
import de.uni_passau.fim.se2.litterbox.ast.model.expression.string.attributes.AttributeFromVariable;
import de.uni_passau.fim.se2.litterbox.ast.model.expression.string.attributes.FixedAttribute;
import de.uni_passau.fim.se2.litterbox.ast.model.identifier.*;
import de.uni_passau.fim.se2.litterbox.ast.model.literals.StringLiteral;
import de.uni_passau.fim.se2.litterbox.ast.model.variable.Parameter;
import de.uni_passau.fim.se2.litterbox.ast.model.variable.ScratchList;
import de.uni_passau.fim.se2.litterbox.ast.model.variable.Variable;
import de.uni_passau.fim.se2.litterbox.ast.opcodes.ProcedureOpcode;
import de.uni_passau.fim.se2.litterbox.ast.opcodes.StringExprOpcode;
import de.uni_passau.fim.se2.litterbox.ast.parser.symboltable.ExpressionListInfo;
import de.uni_passau.fim.se2.litterbox.ast.parser.symboltable.VariableInfo;
import de.uni_passau.fim.se2.litterbox.utils.Preconditions;

import java.util.Optional;

import static de.uni_passau.fim.se2.litterbox.ast.Constants.*;
import static de.uni_passau.fim.se2.litterbox.ast.opcodes.DependentBlockOpcodes.sensing_of_object_menu;

public class StringExprParser {

    public static StringExpr parseStringExpr(JsonNode block, String inputName, JsonNode blocks)
            throws ParsingException {
        ArrayNode exprArray = ExpressionParser.getExprArrayByName(block.get(INPUTS_KEY), inputName);
        int shadowIndicator = ExpressionParser.getShadowIndicator(exprArray);
        if (shadowIndicator == INPUT_SAME_BLOCK_SHADOW
                || (shadowIndicator == INPUT_BLOCK_NO_SHADOW && !(exprArray.get(POS_BLOCK_ID) instanceof TextNode))) {
            try {
                return parseStr(block.get(INPUTS_KEY), inputName);
            } catch (ParsingException e) {
                return new UnspecifiedStringExpr();
            }
        } else if (exprArray.get(POS_BLOCK_ID) instanceof TextNode) {
            return parseTextNode(blocks, exprArray);
        } else {
            StringExpr variableInfo = parseVariable(exprArray);
            if (variableInfo != null) {
                return variableInfo;
            }
        }
        throw new ParsingException("Could not parse StringExpr");
    }

    public static StringExpr parseStringExpr(JsonNode block, int pos, JsonNode blocks) throws ParsingException {
        ArrayNode exprArray = ExpressionParser.getExprArrayAtPos(block.get(INPUTS_KEY), pos);
        int shadowIndicator = ExpressionParser.getShadowIndicator(exprArray);
        if (shadowIndicator == INPUT_SAME_BLOCK_SHADOW ||
                (shadowIndicator == INPUT_BLOCK_NO_SHADOW && !(exprArray.get(POS_BLOCK_ID) instanceof TextNode))) {
            try {
                return parseStr(block.get(INPUTS_KEY), pos);
            } catch (ParsingException e) {
                return new UnspecifiedStringExpr();
            }
        } else if (exprArray.get(POS_BLOCK_ID) instanceof TextNode) {
            return parseTextNode(blocks, exprArray);
        } else {
            StringExpr variableInfo = parseVariable(exprArray);
            if (variableInfo != null) {
                return variableInfo;
            }
        }
        throw new ParsingException("Could not parse StringExpr");
    }

    private static StringExpr parseVariable(ArrayNode exprArray) {
        String idString = exprArray.get(POS_DATA_ARRAY).get(POS_INPUT_ID).asText();
        if (ProgramParser.symbolTable.getVariables().containsKey(idString)) {
            VariableInfo variableInfo = ProgramParser.symbolTable.getVariables().get(idString);

            return new AsString(
                    new Qualified(
                            new StrId(variableInfo.getActor()),
                            new Variable(new StrId((variableInfo.getVariableName())))
                    ));
        } else if (ProgramParser.symbolTable.getLists().containsKey(idString)) {
            ExpressionListInfo variableInfo = ProgramParser.symbolTable.getLists().get(idString);
            return new AsString(
                    new Qualified(
                            new StrId(variableInfo.getActor()),
                            new ScratchList(new StrId((variableInfo.getVariableName())))
                    ));
        }
        return null;
    }

    private static StringExpr parseTextNode(JsonNode blocks, ArrayNode exprArray) throws ParsingException {
        String identifier = exprArray.get(POS_BLOCK_ID).asText();
        String opcode = blocks.get(identifier).get(OPCODE_KEY).asText();
        if (opcode.equals(ProcedureOpcode.argument_reporter_string_number.name()) || opcode.equals(ProcedureOpcode.argument_reporter_boolean.name())) {
            return parseParameter(blocks, exprArray);
        }
        final Optional<StringExpr> stringExpr = maybeParseBlockStringExpr(blocks.get(identifier), blocks);
        if (stringExpr.isPresent()) {
            return stringExpr.get();
        }

        final Optional<NumExpr> optExpr = NumExprParser.maybeParseBlockNumExpr(blocks.get(identifier), blocks);
        if (optExpr.isPresent()) {
            return new AsString(optExpr.get());
        }

        final Optional<BoolExpr> boolExpr = BoolExprParser.maybeParseBlockBoolExpr(blocks.get(identifier), blocks);
        if (boolExpr.isPresent()) {
            return new AsString(boolExpr.get());
        }

        return new UnspecifiedStringExpr();
    }

    private static StringExpr parseParameter(JsonNode blocks, ArrayNode exprArray) {
        JsonNode paramBlock = blocks.get(exprArray.get(POS_BLOCK_ID).asText());
        String name = paramBlock.get(FIELDS_KEY).get(VALUE_KEY).get(VARIABLE_NAME_POS).asText();

        return new AsString(new Parameter(new StrId(name)));
    }

    private static StringLiteral parseStr(JsonNode inputs, int pos) throws ParsingException {
        String value = ExpressionParser.getDataArrayAtPos(inputs, pos).get(POS_INPUT_VALUE).asText();
        return new StringLiteral(value);
    }

    private static StringLiteral parseStr(JsonNode inputs, String inputName) throws ParsingException {
        String value = ExpressionParser.getDataArrayByName(inputs, inputName).get(POS_INPUT_VALUE).asText();
        return new StringLiteral(value);
    }

    static Optional<StringExpr> maybeParseBlockStringExpr(JsonNode expressionBlock, JsonNode blocks) {
        try {
            return Optional.of(parseBlockStringExpr(expressionBlock, blocks));
        } catch (ParsingException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    static StringExpr parseBlockStringExpr(JsonNode expressionBlock, JsonNode blocks) throws ParsingException {
        String opcodeString = expressionBlock.get(OPCODE_KEY).asText();
        Preconditions
                .checkArgument(StringExprOpcode.contains(opcodeString), opcodeString + " is not a StringExprOpcode.");
        StringExprOpcode opcode = StringExprOpcode.valueOf(opcodeString);
        switch (opcode) {
            case operator_join:
                StringExpr first = parseStringExpr(expressionBlock, 0, blocks);
                StringExpr second = parseStringExpr(expressionBlock, 1, blocks);
                return new Join(first, second);
            case operator_letter_of:
                NumExpr num = NumExprParser.parseNumExpr(expressionBlock, 0, blocks);
                StringExpr word = parseStringExpr(expressionBlock, 1, blocks);
                return new LetterOf(num, word);
            case sensing_username:
                return new Username();
            case data_itemoflist:
                NumExpr index = NumExprParser.parseNumExpr(expressionBlock, 0, blocks);
                String id =
                        expressionBlock.get(FIELDS_KEY).get(LIST_KEY).get(LIST_IDENTIFIER_POS).asText();
                Identifier var;
                if (ProgramParser.symbolTable.getLists().containsKey(id)) {
                    ExpressionListInfo variableInfo = ProgramParser.symbolTable.getLists().get(id);
                    var = new Qualified(new StrId(variableInfo.getActor()),
                            new ScratchList(new StrId((variableInfo.getVariableName()))));
                } else {
                    var = new UnspecifiedId();
                }
                return new ItemOfVariable(index, var);
            case looks_costumenumbername:
                String number_name = expressionBlock.get(FIELDS_KEY).get(NUMBER_NAME_KEY).get(0).asText();
                return new Costume(NameNum.fromString(number_name));
            case looks_backdropnumbername:
                number_name = expressionBlock.get(FIELDS_KEY).get(NUMBER_NAME_KEY).get(0).asText();
                return new Backdrop(NameNum.fromString(number_name));
            case sensing_answer:
                return new Answer();
            case sensing_of:
                String menuIdentifier = expressionBlock.get(INPUTS_KEY).get(OBJECT_KEY).get(1).asText();
                JsonNode objectMenuBlock = blocks.get(menuIdentifier);

                LocalIdentifier localIdentifier;
                if (objectMenuBlock != null) {
                    JsonNode menuOpcode = objectMenuBlock.get(OPCODE_KEY);
                    if (menuOpcode.asText().equalsIgnoreCase(sensing_of_object_menu.name())) {
                        localIdentifier = new StrId(
                                objectMenuBlock.get(FIELDS_KEY).get(OBJECT_KEY).get(FIELD_VALUE)
                                        .asText());
                    } else {
                        //Technically there could be blocks in here, but we do not allow
                        //any expressions to work as identifiers here.
                        localIdentifier = new StrId("");
                    }
                } else {
                    //Technically there could be blocks in here, but we do not allow
                    //any expressions to work as identifiers here.
                    localIdentifier = new StrId("");
                }
                String prop = expressionBlock.get(FIELDS_KEY).get("PROPERTY").get(0).asText();
                Attribute property;
                switch (prop) {
                    case "y position":
                    case "x position":
                    case "direction":
                    case "costume #":
                    case "costume name":
                    case "size":
                    case "volume":
                    case "backdrop name":
                    case "backdrop #":
                        property = new AttributeFromFixed(FixedAttribute.fromString(prop));
                        break;
                    default:
                        property = new AttributeFromVariable(new StrId(prop));
                }
                return new AttributeOf(property, localIdentifier);
            default:
                throw new RuntimeException(opcodeString + " is not covered by parseBlockStringExpr");
        }
    }
}
