package com.ibm.cleancode.unf.visitors;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import com.ibm.cleancode.framework.processor.RewriteVisitor;
import com.ibm.cleancode.framework.processor.StatementCountVisitor;
import com.ibm.cleancode.unf.visitors.LogInspectionResult.LogType;

public class MethodEntryLogCorrectionVisitor extends RewriteVisitor {
	private static final Logger ENTRY_LOG_HIGH_CONFIDENCE = Logger.getLogger("MethodEntryLogHighConfidence");
	private static final Logger ENTRY_LOG_LOW_CONFIDENCE = Logger.getLogger("MethodEntryLogLowConfidence");
	private static final Logger ENTRY_LOG_COMPLIANT = Logger.getLogger("MethodEntryLogCompliant");
	private static final Logger ENTRY_LOG_NOT_PRESETN = Logger.getLogger("MethodEntryLogNotPresent");
	private final List<MethodLogInspectionResult> results;
	private int issueCount = 0;
	private int lowConfidenceMethodStartLog = 0;
	private int highConfidenceMethodStartLog = 0;
	private int compliantMethodStartLog = 0;
	private MethodDeclaration currentMethodDeclaration = null;

	// do rewrite
	boolean doRewrite = false;

	public MethodEntryLogCorrectionVisitor(boolean doRewrite) {
		results = new ArrayList<MethodLogInspectionResult>();
		this.doRewrite = doRewrite;
	}

	public boolean visit(MethodDeclaration methodDeclaration) {
		currentMethodDeclaration = methodDeclaration;
		return true;
	}

	public void endVisit(MethodDeclaration methodDeclaration) {
		currentMethodDeclaration = null;
	}

	public boolean visit(MethodInvocation es) {
		// first log statements in method are handled differently
		if (LogStatementUtils.isLoggerCall(es)) {
			// to see if wrapped in if statement
			ASTNode suscepectedLogCheckWrapper = es.getParent().getParent().getParent();
			// extract log level
			String logLevel = es.getName().getFullyQualifiedName().toUpperCase();
			// handle method entry log
			handleMethodEntryLogStatement(es, suscepectedLogCheckWrapper, logLevel);
		}
		return false;
	}

