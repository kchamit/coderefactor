package com.ibm.cleancode.unf.visitors;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.TryStatement;

import com.ibm.cleancode.framework.processor.CUAwareASTVisitor;

public class ReturnStatementReportingVisitor extends CUAwareASTVisitor {
	private static final Logger LOGGER = Logger.getLogger(CatchClauseReportingFactoryImplVisitor.class);
	private static final Logger CATCH_CLAUSE_HIGH_CONFIDENCE = Logger.getLogger("CatchClauseHighConfidence");
	private static final Logger CATCH_CLAUSE_LOW_CONFIDENCE = Logger.getLogger("CatchClauseLowConfidence");
	private static final Logger CATCH_CLAUSE_COMPLIANT = Logger.getLogger("CatchClauseCompliant");
	private final List<MethodLogInspectionResult> results;
	private MethodDeclaration currentMethodDeclaration = null;
	private int emptyCatchClauseCount = 0;
	private int highConfidenceCatchClauseCount = 0;
	private int lowConfidenceCatchClauseCount = 0;

	public ReturnStatementReportingVisitor() {
		results = new ArrayList<MethodLogInspectionResult>();
	}

	public boolean visit(ReturnStatement rs) {
		if (rs.getParent() instanceof Block) {
			Block parent = (Block) rs.getParent();
		} else {
			System.err.println("Return not inside block!!");
			System.out.println(rs.getParent());
		}
		return false;
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