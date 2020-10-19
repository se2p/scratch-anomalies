package org.softevo.oumextractor;

import org.softevo.cmdline.CommandLine;
import org.softevo.cmdline.InputFormat;
import org.softevo.cmdline.SwitchMultiplicity;
import org.softevo.jutil.JavaUtil;
import org.softevo.oumextractor.modelcreator1.Analyzer;
import org.softevo.oumextractor.modelcreator1.ModelAnalyzer;
import org.softevo.oumextractor.modelcreator1.ModelData;
import org.softevo.oumextractor.modelcreator1.ModelVisitor;
import org.softevo.oumextractor.modelcreator1.model.Model;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.*;

/**
 * This is the main class of the part of the project, that creates models of object
 * usage based on static analysis.
 *
 * @author Andrzej Wasylkowski
 */
public final class OUMExtractor {

    /**
     * Logger to be used by this class.
     */
    private final static Logger logger =
            Logger.getLogger("org.softevo.oumextractor");

    /**
     * Number of classes present in the analyzed jar files.
     */
    private static int classesPresent = 0;

    /**
     * Number of methods present in the analyzed jar files.
     */
    private static int methodsPresent = 0;

    /**
     * Number of analyzed methods.
     */
    private static int methodsAnalyzed = 0;

    static {
        try {
            Handler handler = new FileHandler("OUMExtractor.log");
            handler.setFormatter(new SimpleFormatter());
            OUMExtractor.logger.addHandler(handler);
            OUMExtractor.logger.setLevel(Level.OFF);
        } catch (IOException e) {
            System.err.println("[ERROR] Couldn't open log file");
        }
    }

    /**
     * Creates new instance of this class.  It is to be used only internally, in
     * order to hold values extracted from the command-line.
     */
    private OUMExtractor() {
    }

    /**
     * This method should be called whenever analysis of a new class is
     * starting.  This ensures that the number of classes present is correct.
     */
    public static void newClassPresent() {
        classesPresent++;
    }

    /**
     * This method should be called whenever analysis of a new method is
     * starting.  This ensures that the number of methods present is correct.
     */
    public static void newMethodPresent() {
        methodsPresent++;
    }

    /**
     * This method should be called whenever analysis of a new method is
     * completed without errors.  This ensures that the number of methods
     * analyzed is correct.
     */
    public static void newMethodAnalyzed() {
        methodsAnalyzed++;
    }

    /**
     * Creates models of object usage based on static analysis.
     *
     * @param args List of command-line arguments
     */
    public static void main(String[] args) throws IOException {
        // create the analyzer object and run analysis according to command line
        OUMExtractor analyzer = new OUMExtractor();
        analyzer.runCommandLine(JavaUtil.removeEmptyArguments(args));
    }

