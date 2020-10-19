package org.softevo.jadet.sca;


import org.softevo.jutil.UnionFind;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Each instance of this class represents an ordered list of violations.
 *
 * @author Andrzej Wasylkowski
 */
public class ViolationsList extends ArrayList<Violation> {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = -8660145178183240680L;


    /**
     * Disjoint sets of equivalent (i.e., mutually duplicate) violations ids.
     */
    private UnionFind<Integer> equivalents = new UnionFind<Integer>();

    /**
     * Reads the violations list from the given XML file.
     *
     * @param violationsFile XML file to read violations list from.
     * @return List of violations, as read from the given XML file.
     */
    public static ViolationsList readFromXML(File violationsFile) {
        try {
            ViolationsList violations = new ViolationsList();
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xml = builder.parse(violationsFile);
            Element violationsElement = xml.getDocumentElement();

            // read violations
            NodeList violationsList = violationsElement.getElementsByTagName("violation");
            for (int i = 0; i < violationsList.getLength(); i++) {
                Element violationElement = (Element) violationsList.item(i);
                Violation violation = Violation.createFromXMLElement(violationElement);
                violations.add(violation);
            }

            // read equivalents
            NodeList equivalentsList = violationsElement.getElementsByTagName("equivalents");
            for (int i = 0; i < equivalentsList.getLength(); i++) {
                Element equivalentsElement = (Element) equivalentsList.item(i);
                Set<Integer> ids = new HashSet<Integer>();
                NodeList idsList = equivalentsElement.getElementsByTagName("equivalent");
                for (int j = 0; j < idsList.getLength(); j++) {
                    Element idElement = (Element) idsList.item(j);
                    ids.add(Integer.valueOf(idElement.getAttribute("id")));
                }
                Integer repr = ids.iterator().next();
                for (Integer id : ids) {
                    violations.equivalents.union(repr, id);
                }
            }

            return violations;
        } catch (ParserConfigurationException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        } catch (SAXException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        }
    }

    /**
     * Copies details about identical violations from the given reference list
     * to this list.
     *
     * @param reference         Reference list to copy information from.
     * @param referenceFileName Name of the file with the reference list.
     */
    public void copyDetailsFrom(ViolationsList reference,
                                String referenceFileName) {
        // first, find mappings id => id
        Map<Integer, Integer> id2refid = new HashMap<Integer, Integer>();
        Map<Integer, Integer> refid2id = new HashMap<Integer, Integer>();
        main_loop:
        for (int id = 1; id <= this.size(); id++) {
            Violation v = this.get(id - 1);
            for (int refid = 1; refid <= reference.size(); refid++) {
                Violation refv = reference.get(refid - 1);
                if (refv.getObject().equals(v.getObject()) &&
                        refv.getMissingProperties().equals(v.getMissingProperties()) &&
                        refv.getPattern().getProperties().equals(v.getPattern().getProperties())) {
                    id2refid.put(id, refid);
                    refid2id.put(refid, id);
                    continue main_loop;
                }
            }
        }

        // second, find potential mapping id => ids
        Map<Integer, Set<Integer>> p_id2refid = new HashMap<Integer, Set<Integer>>();
        for (int id = 1; id <= this.size(); id++) {
            if (id2refid.containsKey(id))
                continue;
            Violation v = this.get(id - 1);
            for (int refid = 1; refid <= reference.size(); refid++) {
                Violation refv = reference.get(refid - 1);
                if (refv.getObject().equals(v.getObject())) {
                    if (!p_id2refid.containsKey(id)) {
                        p_id2refid.put(id, new HashSet<Integer>());
                    }
                    p_id2refid.get(id).add(refid);
                }
            }
        }

        // update the details for identical violations
        for (Map.Entry<Integer, Integer> entry : refid2id.entrySet()) {
            Violation refv = reference.get(entry.getKey() - 1);
            Violation v = this.get(entry.getValue() - 1);
            v.setType(refv.getType());
            v.setDescription(refv.getDescription());
        }

        // update the descriptions for potentially identical violations
        for (Map.Entry<Integer, Set<Integer>> entry : p_id2refid.entrySet()) {
            StringBuffer str = new StringBuffer();
            str.append("Potentially equivalent to violations #");
            for (int refid : entry.getValue()) {
                str.append(refid).append(", ");
            }
            str.delete(str.length() - 2, str.length());
            str.append(" from '").append(referenceFileName).append("'");
            Violation v = this.get(entry.getKey() - 1);
            v.setDescription(str.toString());
        }

        // update the duplicates info for all identical violations
        for (int id1 : id2refid.keySet()) {
            int refid1 = id2refid.get(id1);
            for (int id2 : id2refid.keySet()) {
                int refid2 = id2refid.get(id2);
                if (reference.equivalents.find(refid1) == reference.equivalents.find(refid2)) {
                    this.equivalents.union(id1, id2);
                }
            }
        }
    }

