package org.softevo.jadet.sca;


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


/**
 * Each instance of this class represents a list of patterns.
 *
 * @author Andrzej Wasylkowski
 */
public class PatternsList extends ArrayList<Pattern> {

    /**
     * Serial number of this class.
     */
    private static final long serialVersionUID = -6014125209546717568L;

    /**
     * Reads the patterns list from the given XML file.
     *
     * @param patternsFile XML file to read patterns list from.
     * @return List of patterns, as read from the given XML file.
     */
    public static PatternsList readFromXML(File patternsFile) {
        try {
            PatternsList patterns = new PatternsList();
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xml = builder.parse(patternsFile);
            Element patternsElement = xml.getDocumentElement();

            // read patterns
            NodeList patternsList = patternsElement.getElementsByTagName("pattern");
            for (int i = 0; i < patternsList.getLength(); i++) {
                Element patternElement = (Element) patternsList.item(i);
                Pattern pattern = Pattern.createFromXMLElement(patternElement);
                patterns.add(pattern);
            }

            return patterns;
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
     * Outputs patterns in this list into the given file.
     *
     * @param outFile File to output patterns to.
     */
    public void writeXML(File outFile) {
        try {
            // create the XML document
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document xml = docBuilder.newDocument();
            xml.setXmlStandalone(true);
            Element patternsXML = xml.createElement("patterns");
            xml.appendChild(patternsXML);

            // create XML subtree for each pattern
            for (Pattern pattern : this) {
                Element patternXML = pattern.getXMLRepresentation(xml);
                patternsXML.appendChild(patternXML);
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