    /**
     * Parses given command-line arguments and sets flags in this instance
     * accordingly.  Afterwards runs analysis as specified by the command-line.
     *
     * @param args List of command-line arguments
     */
    private void runCommandLine(String[] args) {
        // generate command line format

        CommandLine cmdLine = new CommandLine(this.getClass().getSimpleName());
        InputFormat create = new InputFormat("to create object " +
                "usage models based on static analysis");
        create.addSwitch("create",
                "makes OUMExtractor create object usage models",
                SwitchMultiplicity.ONE);
        create.addSwitch("libdirs", "directories with jar libraries, " +
                "separated with colons", SwitchMultiplicity.ZERO_OR_ONE, true);
        create.addSwitch("libs", "jar libraries, separated with colons",
                SwitchMultiplicity.ZERO_OR_ONE, true);
        create.setDataCount(2);
        create.setDataName(1, "jar_file");
        create.setDataName(2, "models_dir");
        cmdLine.addInputFormat("create", create);

        InputFormat createDir = new InputFormat("to create object " +
                "usage models based on static analysis");
        createDir.addSwitch("createdir",
                "makes OUMExtractor create object usage models",
                SwitchMultiplicity.ONE);
        createDir.addSwitch("libdirs", "directories with jar libraries, " +
                "separated with colons", SwitchMultiplicity.ZERO_OR_ONE, true);
        createDir.addSwitch("libs", "jar libraries, separated with colons",
                SwitchMultiplicity.ZERO_OR_ONE, true);
        createDir.setDataCount(2);
        createDir.setDataName(1, "jars_dir");
        createDir.setDataName(2, "models_dir");
        cmdLine.addInputFormat("createdir", createDir);

        InputFormat createFiles = new InputFormat("to create object " +
                "usage models based on static analysis");
        createFiles.addSwitch("createfiles",
                "makes OUMExtractor create object usage models",
                SwitchMultiplicity.ONE);
        createFiles.addSwitch("libdirs", "directories with jar libraries, " +
                "separated with colons", SwitchMultiplicity.ZERO_OR_ONE, true);
        createFiles.addSwitch("libs", "jar libraries, separated with colons",
                SwitchMultiplicity.ZERO_OR_ONE, true);
        createFiles.setDataCount(2);
        createFiles.setDataName(1, "class_files");
        createFiles.setDataName(2, "models_dir");
        cmdLine.addInputFormat("createFiles", createFiles);

        InputFormat output =
                new InputFormat("to output object usage models as .dot files");
        output.addSwitch("output",
                "makes OUMExtractor output object usage models as .dot " +
                        "files.  If a models file is specified, only models with ids " +
                        "specified in this file will be outputted.",
                SwitchMultiplicity.ONE);
        output.setDataRange(2, 3);
        output.setDataName(1, ".ser_files_dir");
        output.setDataName(2, "output_dir");
        output.setDataName(3, "models_file");
        cmdLine.addInputFormat("output", output);

        cmdLine.addHelpFormat();

        // parse command line
        if (cmdLine.parseCommandLine(args)) {
            if (cmdLine.getFormatName().equals("create")) {
                String jarName = cmdLine.getDataValue(1);
                String modelsDirName = cmdLine.getDataValue(2);
                List<String> libdirsNames = getLibdirsNames(cmdLine);
                List<String> libsNames = getLibsNames(cmdLine);
                createModelsFromJar(jarName, modelsDirName, libdirsNames,
                        libsNames);
            } else if (cmdLine.getFormatName().equals("createdir")) {
                String jarDirName = cmdLine.getDataValue(1);
                String modelsDirName = cmdLine.getDataValue(2);
                List<String> libdirsNames = getLibdirsNames(cmdLine);
                List<String> libsNames = getLibsNames(cmdLine);
                createModelsFromDir(jarDirName, modelsDirName, libdirsNames,
                        libsNames);
            } else if (cmdLine.getFormatName().equals("createFiles")) {
                String classFiles = cmdLine.getDataValue(1);
                String modelsDirName = cmdLine.getDataValue(2);
                List<String> libdirsNames = getLibdirsNames(cmdLine);
                List<String> libsNames = getLibsNames(cmdLine);
                createModelsFromClasses(classFiles, modelsDirName,
                        libdirsNames, libsNames);
            } else if (cmdLine.getFormatName().equals("output")) {
                String serDirName = cmdLine.getDataValue(1);
                String outputDirName = cmdLine.getDataValue(2);
                String modelsFilename = null;
                if (cmdLine.getDataCount() >= 3) {
                    modelsFilename = cmdLine.getDataValue(3);
                }
                outputModels(serDirName, outputDirName, modelsFilename);
            } else {
                cmdLine.processOtherFormats();
            }
        } else {
            System.err.println(cmdLine.getErrorString());
            cmdLine.printShortHelp();
        }
    }

