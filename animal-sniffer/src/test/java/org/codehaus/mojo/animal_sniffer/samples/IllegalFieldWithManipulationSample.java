package org.codehaus.mojo.animal_sniffer.samples;

import java.time.LocalDateTime;

/**
 * @author Lukas Zaruba, lukas.zaruba@lundegaard.eu, 2019
 */
public class IllegalFieldWithManipulationSample {

    private String stringField;
    private LocalDateTime localDateTimeField;

    public LocalDateTime getLocalDateTimeField() {
        return localDateTimeField;
    }

    public void setLocalDateTimeField(LocalDateTime localDateTimeField) {
        this.localDateTimeField = localDateTimeField.plusDays(1);
    }

}
