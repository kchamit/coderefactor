package com.ibm.cleancode.unf.visitors;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

import com.ibm.cleancode.framework.processor.RewriteVisitor;

public class DebugLogCorrectionVisitor extends RewriteVisitor {
	private static final Logger LOGGER = Logger.getLogger(DebugLogCorrectionVisitor.class);
	private final List<MethodLogInspectionResult> results;
	private int issueCount = 0;
	private MethodDeclaration currentMethodDeclaration = null;

	// do rewrite
	boolean doRewrite = false;

	public DebugLogCorrectionVisitor(boolean doRewrite) {
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
			if (!isFirstStatementInMethod(es, suscepectedLogCheckWrapper, logLevel)) {
				// only interested in debug and info for now
				handleGeneralLogStatement(es, suscepectedLogCheckWrapper, logLevel);
			}
		}
		return false;
	}

	private void handleGeneralLogStatement(MethodInvocation es, ASTNode suscepectedLogCheckWrapper, String logLevel) {
		if ("DEBUG".equals(logLevel) || "INFO".equals(logLevel)) {
			if (suscepectedLogCheckWrapper instanceof IfStatement) {
				// check if it is a wrapper for log level check
				if (!LogStatementUtils.isLogLevelCheckStatement((IfStatement) suscepectedLogCheckWrapper)) {
					LOGGER.error("[" + logLevel + "]Log inside if statement but not wrapped in level checking at "
							+ getCompilationUnit().getLineNumber(es.getStartPosition() - 1) + " in "
							+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
					issueCount++;
				}
			} else {
				LOGGER.warn("[" + logLevel + "]Log not inside an if statement["
						+ suscepectedLogCheckWrapper.getClass().getSimpleName() + "] at "
						+ getCompilationUnit().getLineNumber(es.getStartPosition() - 1) + " in " + getSourceClassName()
						+ ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
				issueCount++;
			}
		}
	}

	private boolean isFirstStatementInMethod(MethodInvocation es, ASTNode suscepectedLogCheckWrapper, String logLevel) {
		if (currentMethodDeclaration != null) {
			Object firstStatementInMethod = currentMethodDeclaration.getBody().statements().get(0);
			// must be an if statement
			if (firstStatementInMethod instanceof IfStatement) {
				IfStatement firstIfStatementInMethod = (IfStatement) firstStatementInMethod;
				// see if current wrapper is the first statement in the method
				// and it is a log level check condition
				if (suscepectedLogCheckWrapper.equals(firstStatementInMethod)
						&& LogStatementUtils.isLogLevelCheckStatement(firstIfStatementInMethod)) {
					return true;
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

}