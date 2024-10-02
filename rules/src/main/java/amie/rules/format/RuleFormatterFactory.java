package amie.rules.format;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class RuleFormatterFactory {
	private static DefaultRuleFormatter defaultFormatter;
	static {
		defaultFormatter = new DefaultRuleFormatter(false);
	}

	public static RuleFormatter getDefaultFormatter(boolean verbose) {
		if (!verbose) {
			return defaultFormatter;
		} else {
			return new DefaultRuleFormatter(verbose);
		}
	}

	public static RuleFormatter getFormatter(String className, boolean verbose, Map<String, Object> args)
			throws ClassNotFoundException,
			NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		if (className.toLowerCase().equals("default") || className.equals("amie.rules.format.DefaultRuleFormatter")) {
			if (args.containsKey("ommitStd") || args.containsKey("ommitPCA")) {
				boolean ommitStd = args.containsKey("ommitStd") && args.get("ommitStd").equals(Boolean.TRUE);
				boolean ommitPCA = args.containsKey("ommitPCA") && args.get("ommitPCA").equals(Boolean.TRUE);
				return new NoStdDefaultRuleFormatter(verbose, ommitStd, ommitPCA);
			}
			return getDefaultFormatter(verbose);
		} else if (className.toLowerCase().equals("anyburl")) {
			return new AnyBurlFormatter(verbose);
		} else {
			Class<?> formatterClass = Class.forName(className);
			Constructor<?> constructor = formatterClass.getConstructor(new Class[] { Boolean.class });
			return (RuleFormatter) constructor.newInstance(verbose);
		}
	}
}
