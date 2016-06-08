package com.ibm.cleancode.framework.processor;

import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class RewriteVisitor extends CUAwareASTVisitor {
	private ASTRewrite astRewrite;

	public ASTRewrite getAstRewrite() {
		return astRewrite;
	}

	public void setAstRewrite(ASTRewrite astRewrite) {
		this.astRewrite = astRewrite;
	}

}
