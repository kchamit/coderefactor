package com.ibm.cleancode.framework.utility;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.ibm.cleancode.framework.processor.StatementCountVisitor;

public class CodeUtils {
	public static boolean isEligibleForInsertion(MethodDeclaration md) {
		String methodName = md.getName().getFullyQualifiedName();
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
