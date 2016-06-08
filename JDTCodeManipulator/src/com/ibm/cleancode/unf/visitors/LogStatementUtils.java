package com.ibm.cleancode.unf.visitors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;

import com.ibm.cleancode.unf.visitors.LogInspectionResult.LogType;


public class LogStatementUtils {
	private static final Set<String> logMethodNames = new HashSet<String>(Arrays.asList(new String[] { "trace",
			"debug", "info", "warn", "error", "fatal" }));

	public static LogInspectionResult isLevelCheckedLogStatement(IfStatement statement, String expectedDebugStatement) {
		LogInspectionResult inspectionResult = null;
		if (statement instanceof IfStatement) {
			IfStatement suspectedLogCheckCondition = (IfStatement) statement;
			// check if the statement is a if statement and it check for log
			// level
			if (isLogLevelCheckStatement(suspectedLogCheckCondition)) {
				Statement thenStatement = suspectedLogCheckCondition.getThenStatement();
				Statement insideIfStatement = null;
				// see if it is a block statement
				if (thenStatement instanceof Block) {
					Block suspectedLogBlock = (Block) thenStatement;
					if (suspectedLogBlock.statements().size() > 0) {
						insideIfStatement = (Statement) suspectedLogBlock.statements().get(0);
					}
				} else {
					insideIfStatement = (Statement) thenStatement;
				}
				// check the statement to see if it is a log
				// statement
				if (insideIfStatement != null && insideIfStatement instanceof ExpressionStatement) {
					LogInspectionResult r = isLogStatement((ExpressionStatement) insideIfStatement,
							expectedDebugStatement);
					if (r.isLogStatement()) {
						inspectionResult = r;
						inspectionResult.setLogLevelCheck(true);
					}
				}
			}
		}
		return inspectionResult;
	}

	public static LogInspectionResult isLogStatement(ExpressionStatement s, String expectedLogStatement) {
		LogInspectionResult inspectionResult = new LogInspectionResult();
		Expression e = s.getExpression();
		if (e instanceof MethodInvocation) {
			MethodInvocation mie = (MethodInvocation) e;
			// check if it is a log statement
			String logMethodName = mie.getName().getFullyQualifiedName();
			if (isLoggerCall(mie)) {
				inspectionResult.setLogStatement(true);
				inspectionResult.setLogLevel(logMethodName);
				// check if it is logging service log or a direct logger call
				inspectionResult.setLogType(getLogType(mie));
				// check if format is correct
				// this is typically used for method start end logs
				if (expectedLogStatement != null) {
					@SuppressWarnings("rawtypes")
					List arguments = mie.arguments();
					String debugStatement = null;
					if (arguments.size() == 1 && arguments.get(0) instanceof StringLiteral) {
						StringLiteral l = (StringLiteral) arguments.get(0);
						debugStatement = l.getLiteralValue();
						if (expectedLogStatement.equals(debugStatement)) {
							inspectionResult.setFormatCompliant(true);
						}
					} else if (arguments.size() == 2 && arguments.get(1) instanceof StringLiteral) {
						StringLiteral l = (StringLiteral) arguments.get(1);
						debugStatement = l.getLiteralValue();
						if (expectedLogStatement.equals(debugStatement)) {
							inspectionResult.setFormatCompliant(true);
						}
					} else {
						inspectionResult.setFormatCompliant(false);
					}
				}
			}
		}
		return inspectionResult;
	}

	public static boolean isLogLevelCheckStatement(IfStatement suspectedLogCheckCondition) {
		return suspectedLogCheckCondition.getExpression().toString()
				.matches(".*\\.is(Trace|Debug|Info|Warn|Error|Fatal)(Level)*Enable.*");
	}
	
	
	public static boolean isLoggerCall(MethodInvocation mie){
		String logMethodName = mie.getName().getFullyQualifiedName();
		return logMethodNames.contains(logMethodName);
	}
	
	public static LogType getLogType(MethodInvocation mie) {
		// check if it is logging service log or a direct logger call
		Expression loggerExpression = mie.getExpression();
		if (loggerExpression instanceof MethodInvocation) {
			if (((MethodInvocation) loggerExpression).getName().getFullyQualifiedName().equals("getLoggingService")) {
				return LogType.SERVICE;
			} else {
				return LogType.SERVICE_SUSPECT;
			}
		} else if (loggerExpression instanceof SimpleName) {
			if (((SimpleName) loggerExpression).getFullyQualifiedName().matches(".*(l|L)(o|O)(g|G){2}(e|E)(r|R)")) {
				return LogType.DIRECT;
			} else {
				return LogType.DIRECT_SUSPECT;
			}
		} else {
			return LogType.UNKNOWN;
		}
	}

}
