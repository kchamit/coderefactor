package com.ibm.cleancode.unf.visitors;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import com.ibm.cleancode.framework.processor.RewriteVisitor;

public class CatchClauseExacptionEatingCorrectionAllVisitor extends RewriteVisitor {
	private static final Logger EXCEPTION_TYPE_LOGGER = Logger.getLogger("ExceptionType");
	private static final Logger CATCH_CLAUSE_HIGH_CONFIDENCE = Logger.getLogger("CatchClauseHighConfidence");
	private MethodDeclaration currentMethodDeclaration = null;
	private int exceptionEaterCatchClause = 0;
	private boolean doRewrite = false;

	public CatchClauseExacptionEatingCorrectionAllVisitor(boolean doRewrite) {
		super();
		this.doRewrite = doRewrite;
	}

	public boolean visit(MethodDeclaration methodDeclaration) {
		currentMethodDeclaration = methodDeclaration;
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
		// see if we have only 1 statement in catch clause and it is a handle
		// invocation
		boolean hasLog = false;
		boolean hasHandle = false;
		boolean hasOther = false;
		for (Object oStatement : catchClause.getBody().statements()) {
			if (oStatement instanceof ExpressionStatement) {
				ExpressionStatement expressionStatement = (ExpressionStatement) oStatement;
				if (expressionStatement.getExpression() instanceof MethodInvocation) {
					if (CatchStatementUtils.isHandleCall((MethodInvocation) expressionStatement.getExpression())) {
						return true;
					} else if (CatchStatementUtils.isLoggerOrPrintStackTraceCall((MethodInvocation) expressionStatement
							.getExpression())) {
						hasLog = true;
					} else {
						hasOther = true;
					}
				} else {
					hasOther = true;
				}
			} else if (oStatement instanceof IfStatement) {
				if (LogStatementUtils.isLogLevelCheckStatement((IfStatement) oStatement)) {
					hasLog = true;
				} else {
					hasOther = true;
				}
			} else {
				hasOther = true;
			}
		}
		if (catchClause.getBody().statements().size() == 0 || (hasLog && !hasOther && !hasHandle)) {
			// must be only a log
			CATCH_CLAUSE_HIGH_CONFIDENCE.info("at "
					+ getCompilationUnit().getLineNumber(catchClause.getStartPosition() - 1) + " in "
					+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
			CATCH_CLAUSE_HIGH_CONFIDENCE.info(catchClause);
			exceptionEaterCatchClause++;
			if (doRewrite) {
				// add a throws clause
				ThrowStatement throwStatement = getCompilationUnit().getAST().newThrowStatement();
				throwStatement.setExpression(getCompilationUnit().getAST().newName(
						catchClause.getException().getName().getFullyQualifiedName()));
				ListRewrite catchClauseStatementslistRewrite = getAstRewrite().getListRewrite(catchClause.getBody(),
						Block.STATEMENTS_PROPERTY);
				catchClauseStatementslistRewrite.insertLast(throwStatement, null);

				// add to throws
				SimpleName exceptionName = getCompilationUnit().getAST().newSimpleName(
						catchClause.getException().getType().toString());
				ListRewrite methodThrowsListRewrite = getAstRewrite().getListRewrite(currentMethodDeclaration,
						MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
				methodThrowsListRewrite.insertLast(exceptionName, null);
			}
		}
		return true;
	}

	public int getExceptionEaterCatchClause() {
		return exceptionEaterCatchClause;
	}

	public void setExceptionEaterCatchClause(int exceptionEaterCatchClause) {
		this.exceptionEaterCatchClause = exceptionEaterCatchClause;
	}
}