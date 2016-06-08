package com.ibm.cleancode.framework.processor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class StatementCountVisitor extends ASTVisitor {
	private int count = 0;

	public boolean visit(AssertStatement s) {
		count++;
		return true;
	};

	public boolean visit(BreakStatement s) {
		count++;
		return true;
	};

	public boolean visit(ConstructorInvocation s) {
		count++;
		return true;
	};

	public boolean visit(ContinueStatement s) {
		count++;
		return true;
	};

	public boolean visit(DoStatement s) {
		count++;
		return true;
	};

	public boolean visit(EmptyStatement s) {
		count++;
		return true;
	};

	public boolean visit(EnhancedForStatement s) {
		count++;
		return true;
	};

	public boolean visit(ExpressionStatement s) {
		count++;
		return true;
	};

	public boolean visit(ForStatement s) {
		count++;
		return true;
	};

	public boolean visit(IfStatement s) {
		count++;
		return true;
	};

	public boolean visit(LabeledStatement s) {
		count++;
		return true;
	};

	public boolean visit(ReturnStatement s) {
		count++;
		return true;
	};

	public boolean visit(SuperConstructorInvocation s) {
		count++;
		return true;
	};

	public boolean visit(SwitchCase s) {
		count++;
		return true;
	};

	public boolean visit(SwitchStatement s) {
		count++;
		return true;
	};

	public boolean visit(SynchronizedStatement s) {
		count++;
		return true;
	};

	public boolean visit(ThrowStatement s) {
		count++;
		return true;
	};

	public boolean visit(TryStatement s) {
		count++;
		return true;
	};

	public boolean visit(TypeDeclarationStatement s) {
		count++;
		return true;
	};

	public boolean visit(VariableDeclarationStatement s) {
		count++;
		return true;
	};

	public boolean visit(WhileStatement s) {
		count++;
		return true;
	};

	public int getCount() {
		return count;
	}
}
