package org.softevo.oumextractor;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to represent Java interfaces.
 *
 * @author Andrzej Wasylkowski
 */
public class JavaInterface extends JavaType {

    /**
     * Mapping unique method name => method representation.
     */
    private final Map<String, JavaMethod> methods;

    /**
     * Creates representation of the Java interface based on its bytecode
     * representation.
     *
     * @param classNode        Bytecode representation of the interface to represent.
     * @param getMethodsBodies If <code>true</code> methods bodies will be
     *                         stored; otherwise not.
     */
    public JavaInterface(ClassNode classNode, boolean getMethodsBodies) {
        super(classNode, getMethodsBodies);

        // initialize fields
        this.methods = new HashMap<String, JavaMethod>();
        this.methodsBodiesStored = getMethodsBodies;

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

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.JavaType#getMethod(java.lang.String)
     */
    @Override
    public JavaMethod getMethod(String name)
            throws ClassNotFoundException, NoSuchMethodException {
        // look in this interface
        if (this.methods.containsKey(name)) {
            return this.methods.get(name);
        }

        // look in all superinterfaces
        for (String interfaceName : this.interfaces) {
            JavaInterface interfac = (JavaInterface) JavaClassPool.
                    get().getType(interfaceName, this.methodsBodiesStored);
            try {
                JavaMethod interfaceMethod = interfac.getMethod(name);

                // temporarily disregard methods found in the "Object" class
                if (interfaceMethod.getJavaType() instanceof JavaClass) {
                    continue;
                }

                return interfaceMethod;
            } catch (NoSuchMethodException e) {
            }
        }

        // look in the "Object" class if the method was not found in a
        // superinterface
        try {
            JavaMethod objectMethod = JavaClassPool.get().
                    getType("java.lang.Object", this.methodsBodiesStored).
                    getMethod(name);
            return objectMethod;
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodException(name + " in " + getFullName());
        }
    }

    /**
     * Returns <code>true</code> if this interface is a superinterface (direct
     * or indirect) of a given type.
     *
     * @param other Type to check this interface against.
     * @return <code>true</code> if this type is a superinterface (direct or
     * indirect) of a given type; <code>false</code> otherwise.
     * @throws ClassNotFoundException If some supertype has not been found.
     */
    public boolean isSuperInterface(JavaType other) throws ClassNotFoundException {
        // check if we are a direct superinterface of a given type
        if (other.interfaces.contains(this.fullName)) {
            return true;
        }

        // check if we are an indirect superinterface of a given type
        if (other instanceof JavaClass) {
            JavaClass otherClass = (JavaClass) other;
            if (otherClass.superFullName != null) {
                return this.isSuperInterface(otherClass.getSuperclass());
            }
        }
        JavaClassPool pool = JavaClassPool.get();
        for (String otherInterfaceName : other.interfaces) {
            JavaType otherInterface = pool.getType(otherInterfaceName, false);
            if (this.isSuperInterface(otherInterface)) {
                return true;
            }
        }
        return false;
    }
}
