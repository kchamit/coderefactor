package com.ibm.cleancode.unf.processor;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

public class JDTCatchProcessor {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String fileName = "C:/Dev/UNF-Rsa-SIT3/RsaBatchWeb/src/com/unifirst/cni010/rsa/batch/servlet/ExportContractInfoCNI010Servlet.java";
		final String source = FileUtils.readFileToString(new File(fileName), "cp1252");
		Document document = new Document(source);
		// parse compilation unit
		CompilationUnit cu = parse(document);
		final ASTRewrite rewrite = ASTRewrite.create(cu.getAST());

		cu.accept(new ASTVisitor() {
			public boolean visit(CatchClause node) {

				System.out.println(node.getBody());
				ListRewrite listRewrite = rewrite.getListRewrite(node.getBody(), Block.STATEMENTS_PROPERTY);

				// AST nAst = node.getAST();
				// Block nBlock = nAst.newBlock();

				Statement placeHolder = (Statement) rewrite.createStringPlaceholder(
						"//added comment in place of printStackTrace", ASTNode.EMPTY_STATEMENT);
				for (Object oRewriteNode : listRewrite.getOriginalList()) {
					ASTNode rewriteNode = (ASTNode) oRewriteNode;
					if (rewriteNode.toString().contains("printStackTrace")) {
						listRewrite.replace(rewriteNode, placeHolder, null);
					}
				}

				// nBlock.statements().add(placeHolder);
				// rewrite.replace(node.getBody(), nBlock, null);

				// listRewrite.insertFirst(placeHolder, null);

				return true;
			}

		});

		TextEdit edits = rewrite.rewriteAST(document, null);
		edits.apply(document);
		FileUtils.write(new File(fileName), document.get(), "cp1252");
		// System.out.println(document.get());
	}

	private static CompilationUnit parse(Document document) throws Exception {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(document.get().toCharArray());
		// parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}

	
}
