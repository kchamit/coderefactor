package com.ibm.cleancode.unf.visitors;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import com.ibm.cleancode.framework.processor.RewriteVisitor;

public class TryBlockRemovingVisitor extends RewriteVisitor {
	private static final Logger CATCH_CLAUSE_HIGH_CONFIDENCE = Logger.getLogger("CatchClauseHighConfidence");
	private static final Logger CATCH_CLAUSE_LOW_CONFIDENCE = Logger.getLogger("CatchClauseLowConfidence");
	private MethodDeclaration currentMethodDeclaration = null;
	private int emptyCatchClauseCount = 0;
	private int highConfidenceCatchClauseCount = 0;
	private int lowConfidenceCatchClauseCount = 0;
	private boolean doReWrite = false;

	public TryBlockRemovingVisitor(boolean doReWrite) {
		this.doReWrite = doReWrite;
	}

	public boolean visit(MethodDeclaration methodDeclaration) {
		currentMethodDeclaration = methodDeclaration;
		return true;
	}

	public void endVisit(MethodDeclaration methodDeclaration) {
		currentMethodDeclaration = null;
	}

	public boolean visit(TryStatement tryStmt) {
		@SuppressWarnings("rawtypes")
		List catchClauses = tryStmt.catchClauses();
		if (currentMethodDeclaration == null) {
			System.err.println("Try block in static initializer " + "at "
					+ getCompilationUnit().getLineNumber(tryStmt.getStartPosition() - 1) + " in "
					+ getSourceClassName());
			return true;
		}
		if (catchClauses != null && catchClauses.size() == 1) {
			CatchClause catchClause = (CatchClause) catchClauses.get(0);
			if (catchClause.getException().getType().toString().equals("Exception")) {
				CATCH_CLAUSE_HIGH_CONFIDENCE.info("at "
						+ getCompilationUnit().getLineNumber(catchClause.getStartPosition() - 1) + " in "
						+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
				CATCH_CLAUSE_HIGH_CONFIDENCE.info(catchClause);
				highConfidenceCatchClauseCount++;
				if (doReWrite) {
					ASTRewrite astRewrite = getAstRewrite();
					astRewrite.replace(tryStmt, tryStmt.getBody(), null);
				}
				return true;
			}
		}
		CatchClause catchClause = (CatchClause) catchClauses.get(0);
		CATCH_CLAUSE_LOW_CONFIDENCE.info("at " + getCompilationUnit().getLineNumber(catchClause.getStartPosition() - 1)
				+ " in " + getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
		for (Object oCatchClause : catchClauses) {
			CATCH_CLAUSE_LOW_CONFIDENCE.info(oCatchClause);
		}
		lowConfidenceCatchClauseCount++;
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

}