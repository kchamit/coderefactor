package com.ibm.cleancode.unf.visitors;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.sonar.squidbridge.recognizer.CodeRecognizer;
import org.sonar.squidbridge.text.JavaFootprint;

public class CommentReportingVisitor extends ASTVisitor {
	private static final Logger LOGGER = Logger.getLogger(CatchClauseReportingFactoryImplVisitor.class);
	private static final Logger CATCH_CLAUSE_HIGH_CONFIDENCE = Logger.getLogger("CatchClauseHighConfidence");
	private MethodDeclaration currentMethodDeclaration = null;
	private int commentedCodeCount = 0;
	private String sourceCode;
	private CompilationUnit compliationUnit;
	private CodeRecognizer codeRecognizer = new CodeRecognizer(0.9d, new JavaFootprint());

	public CommentReportingVisitor(String sourceCode, CompilationUnit compliationUnit) {
		super();
		this.sourceCode = sourceCode;
		this.compliationUnit = compliationUnit;
	}

	public boolean visit(BlockComment comment) {
		int start = comment.getStartPosition();
		int end = start + comment.getLength();
		String commentStr = sourceCode.substring(start, end);
		if (codeRecognizer.isLineOfCode(commentStr)) {
			CATCH_CLAUSE_HIGH_CONFIDENCE.info("Commented code " + "at "
					+ compliationUnit.getLineNumber(comment.getStartPosition() - 1));
			CATCH_CLAUSE_HIGH_CONFIDENCE.info(commentStr);
			commentedCodeCount++;
		}
		return true;
	}
	
	public boolean visit(LineComment comment) {
		int start = comment.getStartPosition();
		int end = start + comment.getLength();
		String commentStr = sourceCode.substring(start, end);
		if (codeRecognizer.isLineOfCode(commentStr)) {
			CATCH_CLAUSE_HIGH_CONFIDENCE.info("Commented code " + "at "
					+ compliationUnit.getLineNumber(comment.getStartPosition() - 1));
			CATCH_CLAUSE_HIGH_CONFIDENCE.info(commentStr);
			commentedCodeCount++;
		}
		return true;
	}

	public int getCommentedCodeCount() {
		return commentedCodeCount;
	}

}