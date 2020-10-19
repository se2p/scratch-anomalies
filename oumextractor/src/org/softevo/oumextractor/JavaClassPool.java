package org.softevo.oumextractor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * This class is used to represent a pool of analyzed Java types.  It follows
 * a singleton design pattern.
 *
 * @author Andrzej Wasylkowski
 */
public final class JavaClassPool {

    /**
     * This is the only instance of the pool.
     */
    private final static JavaClassPool pool = new JavaClassPool();

    /**
     * Mapping fully qualified type name => its representation.
     */
    private final Map<String, JavaType> types;

    /**
     * Mapping class name => directly derived classes' names.
     */
    private final Map<String, Set<String>> derivedClasses;

    /**
     * Mapping class name => all direct base classes' and interfaces' names.
     */
    private final Map<String, Set<String>> supertypes;

    /**
     * Set of names of types that were needed but were not found.
     */
    private final Set<String> missingTypes;

    /**
     * Mapping from classes' names to loaders.
     */
    private final Map<String, ClassLoader> className2loader;

    /**
     * Creates new instance of the class pool.
     */
    private JavaClassPool() {
        System.err.println("initialising");
        this.types = new HashMap<String, JavaType>();
        this.derivedClasses = new HashMap<String, Set<String>>();
        this.supertypes = new HashMap<String, Set<String>>();
        this.missingTypes = new HashSet<String>();
        this.className2loader = new HashMap<String, ClassLoader>();
        addSystemClassLoaders();
    }

    /**
     * Returns an instance of the class pool.
     *
     * @return An instance of the class pool.
     */
    public static JavaClassPool get() {
        return JavaClassPool.pool;
    }

    /**
     * Returns representation of the type of a given fully qualified name.
     *
     * @param name             Fully qualified name of the type.
     * @param getMethodsBodies If <code>true</code> methods bodies will be
     *                         stored; otherwise not.
     * @return Representation of the type wanted.
     * @throws ClassNotFoundException if a type can not be found
     */
    public JavaType getType(String name, boolean getMethodsBodies)
            throws ClassNotFoundException {
        if (!this.types.containsKey(name)) {
            createTypeRepresentation(name, getMethodsBodies);
        }
        JavaType type = this.types.get(name);
        if (getMethodsBodies && !type.hasMethodsBodies()) {
            createTypeRepresentation(name, getMethodsBodies);
            type = this.types.get(name);
        }
        return type;
    }

    /**
     * Adds a given class loader to this pool. If there is more than one loader
     * added for a given class, the first loader given will be used by the
     * pool.
     *
     * @param loader Loader to use by the pool.
     */
    public void addClassLoader(ClassLoader loader) {
        for (String className : loader.getClassesNames()) {
            if (this.className2loader.containsKey(className)) {
                ClassLoader oldLoader = this.className2loader.get(className);
                if (!equalRepresentations(className, loader, oldLoader)) {
                    if (loader instanceof JarClassLoader &&
                            oldLoader instanceof JarClassLoader) {
                        String javaJarsDirsProperty =
                                System.getProperty("org.softevo.oumextractor.javajarsdirs");
                        JarClassLoader l1 = (JarClassLoader) loader;
                        JarClassLoader l2 = (JarClassLoader) oldLoader;
                        if (l1.getFilename().startsWith(javaJarsDirsProperty) &&
                                l2.getFilename().startsWith(javaJarsDirsProperty)) {
                            continue;
                        }
                    }
                    System.err.println("[ERROR] Class " + className +
                            " has two different representations when using " +
                            "loaders " + loader + " and " + oldLoader);
//					System.exit (1);
                }
            } else {
                this.className2loader.put(className, loader);
            }
        }
    }

    /**
     * Outputs names of all classes that were requested but not found.
     */
    public void outputMissingTypes() {
        System.out.println("Missing types:");
        List<String> missingTypesNames =
                new ArrayList<String>(this.missingTypes);
        Collections.sort(missingTypesNames);
        for (String name : missingTypesNames) {
            System.out.println(" - " + name);
        }
    }

    /**
     * Returns names of all base classes' and interfaces' (including indirect
     * ones) of type of given name.
     *
     * @param name Name of the type to get base types of.
     * @return Names of base types of type of given name.
     */
    public Set<String> getBaseTypesNames(String name) {
        if (!this.supertypes.containsKey(name)) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<String>();
        result.addAll(this.supertypes.get(name));
        for (String baseName : this.supertypes.get(name)) {
            result.addAll(getBaseTypesNames(baseName));
        }
        return result;
    }

