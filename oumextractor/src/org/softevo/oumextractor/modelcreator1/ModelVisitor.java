package org.softevo.oumextractor.modelcreator1;

import org.softevo.oumextractor.modelcreator1.model.Model;

/**
 * This is the interface to use for visitors passed to the
 * <code>ModelAnalyzer</code> class.
 *
 * @author Andrzej Wasylkowski
 */
public interface ModelVisitor {

    /**
     * This method gets called for each model that needs to be analyzed.
     *
     * @param id    Id of the model.
     * @param model Model that needs to be analyzed.
     * @param data  Data about the model.
     */
    public void visit(int id, Model model, ModelData data);
}
