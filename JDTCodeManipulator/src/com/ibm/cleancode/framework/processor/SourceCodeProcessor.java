package com.ibm.cleancode.framework.processor;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

public class SourceCodeProcessor {

	private CUAwareASTVisitor visitor;

	public SourceCodeProcessor(CUAwareASTVisitor visitor) {
		super();
		this.visitor = visitor;
	}

	public String start(String sourceCode) throws Exception {
		return process(sourceCode);
	}

	protected String process(String sourceCode) throws Exception {
		boolean isWewrite = false;
		ASTRewrite rewrite = null;
		Document document = new Document(sourceCode);
		// parse compilation unit
		CompilationUnit cu = parse(document);
		visitor.setCompilationUnit(cu);
		// see if rewrite requested
		if (visitor instanceof RewriteVisitor) {
			rewrite = ASTRewrite.create(cu.getAST());
			((RewriteVisitor) visitor).setAstRewrite(rewrite);
			isWewrite = true;
		}
		cu.accept(visitor);
		if (isWewrite) {
			TextEdit edits = rewrite.rewriteAST(document, null);
			edits.apply(document);
			return document.get();
		}
		return null;
	}

	private static CompilationUnit parse(Document document) throws Exception {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(document.get().toCharArray());
		return (CompilationUnit) parser.createAST(null);
	}
}
