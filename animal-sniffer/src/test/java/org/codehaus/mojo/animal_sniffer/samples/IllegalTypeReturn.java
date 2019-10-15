package org.codehaus.mojo.animal_sniffer.samples;

import java.nio.file.Path;

/**
 * @author Lukas Zaruba, lukas.zaruba@lundegaard.eu, 2019
 */
public class IllegalTypeReturn {

    public Path localDateTime() {
        return null;
    }

}