    /**
     * Returns set of classes derived from class of given fully qualified name
     * that are present in this pool.
     *
     * @param name             Fully qualified name of a base class.
     * @param getMethodsBodies If <code>true</code> methods bodies will be
     *                         stored; otherwise not.
     * @return Set of classes derived from class of given name.
     * @throws ClassNotFoundException if some class can not be found
     */
    Set<JavaClass> getDerivedClasses(String name, boolean getMethodsBodies)
            throws ClassNotFoundException {
        // make sure this is called for a class and not an interface
        JavaType type = getType(name, getMethodsBodies);
        if (type instanceof JavaInterface) {
            throw new IllegalArgumentException();
        }

        // get direct subclasses of the investigated class
        Queue<String> toProcess = new LinkedList<String>();
        for (String className : this.derivedClasses.get(name)) {
            toProcess.offer(className);
        }

        // get all subclasses of the investigated class
        Set<JavaClass> result = new HashSet<JavaClass>();
        while (!toProcess.isEmpty()) {
            String derivedClassName = toProcess.remove();
            JavaClass derivedClass = (JavaClass) getType(derivedClassName,
                    getMethodsBodies);
            result.add(derivedClass);
            for (String className : this.derivedClasses.get(derivedClassName)) {
                toProcess.offer(className);
            }
        }

        return result;
    }

    /**
     * Adds loaders for all Java internal classes.
     */
    private void addSystemClassLoaders() {
        // get the list of directories with Java internal jar files
        String javaJarsDirsProperty =
                System.getProperty("org.softevo.oumextractor.javajarsdirs");
        if (javaJarsDirsProperty == null) {
            System.err.println("[ERROR] org.softevo.oumextractor.javajarsdirs property not set");
            System.exit(1);
        }
        String[] javaJarsDirs = javaJarsDirsProperty.split(":");

        // add a loader for each jar file found
        for (String dirName : javaJarsDirs) {
            File dir = new File(dirName);
            System.out.println(dir);
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".jar")) {
                    addClassLoader(new JarClassLoader(file));
                }
            }
        }
    }

    /**
     * Checks, if a given class is represented in the same way when read using
     * the two given loaders.
     *
     * @param className Class, whose representation is to be checked.
     * @param loader1   First loader to use.
     * @param loader2   Second loader to use.
     * @return <code>true</code> if both loaders yield the same representation
     * of the class; <code>false</code> otherwise.
     */
    private boolean equalRepresentations(String className, ClassLoader loader1,
                                         ClassLoader loader2) {
        try {
            final int BUFFER_SIZE = 4096;
            byte[] buffer1 = new byte[BUFFER_SIZE];
            byte[] buffer2 = new byte[BUFFER_SIZE];
            InputStream stream1 = loader1.getClassStream(className);
            InputStream stream2 = loader2.getClassStream(className);
            while (true) {
                int size1 = stream1.read(buffer1);
                int size2 = stream2.read(buffer2);
                if (size1 != size2) {
                    return false;
                }
                if (size1 == -1) {
                    return true;
                }
                for (int i = 0; i < size1; i++) {
                    if (buffer1[i] != buffer2[i]) {
                        return false;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        }
    }

    /**
     * Creates representation of the type with given fully qualified name.  The
     * representation is stored in the pool and available for retrieval.
     *
     * @param name             Fully qualified name of the type.
     * @param getMethodsBodies If <code>true</code> methods' bodies will be
     *                         stored; otherwise not.
     * @throws ClassNotFoundException if a type can not be found
     */
    private void createTypeRepresentation(String name, boolean getMethodsBodies)
            throws ClassNotFoundException {
        try {
            // get the type stream
            if (!this.className2loader.containsKey(name)) {
                missingTypes.add(name);
                throw new ClassNotFoundException(name);
            }
            ClassLoader loader = this.className2loader.get(name);
            InputStream typeStream = loader.getClassStream(name);

            // get the type
            ClassNode classNode = new ClassNode();
            ClassReader reader = new ClassReader(typeStream);
            reader.accept(classNode, 0);
            if ((classNode.access & Opcodes.ACC_INTERFACE) != 0) {
                JavaInterface interfac = new JavaInterface(classNode, getMethodsBodies);
                this.types.put(name, interfac);
            } else {
                JavaClass clas = new JavaClass(classNode, getMethodsBodies);
                this.types.put(name, clas);
            }
            typeStream.close();

            // adjust the derived classes hierarchy (for classes only)
            JavaType type = this.types.get(name);
            if (type instanceof JavaClass) {
                if (!this.derivedClasses.containsKey(name)) {
                    this.derivedClasses.put(name, new HashSet<String>());
                }

                if (classNode.superName != null) {
                    String superName = classNode.superName.replace('/', '.');
                    try {
                        getType(superName, getMethodsBodies);
                        this.derivedClasses.get(superName).add(name);
                    } catch (ClassNotFoundException e) {
                    }
                }
            }

            // adjust the base classes hierarchy
            Set<String> typeSupertypes = new HashSet<String>();
            this.supertypes.put(name, typeSupertypes);
            if (classNode.superName != null) {
                String superName = classNode.superName.replace('/', '.');
                typeSupertypes.add(superName);
                try {
                    getType(superName, getMethodsBodies);
                } catch (ClassNotFoundException e) {
                }
                for (Object interfaceObject : classNode.interfaces) {
                    String interfac =
                            ((String) interfaceObject).replace('/', '.');
                    typeSupertypes.add(interfac);
                    try {
                        getType(interfac, getMethodsBodies);
                    } catch (ClassNotFoundException e) {
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
//			missingTypes.add (name);
//			throw new ClassNotFoundException (name);
        }
    }
}
