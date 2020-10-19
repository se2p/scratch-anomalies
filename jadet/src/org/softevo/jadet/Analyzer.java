package org.softevo.jadet;


import main.LWP;
import org.softevo.catools.Anomaly;
import org.softevo.catools.CAMatrix;
import org.softevo.catools.CAMatrixEntry;
import org.softevo.catools.CAObject;
import org.softevo.catools.CAProperty;
import org.softevo.catools.Pattern.PatternComparator;
import org.softevo.jadet.sca.EventPair;
import org.softevo.jadet.sca.Method;
import org.softevo.jadet.sca.OutputVerbosity;
import org.softevo.jadet.sca.Pattern;
import org.softevo.jadet.sca.PatternsList;
import org.softevo.jadet.sca.SCAAbstractor;
import org.softevo.jadet.sca.Violation;
import org.softevo.jadet.sca.Violation.ViolationComparator;
import org.softevo.jadet.sca.ViolationsList;
import org.softevo.jutil.Pair;
import org.softevo.jutil.tasks.Task;
import org.softevo.oumextractor.modelcreator1.ModelAnalyzer;
import org.softevo.oumextractor.modelcreator1.ModelData;
import org.softevo.oumextractor.modelcreator1.ModelVisitor;
import org.softevo.oumextractor.modelcreator1.model.Model;
import org.softevo.oumextractor.modelcreator1.model.Transition;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;


/**
 * This class is responsible for dealing with sequential constraints
 * abstractions.
 *
 * @author Andrzej Wasylkowski
 */
public class Analyzer {

    /**
     * List of patterns mined, ordered by the support. If <code>null</code>,
     * mining was not performed.
     */
    private PatternsList patterns;


    /**
     * List of violations mined, ordered by the defect indicator.
     * If <code>null</code>, mining was not performed.
     */
    private ViolationsList violations;


    /**
     * Mapping from a set of properties to their support in the matrix.
     */
    private Map<Set<EventPair>, Integer> properties2support;


    /**
     * Creates a new analyzer.
     */
    public Analyzer() {
        this.patterns = null;
        this.violations = null;
        this.properties2support = new HashMap<Set<EventPair>, Integer>();
    }


    /**
     * Mines patterns from the models or given core files.
     *
     * @param modelsDirName Directory with the models to analyze (optional).
     * @param inputName     Core name of files with sca (optional).
     * @param minSupport    Minimum support parameter to use when mining.
     * @param minSize       Minimum size parameter to use when mining.
     */
    public void minePatterns(String modelsDirName, String inputName,
                             int minSupport, int minSize) {
        // create the concept analysis matrix
        CAMatrix<Method, EventPair> matrix = createCAMatrix(modelsDirName,
                inputName);

        // mine the patterns
        Task task = new Task("Mining patterns");
        Set<org.softevo.catools.Pattern<Method, EventPair>> patterns =
                matrix.minePatterns(minSupport, minSize, 0);
        this.patterns = new PatternsList();
        for (org.softevo.catools.Pattern<Method, EventPair> pattern : patterns)
            this.patterns.add(new Pattern(pattern));
        Collections.sort(this.patterns, new PatternComparator<Method, EventPair>());
        task.addMessage(this.patterns.size() + " patterns found");
        task.done();
    }


    /**
     * Mines violations from the models or given core files.
     *
     * @param modelsDirName Directory with the models to analyze (optional).
     * @param inputName     Core name of files with sca (optional).
     * @param minSupport    Minimum support parameter to use.
     * @param minSize       Minimum size parameter to use.
     * @param maxDevLevel   Maximum deviation level parameter to use.
     * @param minConfidence Minimum confidence parameter to use.
     */
    public void mineViolations(String modelsDirName, String inputName,
                               int minSupport, int minSize, int maxDevLevel,
                               double minConfidence) {
        // create the concept analysis matrix
        CAMatrix<Method, EventPair> matrix = createCAMatrix(modelsDirName,
                inputName);

        // mine the violations
        Task task = new Task("Mining violations");
        Set<Anomaly<Method, EventPair>> anomalies = matrix.mineAnomalies(
                minSupport, minSize, maxDevLevel, minConfidence, 3);
        this.violations = new ViolationsList();
        for (Anomaly<Method, EventPair> anomaly : anomalies) {
            for (org.softevo.catools.Violation<Method, EventPair> cavio : anomaly.getViolations()) {
                Violation violation = new Violation(cavio);
                violations.add(violation);
            }
        }
        task.addMessage(this.violations.size() + " violations found");
        task.done();

        // ranking and filtering the violations
        task = new Task("Ranking and filtering violations");
        rankViolations(matrix);
        filterViolations();
        task.addMessage(this.violations.size() + " violations reported");
        task.done();
    }


