package com.ibm.cleancode.unf.visitors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.MethodInvocation;

public class CatchStatementUtils {
	private static final Set<String> logMethodNames = new HashSet<String>(Arrays.asList(new String[] {
			"trace", "debug", "info", "warn", "error", "fatal", "printStackTrace"
	}));

	public static boolean isLoggerOrPrintStackTraceCall(MethodInvocation mie) {
		String logMethodName = mie.getName().getFullyQualifiedName();
		return logMethodNames.contains(logMethodName);
	}

	public static boolean isHandleCall(MethodInvocation mie) {
		String methodName = mie.getName().getFullyQualifiedName();
		return "handleWithCatch".equals(methodName) || "handle".equals(methodName);
	}

}
