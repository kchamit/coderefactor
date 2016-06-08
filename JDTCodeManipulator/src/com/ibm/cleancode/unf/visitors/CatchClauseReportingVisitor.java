package com.ibm.cleancode.unf.visitors;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;

import com.ibm.cleancode.framework.processor.CUAwareASTVisitor;

public class CatchClauseReportingVisitor extends CUAwareASTVisitor {
	private static final Logger LOGGER = Logger.getLogger(CatchClauseReportingFactoryImplVisitor.class);
	private static final Logger CATCH_CLAUSE_HIGH_CONFIDENCE = Logger.getLogger("CatchClauseHighConfidence");
	private static final Logger CATCH_CLAUSE_LOW_CONFIDENCE = Logger.getLogger("CatchClauseLowConfidence");
	private static final Logger CATCH_CLAUSE_COMPLIANT = Logger.getLogger("CatchClauseCompliant");
	private final List<MethodLogInspectionResult> results;
	private MethodDeclaration currentMethodDeclaration = null;
	private int emptyCatchClauseCount = 0;
	private int highConfidenceCatchClauseCount = 0;
	private int lowConfidenceCatchClauseCount = 0;

	public CatchClauseReportingVisitor() {
		results = new ArrayList<MethodLogInspectionResult>();
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
			// see if we have up to 2 statements inside
			CatchClause catchClause = (CatchClause) catchClauses.get(0);
			if (catchClause.getBody().statements().size() <= 2) {
				CATCH_CLAUSE_HIGH_CONFIDENCE.info("at "
						+ getCompilationUnit().getLineNumber(catchClause.getStartPosition() - 1) + " in "
						+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
				CATCH_CLAUSE_HIGH_CONFIDENCE.info(catchClause);
				highConfidenceCatchClauseCount++;
			}
			return true;
		}
		if (catchClauses.size() == 0) {
			System.err.println("No catch block: " + "at "
					+ getCompilationUnit().getLineNumber(tryStmt.getStartPosition() - 1) + " in "
					+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
			return true;
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

	public List<MethodLogInspectionResult> getResults() {
		return results;
	}
}