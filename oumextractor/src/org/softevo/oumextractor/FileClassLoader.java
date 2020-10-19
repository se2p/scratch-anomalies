package org.softevo.oumextractor;

import org.objectweb.asm.ClassReader;

import java.io.*;
import java.util.Collections;
import java.util.Set;

/**
 * A class loader that is capable of associating one class with a ".class" file,
 * where its representation can be found.
 *
 * @author Andrzej Wasylkowski
 */
public class FileClassLoader extends ClassLoader {

    /**
     * The ".class" file to use when loading a class.
     */
    private File classFile;

    /**
     * Fully qualified name of the class in the ".class". file.
     */
    private String className;

    /**
     * Creates a new file class loader that can be later used to read a
     * representation of a class stored in the given file.
     *
     * @param file A ".class" file.
     */
    public FileClassLoader(File file) {
        assert file.getName().endsWith(".class");
        this.classFile = file;
        try {
            FileInputStream classStream = new FileInputStream(this.classFile);
            ClassReader classReader = new ClassReader(classStream);
            this.className = classReader.getClassName().replace('/', '.');
            classStream.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.ClassLoader#getClassesNames()
     */
    @Override
    public Set<String> getClassesNames() {
        return Collections.singleton(this.className);
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.ClassLoader#getClassStream(java.lang.String)
     */
    @Override
    public InputStream getClassStream(String name)
            throws ClassNotFoundException {
        try {
            if (!this.className.equals(name)) {
                throw new ClassNotFoundException("File " + this.classFile +
                        " does not contain class " + name);
            }
            FileInputStream classStream = new FileInputStream(this.classFile);
            return classStream;
        } catch (FileNotFoundException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "FileClassLoader (" + this.classFile + ")";
    }
}
