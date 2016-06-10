package com.ibm.cleancode.unf.visitors;

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

/**
 * @author Amit
 * 
 */
public abstract class AbstractMethodLogCorrectionVisitor extends RewriteVisitor {

	// do rewrite
	boolean doRewrite = false;

	private static final Logger LOG_HIGH_CONFIDENCE = Logger.getLogger("LogHighConfidence");
	private static final Logger LOG_LOW_CONFIDENCE = Logger.getLogger("LogLowConfidence");
	private static final Logger LOG_COMPLIANT = Logger.getLogger("LogCompliant");
	private static final Logger LOG_NOT_PRESENT = Logger.getLogger("LogNotPresent");

	private int lowConfidenceMethodLogCount = 0;
	private int highConfidenceMethodLogCount = 0;
	private int compliantMethodLogCount = 0;
	private int addedCount = 0;

	private static final String LOG_STRING_SERVICE_TYPE = "if (getLoggingService().isInfoLevelEnable()) {\n\tgetLoggingService().info(this, \"#LOG_STR#\");\n}";

	public AbstractMethodLogCorrectionVisitor(boolean doRewrite) {
		super();
		this.doRewrite = doRewrite;
	}

	public void checkAndRewriteLog(MethodDeclaration methodDeclaration, Statement candidateStatement,
			Block containerForInsertion, Statement anchorForInsertion, String phase) {
		IfStatement logWrapperifStatament = null;
		MethodInvocation logStatement = null;
		if (candidateStatement instanceof ExpressionStatement) {
			ExpressionStatement _es = (ExpressionStatement) candidateStatement;
			if (_es.getExpression() instanceof MethodInvocation) {
				MethodInvocation _me = (MethodInvocation) _es.getExpression();
				if (LogStatementUtils.isLoggerCall(_me)) {
					System.err.println("Orphan logger at "
							+ getCompilationUnit().getLineNumber(_es.getStartPosition() - 1) + " in "
							+ getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName());
					logStatement = _me;
				}
			}
		} else if (candidateStatement instanceof IfStatement) {
			// else must be an if statement
			// TODO there are instanced when the first logger is neither
			// orphan nor in an if block (like in try block) we need to
			// tackle those cases
			logWrapperifStatament = (IfStatement) candidateStatement;
			// see if current wrapper is the first statement in the method
			// and it is a log level check condition
			if (LogStatementUtils.isLogLevelCheckStatement(logWrapperifStatament)) {
				// check basics
				Statement thenStatement = logWrapperifStatament.getThenStatement();
				// typically it should be a block if containing exactly one
				// statement
				if ((thenStatement instanceof Block && ((Block) thenStatement).statements().size() > 1)) {
					lowConfidenceMethodLogCount++;
					LOG_LOW_CONFIDENCE.info("at "
							+ getCompilationUnit().getLineNumber(thenStatement.getStartPosition() - 1) + " in "
							+ getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName());
					LOG_LOW_CONFIDENCE.info(logWrapperifStatament);
					// TODO we may need to later add logger to such multi-block
					// logs
					return;
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
			}
		}
		// we only process if we have a log statement by now - else we go and
		// add
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
							|| !(arguments.get(expectedArgCount - 1) instanceof StringLiteral) || !isPossibleMehtodPhaseLogLiteral(
								((StringLiteral) arguments.get(expectedArgCount - 1)).getLiteralValue(),
								methodDeclaration.getName().getFullyQualifiedName()))) {
				lowConfidenceMethodLogCount++;
				LOG_LOW_CONFIDENCE.info("at " + getCompilationUnit().getLineNumber(logStatement.getStartPosition() - 1)
						+ " in " + getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName());
				LOG_LOW_CONFIDENCE.info(logWrapperifStatament != null ? logWrapperifStatament : logStatement);
				markForAddition(methodDeclaration, containerForInsertion, anchorForInsertion, LogType.SERVICE, phase);
				return;
			}

			String logString = ((StringLiteral) arguments.get(expectedArgCount - 1)).getLiteralValue();
			// check compliance
			String expectedLogStatement = getSourceClassName() + "#"
					+ methodDeclaration.getName().getFullyQualifiedName() + ":" + phase;

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
				compliantMethodLogCount++;
				LOG_COMPLIANT.info("at " + getCompilationUnit().getLineNumber(logStatement.getStartPosition() - 1)
						+ " in " + getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName());
				LOG_COMPLIANT.info(logWrapperifStatament != null ? logWrapperifStatament : logStatement);
			} else {
				highConfidenceMethodLogCount++;
				LOG_HIGH_CONFIDENCE.info("at "
						+ getCompilationUnit().getLineNumber(logStatement.getStartPosition() - 1) + " in "
						+ getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName());
				LOG_HIGH_CONFIDENCE.info(logWrapperifStatament != null ? logWrapperifStatament : logStatement);
				if (doRewrite) {
					ListRewrite listRewrite = getAstRewrite().getListRewrite(logStatement,
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
							getAstRewrite().set(logStatement, MethodInvocation.NAME_PROPERTY,
									getAstRewrite().getAST().newSimpleName("info"), null);
						}
					}
				}
			}
		} else {
			markForAddition(methodDeclaration, containerForInsertion, anchorForInsertion, LogType.SERVICE, phase);
		}
	}

	public int getLowConfidenceMethodStartLog() {
		return lowConfidenceMethodLogCount;
	}

	public int getHighConfidenceMethodStartLog() {
		return highConfidenceMethodLogCount;
	}

	public int getCompliantMethodStartLog() {
		return compliantMethodLogCount;
	}

	public int getAddedCount() {
		return addedCount;
	}

	protected void markForAddition(MethodDeclaration methodDeclaration, Block container, Statement anchorForInsertion,
			LogType logType, String phase) {
		if (CodeUtils.isEligibleForInsertion(methodDeclaration)) {
			LOG_NOT_PRESENT.info("Phase log not found at "
					+ getCompilationUnit().getLineNumber(methodDeclaration.getStartPosition() - 1) + " in "
					+ getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName());
			if (doRewrite) {
				String expectedLogStatement = getSourceClassName() + "#"
						+ methodDeclaration.getName().getFullyQualifiedName() + ":" + phase;
				String finalLogStr = LOG_STRING_SERVICE_TYPE.replace("#LOG_STR#", expectedLogStatement);
				if (logType == LogType.SERVICE) {
					// add log statement to method
					ListRewrite listRewrite = getAstRewrite().getListRewrite(container, Block.STATEMENTS_PROPERTY);
					Statement toInsert = (Statement) getAstRewrite().createStringPlaceholder(finalLogStr,
							ASTNode.IF_STATEMENT);
					if (anchorForInsertion == null) {
						listRewrite.insertFirst(toInsert, null);
					} else {
						listRewrite.insertBefore(toInsert, anchorForInsertion, null);
					}
				}
			}
			addedCount++;
		} else {
			LOG_NOT_PRESENT.info("Method ineligibe for phase log at "
					+ getCompilationUnit().getLineNumber(methodDeclaration.getStartPosition() - 1) + " in "
					+ getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName());
		}
	}

	protected abstract boolean isPossibleMehtodPhaseLogLiteral(String s, String methodName);

}
