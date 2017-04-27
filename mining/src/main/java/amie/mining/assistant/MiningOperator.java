package amie.mining.assistant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

/**
 * An annotation for methods that are considered mining operators like mining operators
 * @author galarrag
 *
 */
public @interface MiningOperator {
	// It defines the order of application of this operator
	String name();
	
	String dependency() default "";
	
}
