package org.jvnet.animal_sniffer;

import java.lang.annotation.Retention;
import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * @author Kohsuke Kawaguchi
 */
@Retention(CLASS)
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
public @interface IgnoreJRERequirement {
}
