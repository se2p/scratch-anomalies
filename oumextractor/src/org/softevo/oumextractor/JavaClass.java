package org.softevo.oumextractor;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.softevo.oumextractor.analysis.Analyzer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for representing Java classes.
 *
 * @author Andrzej Wasylkowski
 */
public final class JavaClass extends JavaType {

    /**
     * Mapping unique method name => method representation.
     */
    private final Map<String, JavaMethod> methods;

    /**
     * Creates representation of the Java class based on its bytecode
     * representation.
     *
     * @param classNode        Bytecode representation of the class to represent.
     * @param getMethodsBodies If <code>true</code> methods bodies will be
     *                         stored; otherwise not.
     */
    public JavaClass(ClassNode classNode, boolean getMethodsBodies) {
        super(classNode, getMethodsBodies);

        // initialize fields
        this.methods = new HashMap<String, JavaMethod>();

        // create representations of all methods in this class
        for (Object methodObject : classNode.methods) {
            MethodNode methodNode = (MethodNode) methodObject;
            JavaMethod method = new JavaMethod(this, methodNode,
                    getMethodsBodies);
            this.methods.put(method.getName(), method);
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.JavaType#clear()
     */
    @Override
    public void clear() {
        this.methods.clear();
    }

    /**
     * Returns set of classes derived from this class.
     *
     * @return Set of classes derived from this class.
     * @throws ClassNotFoundException if some class can not be found
     */
    public Set<JavaClass> getDerivedClasses() throws ClassNotFoundException {
        return JavaClassPool.get().getDerivedClasses(this.getFullName(),
                this.methodsBodiesStored);
    }

    /**
     * Returns <code>true</code> if this class is a superclass (direct or
     * indirect) of a given class.
     *
     * @param other Class to check this class against.
     * @return <code>true</code> if this class is a superclass (direct or
     * indirect) of a given class; <code>false</code> otherwise.
     * @throws ClassNotFoundException If some superclass has not been found.
     */
    public boolean isSuperClass(JavaClass other) throws ClassNotFoundException {
        // if the other class has no superclass, we cannot be its superclass
        // this can happen if the other class is the Object class
        if (other.superFullName == null) {
            return false;
        }

        // check if we are a direct superclass of a given class
        if (other.superFullName.equals(this.fullName)) {
            return true;
        }

        // check if we are an indirect superclass of a given class
        JavaClass otherSuper = other.getSuperclass();
        return this.isSuperClass(otherSuper);
    }

    /**
     * Returns superclass of this class.
     *
     * @return Superlcass of this class.
     * @throws ClassNotFoundException If the superclass has not been found.
     */
    public JavaClass getSuperclass() throws ClassNotFoundException {
        // make sure we're not trying to get a superclass of the Object class
        if (this.superFullName == null) {
            throw new InternalError("trying to get super class of Object");
        }

        // get the superclass
        return (JavaClass) JavaClassPool.get().getType(this.superFullName,
                this.methodsBodiesStored);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.JavaType#getMethod(java.lang.String)
     */
    @Override
    public JavaMethod getMethod(String name)
            throws ClassNotFoundException, NoSuchMethodException {
        // if this class is not abstract, the method must be in this class or
        // one of its superclasses
        if ((this.accessFlags & Opcodes.ACC_ABSTRACT) == 0) {
            JavaClass clas = this;
            while (clas != null) {
                if (clas.methods.containsKey(name)) {
                    return clas.methods.get(name);
                }

                if (clas.superFullName == null) {
                    clas = null;
                } else {
                    clas = clas.getSuperclass();
                }
            }

            // at this point the method does not exist
            throw new NoSuchMethodException(name + " in " + getFullName());
        }
        // if this class is abstract, look in the class itself first, then in
        // its superclass and then in its interfaces
        else {
            // look in this class
            if (this.methods.containsKey(name)) {
                return this.methods.get(name);
            }

            // look in this class' superclasses
            JavaMethod superMethod = null;
            if (this.superFullName != null) {
                try {
                    superMethod = this.getSuperclass().getMethod(name);
                } catch (NoSuchMethodException e) {
                }
            }

            // if no method was found, or it was found in an interface, try to
            // look in interfaces implemented by this class
            if (superMethod == null ||
                    superMethod.getJavaType() instanceof JavaInterface) {
                JavaMethod interfaceMethod = null;
                for (String interfaceName : this.interfaces) {
                    JavaInterface interfac = (JavaInterface) JavaClassPool.
                            get().getType(interfaceName, this.methodsBodiesStored);
                    try {
                        interfaceMethod = interfac.getMethod(name);
                        return interfaceMethod;
                    } catch (NoSuchMethodException e) {
                    }
                }
            }

            // now return what we have, if we have something at all
            if (superMethod != null) {
                return superMethod;
            } else {
                throw new NoSuchMethodException(name + " in " + getFullName());
            }
        }
    }

    /**
     * Creates control flow graphs for all methods in this class.
     */
    void createCFGRepresentation() throws ClassNotFoundException {
        for (JavaMethod method : this.methods.values()) {
            if ((method.getMethodNode().access &
                    (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                method.createCFG();
            }
        }
    }

    /**
     * Performs data flow analysis of all methods in this class.
     *
     * @param analyzer Analyzer to be used.
     */
    void analyzeDataFlow(Analyzer analyzer) {
        for (JavaMethod method : this.methods.values()) {
            if ((method.getMethodNode().access &
                    (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                method.analyzeDataFlow(analyzer);
            }
        }
    }
}
