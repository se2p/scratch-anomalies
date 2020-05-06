package de.uni_passau.fim.se2.litterbox.ast.model.metadata.block;

import de.uni_passau.fim.se2.litterbox.ast.model.AbstractNode;
import de.uni_passau.fim.se2.litterbox.ast.model.metadata.Metadata;
import de.uni_passau.fim.se2.litterbox.ast.visitor.ScratchVisitor;

import java.util.List;

public class ExistingMutationMetadata extends AbstractNode implements MutationMetadata {
    private String tagName;
    private List<String> children;
    private String procCode;
    private String argumentIds;
    private String argumentNames;
    private String argumentDefaults;
    private boolean warp;

    public ExistingMutationMetadata(String tagName, List<String> children, String procCode, String argumentIds,
                                    String argumentNames, String argumentDefaults, boolean warp) {
        super();
        this.tagName = tagName;
        this.children = children;
        this.procCode = procCode;
        this.argumentIds = argumentIds;
        this.argumentNames = argumentNames;
        this.argumentDefaults = argumentDefaults;
        this.warp = warp;
    }

    public String getTagName() {
        return tagName;
    }

    public List<String> getChild() {
        return children;
    }

    public String getProcCode() {
        return procCode;
    }

    public String getArgumentIds() {
        return argumentIds;
    }

    public String getArgumentNames() {
        return argumentNames;
    }

    public String getArgumentDefaults() {
        return argumentDefaults;
    }

    public boolean isWarp() {
        return warp;
    }

    @Override
    public void accept(ScratchVisitor visitor) {
        visitor.visit(this);
    }
}
