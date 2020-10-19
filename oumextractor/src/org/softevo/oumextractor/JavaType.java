package org.softevo.oumextractor;

import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to represent Java objects and interfaces.
 *
 * @author Andrzej Wasylkowski
 */
public abstract class JavaType {

    /**
     * Fully qualified name of this type.
     */
    protected final String fullName;

    /**
     * Fully qualified name of the supertype of this type.
     */
    protected final String superFullName;

    /**
     * Fully qualified names of interfaces implemented by this type.
     */
    protected final List<String> interfaces;

    /**
     * Access flags of this type.
     */
    protected final int accessFlags;

    /**
     * <code>true</code> if methods' bodies are stored.
     */
    protected boolean methodsBodiesStored;

    /**
     * Creates representation of the Java type based on its bytecode
     * representation.
     *
     * @param classNode        Bytecode representation of the type to represent.
     * @param getMethodsBodies If <code>true</code> methods bodies will be
     *                         stored; otherwise not.
     */
    protected JavaType(ClassNode classNode, boolean getMethodsBodies) {
        this.fullName = classNode.name.replace('/', '.');
        if (classNode.superName != null) {
            this.superFullName = classNode.superName.replace('/', '.');
        } else {
            this.superFullName = null;
        }
        this.interfaces = new ArrayList<String>();
        for (Object interfaceObject : classNode.interfaces) {
            String iface = ((String) interfaceObject).replace('/', '.');
            this.interfaces.add(iface);
        }
        this.accessFlags = classNode.access;
        this.methodsBodiesStored = getMethodsBodies;
    }

    /**
     * Returns the most specific common supertype of the given two types.
     *
     * @param type1 One of the types.
     * @param type2 Other one of the types.
     * @return The most specific common supertype of the given two types.
     * @throws UnsupportedOperationException If the most specific common
     *                                       supertype does not exist or can't be found.
     */
    public static JavaType getCommonSupertype(JavaType type1arg,
                                              JavaType type2arg) throws UnsupportedOperationException {
        // if the two types are identical, return one of them
        if (type1arg == type2arg) {
            return type1arg;
        }

        // figure out the most specific common supertype
        try {
            Class<?> type1 = Class.forName(type1arg.getFullName(), false,
                    JavaType.class.getClassLoader());
            Class<?> type2 = Class.forName(type2arg.getFullName(), false,
                    JavaType.class.getClassLoader());

            // deal with both types being interfaces
            if (type1.isInterface() && type2.isInterface()) {
                if (type1.isAssignableFrom(type2)) {
                    return type1arg;
                } else if (type2.isAssignableFrom(type1)) {
                    return type2arg;
                } else {
                    throw new UnsupportedOperationException();
                }
            }
            // deal with one of the types being an interface
            else if (type1.isInterface()) {
                if (type1.isAssignableFrom(type2)) {
                    return type1arg;
                } else {
                    throw new UnsupportedOperationException();
                }
            } else if (type2.isInterface()) {
                if (type2.isAssignableFrom(type1)) {
                    return type1arg;
                } else {
                    throw new UnsupportedOperationException();
                }
            }
            // deal with both types being classes
            else {
                if (type1.isAssignableFrom(type2)) {
                    return type1arg;
                } else if (type2.isAssignableFrom(type1)) {
                    return type2arg;
                } else {
                    JavaClass class1 = (JavaClass) type1arg;
                    JavaClass class2 = (JavaClass) type2arg;
                    return getCommonSupertype(class1.getSuperclass(),
                            class2.getSuperclass());
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] This should never happen");
            throw new InternalError(e.getMessage());
        }
    }

    /**
     * Deletes all information about this type.  This should be used only if
     * the type is not needed any more.
     */
    public abstract void clear();

    /**
     * Returns <code>true</code> if this class' methods' bodies are stored.
     *
     * @return <code>true</code> if this class' methods' bodies are stored;
     * <code>false</code> otherwise.
     */
    public boolean hasMethodsBodies() {
        return this.methodsBodiesStored;
    }

    /**
     * Returns full name of this type, which is fully qualified name of
     * this type.
     *
     * @return Fully qualified name of this type.
     */
    public String getFullName() {
        return this.fullName;
    }

    /**
     * Returns full name of a supertype this type or <code>null</code>.
     *
     * @return Fully qualified name of supertype of this type or
     * <code>null</code> if this type has no supertype.
     */
    public String getSuperFullName() {
        return this.superFullName;
    }

    /**
     * Indicates, whether it is possible to assign instance of a type given as
     * an argument to a variable of a type represented by this object.
     *
     * @param other Representation of the type of the instance of an object to
     *              be assigned.
     * @return <code>true</code> if given argument is assignment compatible
     * with type represented by this object; <code>false</code> if it
     * is not.
     * @throws ClassNotFoundException If some class can not be found.
     */
    public boolean isAssignableFrom(JavaType other)
            throws ClassNotFoundException {
        // check if this type is the same as the other type
        if (this.fullName.equals(other.fullName)) {
            return true;
        }

        // check if this type is a superinterface or a superclass of the other
        if (this instanceof JavaClass) {
            if (this.fullName.equals("java.lang.Object")) {
                return true;
            }
            if (!(other instanceof JavaClass)) {
                return false;
            }
            JavaClass thisClass = (JavaClass) this;
            JavaClass otherClass = (JavaClass) other;
            return thisClass.isSuperClass(otherClass);
        } else {
            JavaInterface thisInterface = (JavaInterface) this;
            return thisInterface.isSuperInterface(other);
        }
    }

    /**
     * Returns representation of a method of given unique name.  If the method
     * is implemented in this class or any of its base classes, the
     * representation of this method in the most specific class will be
     * returned.  If it is only in an interface, the top-most left-most
     * interface declaring this method will be returned.
     *
     * @param name Unique name of the method: method name, descriptor.
     * @return Representation of a method of given name.
     * @throws ClassNotFoundException If a class can not be found.
     * @throws NoSuchMethodException  If a method can not be found.
     */
    public abstract JavaMethod getMethod(String name)
            throws ClassNotFoundException, NoSuchMethodException;
}