    /**
     * Filters the violations.
     */
    private void filterViolations() {
        // remove violations that have a conviction value no greater than 1.25
        ListIterator<Violation> iterator =
                this.violations.listIterator(this.violations.size());
        while (iterator.hasPrevious()) {
            Violation violation = iterator.previous();
            if (violation.getDefectIndicator() > 1.25) break;
            iterator.remove();
        }

        // remove violations with subsets that have at least as high conviction
        // value (i.e., if A => B is a violation, and there is a violation
        // C => B such that C is a subset of A and conviction of C => B is at
        // least as high as conviction of A => B, remove A => B)
        Set<Violation> toRemove = new HashSet<Violation>();
        for (Violation refViolation : this.violations) {
            if (toRemove.contains(refViolation)) continue;
            Set<EventPair> refMissing = refViolation.getMissingProperties();
            Set<EventPair> refPresent = new HashSet<EventPair>(
                    refViolation.getPattern().getProperties());
            refPresent.removeAll(refMissing);
            for (Violation checkedViolation : this.violations) {
                if (checkedViolation == refViolation) continue;
                if (toRemove.contains(checkedViolation)) continue;
                Set<EventPair> checkedMissing =
                        checkedViolation.getMissingProperties();
                if (!refMissing.equals(checkedMissing)) continue;
                Set<EventPair> checkedPresent = new HashSet<EventPair>(
                        checkedViolation.getPattern().getProperties());
                checkedPresent.removeAll(checkedMissing);
                if (checkedPresent.containsAll(refPresent) &&
                        refViolation.getDefectIndicator() >= checkedViolation.getDefectIndicator())
                    toRemove.add(checkedViolation);
            }
        }
        this.violations.removeAll(toRemove);
    }


    /**
     * Ranks the violations and orders them from the best- to worst-ranked.
     *
     * @param matrix Concept analysis matrix used to find the violations.
     */
    private void rankViolations(CAMatrix<Method, EventPair> matrix) {
        for (Violation violation : this.violations) {
            double conviction = calculateViolationConviction(matrix, violation);
            violation.setDefectIndicator(conviction);
        }
        Collections.sort(this.violations, new ViolationComparator());
    }


    /**
     * Outputs patterns mined earlier in a human-readable format into the
     * given file.
     *
     * @param outFile   File to output patterns to.
     * @param verbosity Verbosity of the output.
     */
    public void outputPatterns(File outFile, OutputVerbosity verbosity) {
        Task task = new Task("Outputting patterns");

        // check that the patterns have been mined
        if (this.patterns == null) {
            System.out.println("Patterns have not been mined yet; cannot " +
                    "output them");
            return;
        }

        // assign ids to patterns (from 1 to #patterns)
        Map<Integer, Pattern> id2pattern = new HashMap<Integer, Pattern>();
        for (Pattern pattern : this.patterns) {
            id2pattern.put(id2pattern.size() + 1, pattern);
        }

        // output all the patterns
        try {
            PrintWriter out = new PrintWriter(outFile);

            // output general statistics
            out.println("==================================================");
            out.println("    STATISTICS");
            out.println("==================================================");
            out.println();
            outputPatternsStatistics(out);
            out.println();

            // output the patterns only
            out.println("==================================================");
            out.println("    PATTERNS (SUMMARY)");
            out.println("==================================================");
            out.println();
            for (int id = 1; id <= this.patterns.size(); id++) {
                Pattern pattern = id2pattern.get(id);
                List<EventPair> properties =
                        new ArrayList<EventPair>(pattern.getProperties());
                Collections.sort(properties);
                out.println("==================================================");
                out.println("    Pattern #" + id);
                out.println("==================================================");
                out.println("Support: " + pattern.getSupport());
                out.println("--------------------------------------------------");
                out.println("Properties (" + pattern.getProperties().size() + "):");
                out.println("--------------------------------------------------");
                for (EventPair pair : properties) {
                    out.println(pair.getTextRepresentation(verbosity));
                }
                out.println("--------------------------------------------------");
            }

            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace(System.err);
            return;
        }

        task.done();
    }