    /**
     * Finds ids of violations that are potentially duplicates of the given
     * violation (i.e., have the same violating object).
     *
     * @param vid Id of the violation, whose duplicates are to be found.
     * @return Sorted list of indices of potential duplicates.
     */
    public List<Integer> getPotentialDuplicatesIds(int vid) {
        List<Integer> ids = new ArrayList<Integer>();
        Violation v = this.get(vid - 1);
        for (int id = 1; id <= this.size(); id++) {
            if (id == vid)
                continue;
            Violation pd = this.get(id - 1);
            if (pd.getObject().equals(v.getObject()))
                ids.add(id);
        }
        return ids;
    }

    /**
     * Returns sorted duplicates of the given violation.
     *
     * @param vid Id of the violation to get duplicates of.
     * @return Sorted list of ids of duplicates.
     */
    public List<Integer> getDuplicates(int vid) {
        List<Integer> duplicates = new ArrayList<Integer>();
        for (int id = 1; id <= this.size(); id++) {
            if (id != vid && this.equivalents.find(vid) == this.equivalents.find(id))
                duplicates.add(id);
        }
        return duplicates;
    }

    /**
     * Returns disjoint lists of quivalents od potential duplicates of the given
     * violation (including the list containing the violation itself).
     * Everything is sorted by the ids.
     *
     * @param vid Id of the violation, whose duplicates are to be found.
     * @return Sorted list of sorted indices of equivalents of potential duplicates.
     */
    public List<List<Integer>> getDuplicatesGroupsIds(int vid) {
        List<Integer> potentialDuplicatesIds = getPotentialDuplicatesIds(vid);
        potentialDuplicatesIds.add(vid);
        Map<Integer, List<Integer>> repr2equivalents =
                new HashMap<Integer, List<Integer>>();
        for (int id : potentialDuplicatesIds) {
            int repr = this.equivalents.find(id);
            if (!repr2equivalents.containsKey(repr)) {
                repr2equivalents.put(repr, new ArrayList<Integer>());
            }
            repr2equivalents.get(repr).add(id);
        }
        List<List<Integer>> result = new ArrayList<List<Integer>>();
        Set<Integer> usedReprs = new HashSet<Integer>();
        for (Integer id : potentialDuplicatesIds) {
            int repr = this.equivalents.find(id);
            if (usedReprs.contains(repr))
                continue;
            result.add(repr2equivalents.get(repr));
            usedReprs.add(repr);
        }
        return result;
    }

    /**
     * Sets the violations of a given id as a duplicate of the other violation.
     * This changes the duplicate's type and description.
     *
     * @param idNewDuplicate Id of the new duplicate.
     * @param idOther        Id of the other (reference) violation.
     */
    public void setDuplicate(int idNewDuplicate, int idOther) {
        this.equivalents.union(idNewDuplicate, idOther);
        Violation newDuplicate = this.get(idNewDuplicate - 1);
        Violation other = this.get(idOther - 1);
        newDuplicate.setType(other.getType());
        newDuplicate.setDescription(other.getDescription());
    }

    /**
     * Marks the given violation as a non-duplicate.
     *
     * @param vid Id of the violation to mark as a non-duplicate.
     */
    public void detachDuplicate(int vid) {
        this.equivalents.makeSingleton(vid);
    }

    /**
     * Outputs violations in this list into the given file.
     *
     * @param outFile File to output violations to.
     */
    public void writeXML(File outFile) {
        try {
            // create the XML document
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document xml = docBuilder.newDocument();
            xml.setXmlStandalone(true);
            Element violationsXML = xml.createElement("violations");
            xml.appendChild(violationsXML);

            // create XML subtree for each violation
            for (Violation violation : this) {
                Element violationXML = violation.getXMLRepresentation(xml);
                violationsXML.appendChild(violationXML);
            }

            // find sets of equivalent violations
            Map<Integer, Set<Integer>> id2equivalent =
                    new HashMap<Integer, Set<Integer>>();
            for (int id = 1; id <= this.size(); id++) {
                int repr = this.equivalents.find(id);
                if (!id2equivalent.containsKey(repr))
                    id2equivalent.put(repr, new HashSet<Integer>());
                Set<Integer> eqs = id2equivalent.get(repr);
                eqs.add(id);
            }

            // create XML subtree for each set of equivalent violations
            for (Map.Entry<Integer, Set<Integer>> entry : id2equivalent.entrySet()) {
                Set<Integer> eqs = entry.getValue();
                if (eqs.size() == 1)
                    continue;
                Element equivalentsXML = xml.createElement("equivalents");
                violationsXML.appendChild(equivalentsXML);
                for (int id : eqs) {
                    Element idXML = xml.createElement("equivalent");
                    equivalentsXML.appendChild(idXML);
                    idXML.setAttribute("id", String.valueOf(id));
                }
            }

            // output the XML data to the file
            PrintWriter writer = new PrintWriter(outFile);
            TransformerFactory tranFactory = TransformerFactory.newInstance();
            tranFactory.setAttribute("indent-number", Integer.valueOf(4));
            Transformer transformer = tranFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            Source src = new DOMSource(xml);
            Result dest = new StreamResult(writer);
            transformer.transform(src, dest);
            writer.close();
        } catch (ParserConfigurationException e) {
            e.printStackTrace(System.err);
            return;
        } catch (TransformerException e) {
            e.printStackTrace(System.err);
            return;
        } catch (FileNotFoundException e) {
            e.printStackTrace(System.err);
            return;
        }
    }
}
