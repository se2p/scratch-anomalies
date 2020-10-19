package org.softevo.oumextractor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A class loader that is capable of associating classes with a jar file, where
 * their representations can be found.
 *
 * @author Andrzej Wasylkowski
 */
public class JarClassLoader extends ClassLoader {

    /**
     * The jar file to use when loading classes.
     */
    private File file;

    /**
     * Creates a new jar class loader that can be later used to read
     * representations of classes stored in the given (jar) file.
     *
     * @param file A jar file.
     */
    public JarClassLoader(File file) {
        assert file.getName().endsWith(".jar");
        this.file = file;
    }

    /**
     * Returns the absolute filename of this jar.
     *
     * @return Absolute filename of this jar.
     */
    public String getFilename() {
        return file.getAbsolutePath();
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.ClassLoader#getClassesNames()
     */
    @Override
    public Set<String> getClassesNames() {
        try {
            Set<String> classesNames = new HashSet<String>();
            JarFile jarFile = new JarFile(this.file);
            for (Enumeration<JarEntry> e = jarFile.entries();
                 e.hasMoreElements(); ) {
                JarEntry entry = e.nextElement();
                String entryName = entry.getName();
                if (entryName.endsWith(".class")) {
                    String name = entryName.replace(".class", "").replace('/', '.');
                    classesNames.add(name);
                }
            }
            jarFile.close();
            return classesNames;
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        }
    }

    /* (non-Javadoc)
     * @see org.softevo.oumextractor.ClassLoader#getClassStream(java.lang.String)
     */
    @Override
    public InputStream getClassStream(String name)
            throws ClassNotFoundException {
        try {
            JarFile jarFile = new JarFile(this.file);
            String entryName = name.replace('.', '/') + ".class";
            JarEntry entry = jarFile.getJarEntry(entryName);
            if (entry == null) {
                throw new ClassNotFoundException("Jar file " + jarFile +
                        " does not contain a file " + entryName);
            }
            return jarFile.getInputStream(entry);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new InternalError();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "JarClassLoader (" + this.file + ")";
    }
}