    /**
     * Outputs patterns mined earlier in an XML format into the given file.
     *
     * @param outFile File to output patterns to.
     */
    public void outputPatternsXML(File outFile) {
        // check that the patterns have been mined
        if (this.patterns == null) {
            System.out.println("Patterns have not been mined yet; cannot " +
                    "output them");
            return;
        }

        Task task = new Task("Outputting patterns");
        this.patterns.writeXML(outFile);
        task.done();
    }


    /**
     * Outputs violations mined earlier in an XML format into the given file.
     *
     * @param outFile File to output patterns to.
     */
    public void outputViolationsXML(File outFile) {
        Task task = new Task("Outputting violations");
        this.violations.writeXML(outFile);
        task.done();
    }


    /**
     * Calculates and returns the lift measure of the given violation.
     *
     * @param matrix    Concept analysis matrix.
     * @param violation Violation found in the matrix.
     * @return Lift measure of the violation.
     */
    @SuppressWarnings("unused")
    private <O extends CAObject, P extends CAProperty>
    double calculateViolationLift(CAMatrix<O, P> matrix,
                                  org.softevo.catools.Violation<O, P> violation) {
        // transform the violation into an association rule a->b
        Set<P> a = new HashSet<P>(violation.getPattern().getProperties());
        a.removeAll(violation.getMissingProperties());
        Set<P> b = violation.getMissingProperties();

        // get the number of all transactions
        double allTransactionsNum = (double) matrix.getEntries().size();

        // get the number of transactions that contain both a and b (this is
        // simply the violated pattern's support)
        double abTransactionsNum = (double) violation.getPattern().getSupport();

        // get the number of transactions that contain a
        double aTransactionsNum = (double) matrix.getSupport(a);

        // get the number of transactions that contain b
        double bTransactionsNum = (double) matrix.getSupport(b);

        // calculate the lift measure
        double lift = allTransactionsNum *
                (abTransactionsNum / (aTransactionsNum * bTransactionsNum));
        return lift;
    }


    /**
     * Calculates and returns the conviction measure of the given violation.
     *
     * @param matrix    Concept analysis matrix.
     * @param violation Violation found in the matrix.
     * @return Conviction measure of the violation.
     */
    private double calculateViolationConviction(CAMatrix<Method, EventPair> matrix,
                                                org.softevo.catools.Violation<Method, EventPair> violation) {
        // transform the violation into an association rule a->b
        Set<EventPair> a = new HashSet<EventPair>(
                violation.getPattern().getProperties());
        a.removeAll(violation.getMissingProperties());
        Set<EventPair> b = violation.getMissingProperties();

        // get the number of all transactions
        double allTransactionsNum = (double) matrix.getEntries().size();

        // get the number of transactions that contain b
        if (!this.properties2support.containsKey(b)) {
            this.properties2support.put(b, matrix.getSupport(b));
        }
        double bTransactionsNum = this.properties2support.get(b);

        // get the confidence of the violation (= association rule)
        double confidence = violation.getConfidence();

        // calculate the conviction measure
        double conviction = ((allTransactionsNum - bTransactionsNum) / allTransactionsNum) /
                (1 - confidence);
        return conviction;
    }


    /**
     * Outputs statistics information about patterns into the given stream.
     *
     * @param out Stream to output information to.
     */
    private void outputPatternsStatistics(PrintWriter out) {
        // output all the statistics
        out.println("# of patterns: " + this.patterns.size());
    }


