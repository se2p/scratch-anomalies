package de.uni_passau.fim.se2.litterbox.analytics;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class Analyzer {

    Path input;
    Path output;

    public Analyzer(String input, String output) {
        this.input = Paths.get(input);
        this.output = Paths.get(output);
    }

    public abstract void analyzeFile();

    public abstract void analyzeMultiple(String listPath);

    public abstract void analyzeSingle(String listPath);
}
