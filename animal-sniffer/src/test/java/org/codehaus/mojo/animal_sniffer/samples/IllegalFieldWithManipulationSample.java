package org.codehaus.mojo.animal_sniffer.samples;

import java.nio.file.Path;

/**
 * @author Lukas Zaruba, lukas.zaruba@lundegaard.eu, 2019
 */
public class IllegalFieldWithManipulationSample {

    private String stringField;
    private Path pathField;


    public Path getPathField() {
        return pathField;
    }

    public void setPathField(Path pathField) {
        this.pathField = pathField.resolve("other");
    }

}