    /**
     * Creates a concept analysis matrix out of the models or given input files.
     *
     * @param modelsDirName Directory with the models to analyze (optional).
     * @param inputName     Core name of files with sca (optional).
     * @return Concept analysis matrix.
     */
    private CAMatrix<Method, EventPair> createCAMatrix(String modelsDirName,
                                                       String inputName) {
        assert modelsDirName != null || inputName != null;
        assert modelsDirName == null || inputName == null;

        // get the sequential constraints abstraction of each method
        Map<Method, Set<EventPair>> method2sca;
        Task task;
        if (modelsDirName != null) {
            task = new Task("Creating the sequential constraints abstraction");
            method2sca = getMethodsSCA(new File(modelsDirName), true);
        } else {
            assert inputName != null;
            task = new Task("Reading the sequential constraints abstraction");
            method2sca = readMethodsSCA(inputName);
        }
        int eventPairsNum = 0;
        for (Method m : method2sca.keySet()) {
            eventPairsNum += method2sca.get(m).size();
        }
        task.addMessage("Total of " + eventPairsNum +
                " event pairs in the abstraction");
        task.done();

        // create a concept analysis matrix
        task = new Task("Creating the concept analysis matrix");
        Set<CAMatrixEntry<Method, EventPair>> entries =
                new HashSet<CAMatrixEntry<Method, EventPair>>();
        for (Method method : method2sca.keySet()) {
            Set<EventPair> sca = method2sca.get(method);
            CAMatrixEntry<Method, EventPair> entry =
                    new CAMatrixEntry<Method, EventPair>(method, sca);
            entries.add(entry);
        }
        CAMatrix<Method, EventPair> matrix =
                new CAMatrix<Method, EventPair>(entries);
        entries.clear();    // conserve memory
        entries = null;        // conserve memory
        task.addMessage(matrix.getEntries().size() + " entries");
        task.done();
        return matrix;
    }


    /**
     * Calculates and returns the sequential constraints abstraction of each
     * method that has been analyzed and has models stored in the given
     * directory.
     *
     * @param modelsDir Directory with object usage models.
     * @param filter    Indicates if filtering of constraints should be done.
     */
    public Map<Method, Set<EventPair>> getMethodsSCA(File modelsDir,
                                                     final boolean filter) {
        ModelAnalyzer analyzer = new ModelAnalyzer(modelsDir, false);
        final Map<Method, Set<EventPair>> method2sca =
                new HashMap<Method, Set<EventPair>>();
        analyzer.analyzeModels(new ModelVisitor() {
            public void visit(int id, Model model, ModelData modelData) {
                String fullMethodName = modelData.getClassName() + " " +
                        modelData.getMethodName();
                Method method = Method.get(fullMethodName, true);
                if (!method2sca.containsKey(method)) {
                    method2sca.put(method, new HashSet<EventPair>());
                }
                Set<EventPair> sca = method2sca.get(method);
                sca.addAll(SCAAbstractor.getSCAAbstraction(model, filter));
            }
        });
        return method2sca;
    }


    /**
     * Reads the sequential constraints abstraction from the three ".matrix",
     * ".funcs" and ".scs" files with the given core name. The files are
     * assumed to be created by the lightweight parser.
     *
     * @param inputName Core name of the files.
     */
    private Map<Method, Set<EventPair>> readMethodsSCA(String inputName) {
        Map<String, Set<Pair<Transition, Transition>>> method2constraints =
                LWP.readSCA(inputName);
        Map<Method, Set<EventPair>> method2sca =
                new HashMap<Method, Set<EventPair>>();
        for (String methodName : method2constraints.keySet()) {
            Method method = Method.get(methodName, false);
            Set<EventPair> events = new HashSet<EventPair>();
            for (Pair<Transition, Transition> constraint :
                    method2constraints.get(methodName)) {
                EventPair pair = EventPair.get(constraint.getFirst(),
                        constraint.getSecond());
                events.add(pair);
            }
            method2sca.put(method, events);
        }
        return method2sca;
    }
}