	private boolean handleMethodEntryLogStatement(MethodInvocation es, ASTNode suscepectedLogCheckWrapper,
			String logLevel) {
		if (currentMethodDeclaration != null) {
			boolean notWrapped = false;
			IfStatement firstIfStatementInMethod = null;
			Object firstStatementInMethod = currentMethodDeclaration.getBody().statements().get(0);
			// see if first statement itself is a log statement
			if (firstStatementInMethod instanceof ExpressionStatement
					&& es.equals(((ExpressionStatement) firstStatementInMethod).getExpression())) {
				System.err.println("Orphan logger at " + getCompilationUnit().getLineNumber(es.getStartPosition() - 1)
						+ " in " + getSourceClassName() + ":"
						+ currentMethodDeclaration.getName().getFullyQualifiedName());
				notWrapped = true;
			} else if (firstStatementInMethod instanceof IfStatement) {
				// else must be an if statement
				// TODO there are instanced when the first logger is neither
				// orphan nor in an if block (like in try block) we need to
				// tackle those cases
				firstIfStatementInMethod = (IfStatement) firstStatementInMethod;

				// see if current wrapper is the first statement in the method
				// and it is a log level check condition
				if (suscepectedLogCheckWrapper.equals(firstStatementInMethod)
						&& LogStatementUtils.isLogLevelCheckStatement(firstIfStatementInMethod)) {
					// check basics
					Statement thenStatement = firstIfStatementInMethod.getThenStatement();
					// typically it should be a block if containing exactly one
					// statement
					if ((thenStatement instanceof Block && ((Block) thenStatement).statements().size() > 1)) {
						lowConfidenceMethodStartLog++;
						ENTRY_LOG_LOW_CONFIDENCE.info("at "
								+ getCompilationUnit().getLineNumber(es.getStartPosition() - 1) + " in "
								+ getSourceClassName() + ":"
								+ currentMethodDeclaration.getName().getFullyQualifiedName());
						ENTRY_LOG_LOW_CONFIDENCE.info(suscepectedLogCheckWrapper);
						return true;
					}
				} else {
					// do not proceed as it is not a logger wrapper
					return true;
				}
			}
			// we only process if it is wrapped (firstIfStatementInMethod is
			// initialized) or not wrapped
			if (firstIfStatementInMethod != null || notWrapped) {
				// check the logger statement for compliance
				LogType logType = LogStatementUtils.getLogType(es);
				@SuppressWarnings("rawtypes")
				List arguments = es.arguments();
				int expectedArgCount = 1;
				// for service log call type log call should have max 2
				// arguments

				if (logType == LogType.SERVICE) {
					expectedArgCount = 2;
				}

				if ((logType != LogType.SERVICE && logType != LogType.DIRECT)
						|| (arguments.size() != expectedArgCount
								|| !(arguments.get(expectedArgCount - 1) instanceof StringLiteral) || !isPossibleMehtodStartLogLiteral(((StringLiteral) arguments
									.get(expectedArgCount - 1)).getLiteralValue()))) {
					lowConfidenceMethodStartLog++;
					ENTRY_LOG_LOW_CONFIDENCE.info("at " + getCompilationUnit().getLineNumber(es.getStartPosition() - 1)
							+ " in " + getSourceClassName() + ":"
							+ currentMethodDeclaration.getName().getFullyQualifiedName());
					ENTRY_LOG_LOW_CONFIDENCE.info(notWrapped ? es : suscepectedLogCheckWrapper);
					return true;
				}

				String logString = ((StringLiteral) arguments.get(expectedArgCount - 1)).getLiteralValue();
				// check compliance
				String expectedLogStatement = getSourceClassName() + "#"
						+ currentMethodDeclaration.getName().getFullyQualifiedName() + ":start";

				// level check method name
				String levelCheckMethodName = null;
				if (firstIfStatementInMethod != null) {
					levelCheckMethodName = ((MethodInvocation) firstIfStatementInMethod.getExpression()).getName()
							.getFullyQualifiedName();
				}

				if (logString.equals(expectedLogStatement)
						&& logLevel.equals("INFO")
						&& ("isInfoEnabled".equals(levelCheckMethodName) || "isInfoLevelEnable"
								.equals(levelCheckMethodName))) {
					compliantMethodStartLog++;
					ENTRY_LOG_COMPLIANT.info("at " + getCompilationUnit().getLineNumber(es.getStartPosition() - 1)
							+ " in " + getSourceClassName() + ":"
							+ currentMethodDeclaration.getName().getFullyQualifiedName());
					ENTRY_LOG_COMPLIANT.info(notWrapped ? es : suscepectedLogCheckWrapper);
				} else {
					highConfidenceMethodStartLog++;
					ENTRY_LOG_HIGH_CONFIDENCE.info("at "
							+ getCompilationUnit().getLineNumber(es.getStartPosition() - 1) + " in "
							+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
					ENTRY_LOG_HIGH_CONFIDENCE.info(notWrapped ? es : suscepectedLogCheckWrapper);
					if (doRewrite) {
						ListRewrite listRewrite = getAstRewrite().getListRewrite(es,
								MethodInvocation.ARGUMENTS_PROPERTY);
						StringLiteral logStaramentLiteral = (StringLiteral) getAstRewrite().createStringPlaceholder(
								"\"" + expectedLogStatement + "\"", ASTNode.STRING_LITERAL);
						listRewrite.replace(
								(ASTNode) listRewrite.getOriginalList().get(listRewrite.getOriginalList().size() - 1),
								logStaramentLiteral, null);

						// check if log level needs correction
						if (firstIfStatementInMethod != null) {
							if (!StringUtils.contains(levelCheckMethodName, "Info")) {
								if (logType == LogType.DIRECT) {
									getAstRewrite().set(firstIfStatementInMethod.getExpression(),
											MethodInvocation.NAME_PROPERTY,
											getAstRewrite().getAST().newSimpleName("isInfoEnabled"), null);
								} else if (logType == LogType.SERVICE) {
									getAstRewrite().set(firstIfStatementInMethod.getExpression(),
											MethodInvocation.NAME_PROPERTY,
											getAstRewrite().getAST().newSimpleName("isInfoLevelEnable"), null);
								}
							}
							if (!logLevel.equals("INFO")) {
								// change logger call
								getAstRewrite().set(es, MethodInvocation.NAME_PROPERTY,
										getAstRewrite().getAST().newSimpleName("info"), null);
							}
						}
					}
				}
			} else {
				if (isEligibleForInsertion(currentMethodDeclaration)) {
					ENTRY_LOG_NOT_PRESETN.info("Entry log not found at "
							+ getCompilationUnit().getLineNumber(es.getStartPosition() - 1) + " in "
							+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
				}
			}
		}
		return true;
	}

	public List<MethodLogInspectionResult> getResults() {
		return results;
	}

	public int getIssueCount() {
		return issueCount;
	}

	public int getLowConfidenceMethodStartLog() {
		return lowConfidenceMethodStartLog;
	}

	public int getHighConfidenceMethodStartLog() {
		return highConfidenceMethodStartLog;
	}

	public int getCompliantMethodStartLog() {
		return compliantMethodStartLog;
	}

	private boolean isPossibleMehtodStartLogLiteral(String s) {
		return StringUtils.containsIgnoreCase(s, "inside") || StringUtils.containsIgnoreCase(s, "called")
				|| StringUtils.containsIgnoreCase(s, "start") || StringUtils.containsIgnoreCase(s, "enter");
	}

	private boolean isEligibleForInsertion(MethodDeclaration md) {
		String methodName = currentMethodDeclaration.getName().getFullyQualifiedName();
		if (methodName.equals("hashCode") || methodName.equals("equals") || methodName.equals("toString")) {
			return false;
		}
		// check number of raw statements
		StatementCountVisitor statementCountVisitor = new StatementCountVisitor();
		md.accept(statementCountVisitor);
		if (statementCountVisitor.getCount() <= 3) {
			return false;
		}
		return true;
	}

}