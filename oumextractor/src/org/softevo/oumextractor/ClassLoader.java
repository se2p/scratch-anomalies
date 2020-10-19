package org.softevo.oumextractor;

import java.io.InputStream;
import java.util.Set;

/**
 * This is the base class for all class loaders, i.e., classes that represent
 * an association between classes' names and a resource, from which the
 * representations of those classes can be loaded.
 *
 * @author Andrzej Wasylkowski
 */
public abstract class ClassLoader {

    /**
     * Returns a set of fully qualified names of classes that can be loaded
     * using this loader.
     */
    public abstract Set<String> getClassesNames();

    /**
     * Returns a stream, from which the representation of the class with a
     * given name can be read, if the class can be found by the loader. If not,
     * a <code>ClassNotFoundException</code> exception is thrown.
     *
     * @param className Fully qualified name of the class to load.
     * @return Stream, from which the representation of the class can be read.
     * @throws ClassNotFoundException if the class was not found by the loader
     */
    public abstract InputStream getClassStream(String className)
            throws ClassNotFoundException;
}
