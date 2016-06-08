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
import com.ibm.cleancode.framework.utility.CodeUtils;
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

	// do rewrite
	boolean doRewrite = false;

	public MethodEntryLogCorrectionVisitor(boolean doRewrite) {
		results = new ArrayList<MethodLogInspectionResult>();
		this.doRewrite = doRewrite;
	}

	public boolean visit(MethodDeclaration methodDeclaration) {
		// get first statement
		if (methodDeclaration.getBody().statements().size() == 0) {
			return false;
		}
		Object firstStatementInMethod = methodDeclaration.getBody().statements().get(0);
		IfStatement logWrapperifStatament = null;
		MethodInvocation logStatement = null;
		if (firstStatementInMethod instanceof ExpressionStatement) {
			ExpressionStatement _es = (ExpressionStatement) firstStatementInMethod;
			if (_es.getExpression() instanceof MethodInvocation) {
				MethodInvocation _me = (MethodInvocation) _es.getExpression();
				if (LogStatementUtils.isLoggerCall(_me)) {
					System.err.println("Orphan logger at "
							+ getCompilationUnit().getLineNumber(_es.getStartPosition() - 1) + " in "
							+ getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName());
					logStatement = _me;
				}
			} else if (firstStatementInMethod instanceof IfStatement) {
				// else must be an if statement
				// TODO there are instanced when the first logger is neither
				// orphan nor in an if block (like in try block) we need to
				// tackle those cases
				logWrapperifStatament = (IfStatement) firstStatementInMethod;
				// see if current wrapper is the first statement in the method
				// and it is a log level check condition
				if (LogStatementUtils.isLogLevelCheckStatement(logWrapperifStatament)) {
					// check basics
					Statement thenStatement = logWrapperifStatament.getThenStatement();
					// typically it should be a block if containing exactly one
					// statement
					if ((thenStatement instanceof Block && ((Block) thenStatement).statements().size() > 1)) {
						lowConfidenceMethodStartLog++;
						ENTRY_LOG_LOW_CONFIDENCE.info("at "
								+ getCompilationUnit().getLineNumber(_es.getStartPosition() - 1) + " in "
								+ getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName());
						ENTRY_LOG_LOW_CONFIDENCE.info(logWrapperifStatament);
						return false;
					} else {
						// we continue to process this if statement as it is a
						// wrapper with exactly one statement inside
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
						// get the method invocation part for processing
						logStatement = (MethodInvocation) ((ExpressionStatement) insideIfStatement).getExpression();
					}
				} else {
					// do not proceed as it is not a logger wrapper
					return false;
				}
			} else {
				return false;
			}
			// we only process if it is wrapped (firstIfStatementInMethod is
			// initialized) or not wrapped
			if (logStatement != null) {
				String logLevel = logStatement.getName().getFullyQualifiedName().toUpperCase();
				// check the logger statement for compliance
				LogType logType = LogStatementUtils.getLogType(logStatement);
				@SuppressWarnings("rawtypes")
				List arguments = logStatement.arguments();
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
					ENTRY_LOG_LOW_CONFIDENCE.info("at "
							+ getCompilationUnit().getLineNumber(_es.getStartPosition() - 1) + " in "
							+ getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName());
					ENTRY_LOG_LOW_CONFIDENCE.info(logWrapperifStatament != null ? logWrapperifStatament : logStatement);
					return true;
				}

				String logString = ((StringLiteral) arguments.get(expectedArgCount - 1)).getLiteralValue();
				// check compliance
				String expectedLogStatement = getSourceClassName() + "#"
						+ methodDeclaration.getName().getFullyQualifiedName() + ":start";

				// level check method name
				String levelCheckMethodName = null;
				if (logWrapperifStatament != null) {
					levelCheckMethodName = ((MethodInvocation) logWrapperifStatament.getExpression()).getName()
							.getFullyQualifiedName();
				}

				if (logString.equals(expectedLogStatement)
						&& logLevel.equals("INFO")
						&& ("isInfoEnabled".equals(levelCheckMethodName) || "isInfoLevelEnable"
								.equals(levelCheckMethodName))) {
					compliantMethodStartLog++;
					ENTRY_LOG_COMPLIANT
							.info("at " + getCompilationUnit().getLineNumber(_es.getStartPosition() - 1) + " in "
									+ getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName());
					ENTRY_LOG_COMPLIANT.info(logWrapperifStatament != null ? logWrapperifStatament : logStatement);
				} else {
					highConfidenceMethodStartLog++;
					ENTRY_LOG_HIGH_CONFIDENCE.info("at "
							+ getCompilationUnit().getLineNumber(_es.getStartPosition() - 1) + " in "
							+ getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName());
					ENTRY_LOG_HIGH_CONFIDENCE
							.info(logWrapperifStatament != null ? logWrapperifStatament : logStatement);
					if (doRewrite) {
						ListRewrite listRewrite = getAstRewrite().getListRewrite(_es,
								MethodInvocation.ARGUMENTS_PROPERTY);
						StringLiteral logStaramentLiteral = (StringLiteral) getAstRewrite().createStringPlaceholder(
								"\"" + expectedLogStatement + "\"", ASTNode.STRING_LITERAL);
						listRewrite.replace(
								(ASTNode) listRewrite.getOriginalList().get(listRewrite.getOriginalList().size() - 1),
								logStaramentLiteral, null);

						// check if log level needs correction
						if (logWrapperifStatament != null) {
							if (!StringUtils.contains(levelCheckMethodName, "Info")) {
								if (logType == LogType.DIRECT) {
									getAstRewrite().set(logWrapperifStatament.getExpression(),
											MethodInvocation.NAME_PROPERTY,
											getAstRewrite().getAST().newSimpleName("isInfoEnabled"), null);
								} else if (logType == LogType.SERVICE) {
									getAstRewrite().set(logWrapperifStatament.getExpression(),
											MethodInvocation.NAME_PROPERTY,
											getAstRewrite().getAST().newSimpleName("isInfoLevelEnable"), null);
								}
							}
							if (!logLevel.equals("INFO")) {
								// change logger call
								getAstRewrite().set(_es, MethodInvocation.NAME_PROPERTY,
										getAstRewrite().getAST().newSimpleName("info"), null);
							}
						}
					}
				}
			} else {
				if (CodeUtils.isEligibleForInsertion(methodDeclaration)) {
					ENTRY_LOG_NOT_PRESETN.info("Entry log not found at "
							+ getCompilationUnit().getLineNumber(_es.getStartPosition() - 1) + " in "
							+ getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName());
				}
			}
		}
		return false;
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

}