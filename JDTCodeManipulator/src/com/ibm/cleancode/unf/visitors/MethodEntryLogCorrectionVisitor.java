package com.ibm.cleancode.unf.visitors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;

public class MethodEntryLogCorrectionVisitor extends AbstractMethodLogCorrectionVisitor {

	public MethodEntryLogCorrectionVisitor(boolean doRewrite) {
		super(doRewrite);
	}

	public boolean visit(MethodDeclaration methodDeclaration) {
		// get first statement
		if (methodDeclaration.getBody() == null || methodDeclaration.getBody().statements().size() == 0) {
			return false;
		}
		Statement firstStatementInMethod = (Statement) methodDeclaration.getBody().statements().get(0);
		checkAndRewriteLog(methodDeclaration, firstStatementInMethod, methodDeclaration.getBody(), null, "start");
		return false;
	}

	@Override
	protected boolean isPossibleMehtodPhaseLogLiteral(String s, String methodName) {
		return StringUtils.containsIgnoreCase(s, "inside") || StringUtils.containsIgnoreCase(s, "called")
				|| StringUtils.containsIgnoreCase(s, "start") || StringUtils.containsIgnoreCase(s, "enter")
				|| StringUtils.containsIgnoreCase(s, methodName);
	}

}