    /**
     * Performs static analysis for all given classes and saves
     * object usage models in given directory.
     *
     * @param types     Fully qualified names of classes to be analyzed.
     * @param modelsDir Directory to save models into.
     */
    private void createClassesModels(Set<String> types, String modelsDir) {
        try {
            // read the classes to memory
            JavaClassPool pool = JavaClassPool.get();
            System.out.print("Reading classes to memory...");
            try {
                for (String typeName : types) {
                    pool.getType(typeName, true);
                }
            } catch (OutOfMemoryError e) {
            }
            System.out.println("OK");

            // perform the analysis
            Analyzer analyzer = new Analyzer(modelsDir, types);
            int typesProcessed = 0;
            for (String typeName : types) {
                typesProcessed++;
                int percentProcessed =
                        (100 * typesProcessed) / types.size();
                System.out.println("Analyzing " + typesProcessed + "/" +
                        types.size() + " (" + percentProcessed +
                        "% done): " + typeName);
                JavaType type = pool.getType(typeName, true);
                if (type instanceof JavaClass) {
                    OUMExtractor.newClassPresent();
                    JavaClass clas = (JavaClass) type;
                    clas.createCFGRepresentation();
                    clas.analyzeDataFlow(analyzer);
                    analyzer.serializeModels();
                }
            }
            analyzer.shutdownAnalysis();
            pool.outputMissingTypes();
            System.out.println("Classes present: " +
                    OUMExtractor.classesPresent);
            System.out.println("Methods analyzed/present: " +
                    OUMExtractor.methodsAnalyzed + " / " +
                    OUMExtractor.methodsPresent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Performs static analysis for all classes in the given jar files and saves
     * object usage models in given directory.
     *
     * @param jars      Jar files, whose classes are to be analyzed.
     * @param modelsDir Directory to save models into.
     * @param libs      Jar files to use as libraries during analysis.
     */
    private void createModels(Set<JarFile> jars, String modelsDir,
                              List<JarFile> libs) {
        JavaClassPool pool = JavaClassPool.get();

        // get all types from the jar files
        Set<String> typesToProcess = new HashSet<String>();
        for (JarFile jar : jars) {
            System.out.print("Extracting classes to analyze from " +
                    new File(jar.getName()).getName() + "...");
            pool.addClassLoader(new JarClassLoader(new File(jar.getName())));
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                JarEntry entry = e.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String typeName = entry.getName().replace('/', '.').
                            substring(0, entry.getName().length() - 6);
                    if (!typesToProcess.contains(typeName)) {
                        System.out.println("TYPENAME " + typeName);
                        typesToProcess.add(typeName);
                    }
                }
            }
            try {
                jar.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
            System.out.println("OK");
        }

        // add loaders for all libraries
        for (JarFile jarFile : libs) {
            pool.addClassLoader(new JarClassLoader(new File(jarFile.getName())));
        }

        createClassesModels(typesToProcess, modelsDir);
    }

    /**
     * Creates models from a given jar file.
     *
     * @param jarName       Jar file to create models from.
     * @param modelsDirName Directory to put models into.
     * @param libdirsNames  Names of directories with libraries to use
     *                      during the analysis.
     * @param libsNames     Names of library files to use during
     *                      the analysis.
     */
    private void createModelsFromJar(String jarName, String modelsDirName,
                                     List<String> libdirsNames, List<String> libsNames) {
        List<JarFile> libs = extractLibs(libdirsNames, libsNames);
        File jarFile = new File(jarName);
        try {
            JarFile jar = new JarFile(jarFile);
            Set<JarFile> jars = new HashSet<JarFile>();
            jars.add(jar);
            createModels(jars, modelsDirName, libs);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Creates models from a given directory with jar files.
     *
     * @param jarDirName    Directory with jar files to create models from.
     * @param modelsDirName Directory to put models into.
     * @param libdirsNames  Names of directories with libraries to use
     *                      during the analysis.
     * @param libsNames     Names of library files to use during
     *                      the analysis.
     */
    private void createModelsFromDir(String jarDirName, String modelsDirName,
                                     List<String> libdirsNames, List<String> libsNames) {
        List<JarFile> libs = extractLibs(libdirsNames, libsNames);
        try {
            Set<JarFile> jars = new HashSet<JarFile>();
            File jarDir = new File(jarDirName);
            for (File jarFile : jarDir.listFiles()) {
                if (jarFile.getName().endsWith(".jar")) {
                    jars.add(new JarFile(jarFile));
                }
            }
            createModels(jars, modelsDirName, libs);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Creates models from a list of given directories with .class files.
     *
     * @param classFiles    List of directories with .class files (with
     *                      directories separated with ":").
     * @param modelsDirName Directory to put models into.
     * @param libdirsNames  Names of directories with libraries to use
     *                      during the analysis.
     * @param libsNames     Names of library files to use during
     *                      the analysis.
     */
    private void createModelsFromClasses(String classFiles,
                                         String modelsDirName, List<String> libdirsNames,
                                         List<String> libsNames) {
        JavaClassPool pool = JavaClassPool.get();
        Set<String> typesToProcess = new HashSet<String>();
        StringTokenizer tokenizer = new StringTokenizer(classFiles, ":");
        while (tokenizer.hasMoreTokens()) {
            File dir = new File(tokenizer.nextToken());
            typesToProcess.addAll(getClasses(dir));
        }
        List<JarFile> libs = extractLibs(libdirsNames, libsNames);
        for (JarFile jarFile : libs) {
            pool.addClassLoader(new JarClassLoader(new File(jarFile.getName())));
        }

        createClassesModels(typesToProcess, modelsDirName);
    }

    /**
     * Outputs object usage models from given directory as .dot files.
     *
     * @param serDir         Directory to read models from.
     * @param outputDir      Output directory to put .dot files into.
     * @param modelsFilename Name of a file, which holds numbers of models
     *                       to be outputted.  If this is <code>null</code>,
     *                       all models will be outputted.
     */
    private void outputModels(String serDirName, String outputDirName,
                              String modelsFilename) {
        final Set<Integer> modelsToOutput = new HashSet<Integer>();

        // read numbers of models
        if (modelsFilename != null) {
            try {
                File modelsFile = new File(modelsFilename);
                BufferedReader reader =
                        new BufferedReader(new FileReader(modelsFile));
                String line = reader.readLine();
                while (line != null) {
                    Integer id = Integer.valueOf(line);
                    modelsToOutput.add(id);
                    line = reader.readLine();
                }
                reader.close();
            } catch (IOException e) {
                System.err.println("[ERROR] Couldn't read from file: " +
                        modelsFilename);
                e.printStackTrace(System.err);
                return;
            }
        }

        // create and clear output directory
        final File outputDir = new File(outputDirName);
        outputDir.mkdirs();
        for (File file : outputDir.listFiles()) {
            file.delete();
        }

        // output models
        ModelAnalyzer analyzer = new ModelAnalyzer(new File(serDirName));
        analyzer.analyzeModels(new ModelVisitor() {
            public void visit(int id, Model model, ModelData data) {
                if (!modelsToOutput.isEmpty() && !modelsToOutput.contains(id)) {
                    return;
                }

                String dotFilename = id + ".model.dot";
                try {
                    model.saveToDotFile(new File(outputDir, dotFilename));
                } catch (FileNotFoundException e) {
                    e.printStackTrace(System.err);
                    throw new InternalError();
                }
            }
        });
    }

    /**
     * Returns all classes from given directory.
     *
     * @param file Root of all classes.
     * @return All classes from given directory.
     */
    public Set<String> getClasses(File file) {
        Set<String> result = new HashSet<String>();
        getClasses(result, file, "");
        return result;
    }

    /**
     * Adds to given set of class names all classes from given directory,
     * prefixing their names with given prefix.
     *
     * @param classes Set of names.
     * @param file    Directory.
     * @param prefix  Prefix to be added to names of found classes.
     */
    public void getClasses(Set<String> classes, File file, String prefix) {
        JavaClassPool pool = JavaClassPool.get();
        for (File f : file.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".class")) {
                String filename = f.getName();
                String className = filename.substring(0, filename.length() - 6);
                classes.add(prefix + className);
                pool.addClassLoader(new FileClassLoader(f));
            } else if (f.isDirectory()) {
                getClasses(classes, f, prefix + f.getName() + ".");
            }
        }
    }

    /**
     * Extracts the list of names of library directories from the command line
     * options.
     *
     * @param cmdLine Command line to extract the names from.
     * @return Set of names of library directories.
     */
    private List<String> getLibdirsNames(CommandLine cmdLine) {
        List<String> libdirsNames = new LinkedList<String>();
        if (cmdLine.getSwitchCount("libdirs") > 0) {
            for (String libdirsSet : cmdLine.getSwitchValues("libdirs")) {
                for (String libdirName : libdirsSet.split(":")) {
                    libdirsNames.add(libdirName);
                }
            }
        }
        return libdirsNames;
    }

    /**
     * Extracts the list of names of libraries from the command line options.
     *
     * @param cmdLine Command line to extract the names from.
     * @return Set of names of libraries.
     */
    private List<String> getLibsNames(CommandLine cmdLine) {
        List<String> libsNames = new LinkedList<String>();
        if (cmdLine.getSwitchCount("libs") > 0) {
            for (String libsSet : cmdLine.getSwitchValues("libs")) {
                for (String libName : libsSet.split(":")) {
                    libsNames.add(libName);
                }
            }
        }
        return libsNames;
    }

    /**
     * Extracts the set of library files from the given library directories
     * and libraries names.
     *
     * @param libdirsNames Names of directories with libraries to extract.
     * @param libsNames    Names of libraries to extract.
     * @return Set of library files.
     */
    private List<JarFile> extractLibs(List<String> libdirsNames,
                                      List<String> libsNames) {
        List<JarFile> libs = new LinkedList<JarFile>();

        // extract libraries based on names
        for (String libName : libsNames) {
            File file = new File(libName);
            try {
                libs.add(new JarFile(file));
            } catch (IOException e) {
                System.err.println("[WARNING] Could not open " +
                        "the file: " + file);
            }
        }

        // extract libraries from directories
        for (String libdirName : libdirsNames) {
            File libdir = new File(libdirName);
            for (File file : libdir.listFiles()) {
                if (file.getName().endsWith(".jar")) {
                    try {
                        libs.add(new JarFile(file));
                    } catch (IOException e) {
                        System.err.println("[WARNING] Could not open " +
                                "the file: " + file);
                    }
                }
            }
        }

        return libs;
    }
}
