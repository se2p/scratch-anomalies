package org.softevo.jadet;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * This class is responsible for creating visualization of patterns.
 *
 * @author Andrzej Wasylkowski
 */
public class PatternVisualizer {

    /**
     * Indicates if thesis-trimming should be done.
     */
    private boolean thesis;


    /**
     * Creates a new patterns analyzer.
     *
     * @param thesis Indicates if thesis-trimming should be done.
     */
    public PatternVisualizer(boolean thesis) {
        this.thesis = thesis;
    }


    /**
     * Returns indicator if thesis-trimming should be done.
     *
     * @return    <code>true</code> if thesis-trimming should be done;
     * <code>false</code> otherwise.
     */
    public boolean isThesis() {
        return this.thesis;
    }


    /**
     * Visualizes the pattern from the given input file and puts its
     * dot-visualization in the given output file.
     *
     * @param dotFile     File to put the visualization to.
     * @param patternFile File with the pattern to visualize.
     */
    public void visualize(File dotFile, File patternFile) {
        try {
            // read the pattern
            Set<SequentialConstraint> pattern = readPattern(patternFile);
            savePatternToDotFile(dotFile, pattern);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        }
    }

    private void savePatternToDotFile(File dotFile, Set<SequentialConstraint> pattern) throws IOException {
        // assign ids to elements of sequential constraints
        Map<String, Integer> element2id = new HashMap<String, Integer>();
        for (SequentialConstraint constraint : pattern) {
            if (!element2id.containsKey(constraint.left))
                element2id.put(constraint.left, element2id.size() + 1);
            if (!element2id.containsKey(constraint.right))
                element2id.put(constraint.right, element2id.size() + 1);
        }

        // create the textual representation of the pattern
        StringBuffer repr = new StringBuffer("digraph {\n");
        repr.append("bgcolor=\"white\";\n");
        for (String element : element2id.keySet()) {
            int id = element2id.get(element);
            repr.append("\"").append(id).append("\"");
            repr.append(" [shape=\"plaintext\",");
            repr.append("label=\"").append(element).append("\",");
            repr.append("fontname=txtt,fontsize=14.0];\n");
        }
        for (SequentialConstraint constraint : pattern) {
            int leftId = element2id.get(constraint.left);
            int rightId = element2id.get(constraint.right);
            repr.append("\"").append(leftId).append("\"");
            repr.append("->");
            repr.append("\"").append(rightId).append("\"");
            repr.append(";\n");
        }
        repr.append("}\n");

        // write out the textual representation of the pattern
        BufferedWriter out = new BufferedWriter(new FileWriter(dotFile));
        out.write(repr.toString());
        out.close();
    }


    /**
     * Reads a pattern from the given file.
     *
     * @param file File to read a pattern from.
     * @throws IOException
     * @return Pattern read from the file.
     */
    private Set<SequentialConstraint> readPattern(File file) throws IOException {
        Set<SequentialConstraint> pattern =
                new HashSet<SequentialConstraint>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null)
            pattern.add(new SequentialConstraint(line));
        reader.close();
        return pattern;
    }

    public void visualizeAll(File dotFolder, File patternFile) throws IOException {
        if (!dotFolder.isDirectory()) {
            System.err.println("The dot argument must be a folder path, but is " + dotFolder.getAbsolutePath());
            return;
        }
        Set<SequentialConstraint> pattern =
                new HashSet<SequentialConstraint>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(patternFile));
        } catch (FileNotFoundException e) {
            System.err.println("Could not read from file " + patternFile.getAbsolutePath());
        }
        boolean start = true;
        String line;
        int generatedPatterns = 0;
        while ((line = reader.readLine()) != null) {
            if (line.contains("<")) {
                start = false;
                pattern.add(new SequentialConstraint(line));
            } else {
                if (!start) {
                    generatedPatterns++;
                    File patternDotFile = new File(dotFolder, "pattern" + generatedPatterns + ".dot");
                    savePatternToDotFile(patternDotFile, pattern);
                    pattern = new HashSet<SequentialConstraint>();
                    start = true;
                }
            }
        }
        reader.close();
    }


    /**
     * Instances of this class represent sequential constraints with no
     * semantics.
     *
     * @author Andrzej Wasylkowski
     */
    private class SequentialConstraint {

        /**
         * Left side of the sequential constraint.
         */
        String left;

        /**
         * Right side of the sequential constraint.
         */
        String right;

        /**
         * Creates a new sequential constraint out of given representation.
         *
         * @param str Representation of the constraint to create (in the form
         *            "a < b")
         */
        SequentialConstraint(String str) {
            int lessThanIndex = str.indexOf(" < ");
            if (lessThanIndex == -1)
                throw new IllegalArgumentException("String: '" + str +
                        "' is not a valid sequential constraint representation.");
            this.left = str.substring(0, lessThanIndex).trim();
            this.right = str.substring(lessThanIndex + 3).trim();
            if (isThesis()) {
                this.left = thesisTrim(this.left);
                this.right = thesisTrim(this.right);
            }
        }

        /**
         * Thesis-trims the given event (by removing the signature)
         *
         * @param event Event to thesis-trim.
         * @return Thesis-trimmed event.
         */
        private String thesisTrim(String event) {
            int leftParenIndex = event.indexOf(" (");
            if (leftParenIndex == -1)
                return event;
            String leftSide = event.substring(0, leftParenIndex).trim();
            int rightParenIndex = event.indexOf(") : ", leftParenIndex);
            int spaceIndex = event.indexOf(' ', rightParenIndex + 4);
            String rightSide = "";
            if (spaceIndex != -1)
                rightSide = event.substring(spaceIndex).trim();
            return leftSide + " " + rightSide;
        }
    }
}
