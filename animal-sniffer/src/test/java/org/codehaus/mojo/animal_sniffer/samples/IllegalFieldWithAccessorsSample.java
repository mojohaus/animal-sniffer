package org.codehaus.mojo.animal_sniffer.samples;

import java.nio.file.Path;

/**
 * @author Lukas Zaruba, lukas.zaruba@lundegaard.eu, 2019
 */
public class IllegalFieldWithAccessorsSample {

    private String stringField;
    private Path pathField;

    public String getStringField() {
        return stringField;
    }

    public void setStringField(String stringField) {
        this.stringField = stringField;
    }

    public Path getPathField() {
        return pathField;
    }

    public void setPathField(Path pathField) {
        this.pathField = pathField;
    }

}
