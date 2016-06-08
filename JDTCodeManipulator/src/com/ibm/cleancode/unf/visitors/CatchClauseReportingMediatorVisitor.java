package com.ibm.cleancode.unf.visitors;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;

import com.ibm.cleancode.framework.processor.CUAwareASTVisitor;

public class CatchClauseReportingMediatorVisitor extends CUAwareASTVisitor {
	private static final Logger EXCEPTION_TYPE_LOGGER = Logger.getLogger("ExceptionType");
	private static final Logger CATCH_CLAUSE_HIGH_CONFIDENCE = Logger.getLogger("CatchClauseHighConfidence");
	private static final Logger CATCH_CLAUSE_LOW_CONFIDENCE = Logger.getLogger("CatchClauseLowConfidence");
	private static final Logger CATCH_CLAUSE_COMPLIANT = Logger.getLogger("CatchClauseCompliant");
	private static final Logger CATCH_CLAUSE_METHOD_THROWS = Logger.getLogger("MethodThrows");
	private final List<MethodLogInspectionResult> results;
	private MethodDeclaration currentMethodDeclaration = null;
	private int emptyCatchClauseCount = 0;
	private int highConfidenceCatchClauseCount = 0;
	private int lowConfidenceCatchClauseCount = 0;
	private int compliantCatchClauseCount = 0;

	public CatchClauseReportingMediatorVisitor() {
		results = new ArrayList<MethodLogInspectionResult>();
	}

	public boolean visit(MethodDeclaration methodDeclaration) {
		currentMethodDeclaration = methodDeclaration;
		if (methodDeclaration.thrownExceptions() != null && methodDeclaration.thrownExceptions().size() != 0) {
			StringBuffer exceptionList = new StringBuffer();
			for (Object oThrowsException : methodDeclaration.thrownExceptions()) {
				exceptionList.append(oThrowsException.toString()).append(",");
			}
			CATCH_CLAUSE_METHOD_THROWS.info(" at "
					+ getCompilationUnit().getLineNumber(methodDeclaration.getStartPosition() - 1) + " in "
					+ getSourceClassName() + ":" + methodDeclaration.getName().getFullyQualifiedName() + " => "
					+ exceptionList);
		}
		return true;
	}

	public void endVisit(MethodDeclaration methodDeclaration) {
		currentMethodDeclaration = null;
	}

	public boolean visit(ThrowStatement throwStatement) {
		EXCEPTION_TYPE_LOGGER.info(throwStatement.getExpression() + "at "
				+ getCompilationUnit().getLineNumber(throwStatement.getStartPosition() - 1) + " in "
				+ getSourceClassName());
		return true;
	}

	public boolean visit(CatchClause catchClause) {
		EXCEPTION_TYPE_LOGGER.info("Catches: " + catchClause.getException().getType() + "at "
				+ getCompilationUnit().getLineNumber(catchClause.getStartPosition() - 1) + " in "
				+ getSourceClassName());
		if (currentMethodDeclaration == null) {
			System.err.println("Catch block in static initializer at "
					+ getCompilationUnit().getLineNumber(catchClause.getStartPosition() - 1) + " in "
					+ getSourceClassName());
			return true;
		}
		if (catchClause.getBody() == null || catchClause.getBody().statements().size() == 0) {
			CATCH_CLAUSE_HIGH_CONFIDENCE.info("Empty catch clause at "
					+ getCompilationUnit().getLineNumber(catchClause.getStartPosition() - 1) + " in "
					+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
			CATCH_CLAUSE_HIGH_CONFIDENCE.info(catchClause);
			emptyCatchClauseCount++;
			return true;
		}
		// see if we have only 1 statement in catch clause and it is a handle
		// invocation
		boolean hasLog = false;
		boolean hasHandle = false;
		boolean hasOther = false;
		for (Object oStatement : catchClause.getBody().statements()) {
			if (oStatement instanceof ThrowStatement) {
				hasHandle = true;
			} else if (oStatement instanceof ExpressionStatement) {
				ExpressionStatement expressionStatement = (ExpressionStatement) oStatement;
				if (expressionStatement.getExpression() instanceof MethodInvocation) {
					if (CatchStatementUtils.isHandleCall((MethodInvocation) expressionStatement.getExpression())) {
						hasHandle = true;
					} else if (CatchStatementUtils.isLoggerOrPrintStackTraceCall((MethodInvocation) expressionStatement
							.getExpression())) {
						hasLog = true;
					} else {
						hasOther = true;
					}
				} else {
					hasOther = true;
				}
			} else {
				hasOther = true;
			}
		}
		if (hasOther) {
			CATCH_CLAUSE_LOW_CONFIDENCE.info("at "
					+ getCompilationUnit().getLineNumber(catchClause.getStartPosition() - 1) + " in "
					+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
			CATCH_CLAUSE_LOW_CONFIDENCE.info(catchClause);
			lowConfidenceCatchClauseCount++;
			return true;
		}
		// at this point we are sure it does not have anything else other than
		// log and handle
		if (hasHandle) {
			// if we do have handle it is in compliance
			CATCH_CLAUSE_COMPLIANT.info("at " + getCompilationUnit().getLineNumber(catchClause.getStartPosition() - 1)
					+ " in " + getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
			CATCH_CLAUSE_COMPLIANT.info(catchClause);
			compliantCatchClauseCount++;
		} else {
			// must be only a log
			CATCH_CLAUSE_HIGH_CONFIDENCE.info("at "
					+ getCompilationUnit().getLineNumber(catchClause.getStartPosition() - 1) + " in "
					+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
			CATCH_CLAUSE_HIGH_CONFIDENCE.info(catchClause);
			highConfidenceCatchClauseCount++;
		}
		return true;
	}

	public int getEmptyCatchClauseCount() {
		return emptyCatchClauseCount;
	}

	public int getHighConfidenceCatchClauseCount() {
		return highConfidenceCatchClauseCount;
	}

	public int getLowConfidenceCatchClauseCount() {
		return lowConfidenceCatchClauseCount;
	}

	public int getCompliantCatchClauseCount() {
		return compliantCatchClauseCount;
	}

	public List<MethodLogInspectionResult> getResults() {
		return results;
	}
}