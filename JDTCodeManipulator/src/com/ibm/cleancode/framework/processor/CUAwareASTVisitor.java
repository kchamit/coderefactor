package com.ibm.cleancode.framework.processor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class CUAwareASTVisitor extends ASTVisitor {
	private CompilationUnit cu;

	public void setCompilationUnit(CompilationUnit cu) {
		this.cu = cu;
	}

	public CompilationUnit getCompilationUnit() {
		return cu;
	}

	public String getSourceClassName() {
		return ((TypeDeclaration) getCompilationUnit().types().get(0)).getName().getFullyQualifiedName();
	}
}
