package org.codehaus.mojo.animal_sniffer.samples;

import java.time.LocalDateTime;

/**
 * @author Lukas Zaruba, lukas.zaruba@lundegaard.eu, 2019
 */
public class IllegalFieldWithAccessorsSample {

    private String stringField;
    private LocalDateTime localDateTimeField;

    public String getStringField() {
        return stringField;
    }

    public void setStringField(String stringField) {
        this.stringField = stringField;
    }

    public LocalDateTime getLocalDateTimeField() {
        return localDateTimeField;
    }

    public void setLocalDateTimeField(LocalDateTime localDateTimeField) {
        this.localDateTimeField = localDateTimeField;
    }

}
