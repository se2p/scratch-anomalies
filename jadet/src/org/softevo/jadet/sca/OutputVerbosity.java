package org.softevo.jadet.sca;


/**
 * This enum contains flags specifying output verbosity when it comes to
 * outputting methods' names.
 *
 * @author Andrzej Wasylkowski
 */
public enum OutputVerbosity {

    /**
     * Full output: all packages' names included.
     */
    FULL,

    /**
     * Short output: packages' names in signatures are suppressed.
     */
    SHORT,

    /**
     * Very short output: all packages' names are suppressed.
     */
    VERY_SHORT;
}
