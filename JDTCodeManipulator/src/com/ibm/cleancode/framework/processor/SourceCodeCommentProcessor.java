package com.ibm.cleancode.framework.processor;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Document;

import com.ibm.cleancode.unf.visitors.CommentReportingVisitor;

public class SourceCodeCommentProcessor {


	public String start(String sourceCode) throws Exception {
		return process(sourceCode);
	}

	protected String process(String sourceCode) throws Exception {
		Document document = new Document(sourceCode);
		// parse compilation unit
		CompilationUnit cu = parse(document);
		CommentReportingVisitor visitor = new CommentReportingVisitor(sourceCode, cu);
		for (Comment comment : (List<Comment>) cu.getCommentList()) {
			comment.accept(visitor);
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
