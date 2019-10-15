package org.codehaus.mojo.animal_sniffer.samples;

import java.time.LocalDateTime;

/**
 * @author Lukas Zaruba, lukas.zaruba@lundegaard.eu, 2019
 */
public class IllegalTypeReturn {

    public LocalDateTime localDateTime() {
        return null;
    }

}
