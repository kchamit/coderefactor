package com.ibm.cleancode.unf.visitors;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.cleancode.framework.processor.CUAwareASTVisitor;

public class ReturnExitLogReportingVisitor extends CUAwareASTVisitor {
	private static final Logger LOGGER = Logger.getLogger(ReturnExitLogReportingVisitor.class);
	private final List<MethodLogInspectionResult> results;
	private int emptyMethodCount = 0;
	private PackageDeclaration currentPackage;
	private String currentClassName;
	private String currentMethodName;
	private boolean applyMethodFilter;

	public ReturnExitLogReportingVisitor() {
		results = new ArrayList<MethodLogInspectionResult>();
	}

	public boolean visit(PackageDeclaration packageDecl) {
		currentPackage = packageDecl;
		return true;
	}

	public boolean visit(TypeDeclaration type) {
		if (type.isInterface()) {
			System.out.println("Skipping interface!!" + type.getName().getFullyQualifiedName());
			return false;
		}
		return true;
	}

	public boolean visit(MethodDeclaration method) {
		String className = null;
		ASTNode parent = method.getParent();
		if (parent instanceof TypeDeclaration) {
			className = ((TypeDeclaration) parent).getName().getIdentifier();
			if(className.contains("_")){
				System.out.println("Auto generated class " + className);
				return false;
			}
		} else {
			// skips anonymous classes
			return true;
		}

		String methodName = method.getName().getIdentifier();

		if(applyMethodFilter){
			if (LogStatementUtils.isMethodLogEligible(method)) {
				currentClassName = className;
				currentMethodName = methodName;
			}else {
				LOGGER.info("Non eligible method: " + className + ": " + methodName);
				return false;
			}
		} else{
			currentClassName = className;
			currentMethodName = methodName;
		}
		return true;
	}
	
	public void endVisit(MethodDeclaration methodDeclaration) {
		currentClassName = null;
		currentMethodName = null;
	}
	
	public boolean visit(ReturnStatement returnStatement) {
		//System.out.println("Return visit inside: " + currentMethodName);
		ASTNode parent = returnStatement.getParent();
		Statement statementBeforeReturn = null;
		if(parent instanceof Block){
			Block block = (Block)parent;
			@SuppressWarnings("unchecked")
			List<Statement>  statements = block.statements();
			int stSize = statements.size();
			int returnPos = 0;
			for(int i=0; i<stSize; i++){
				Object statement = statements.get(i);
				//System.out.println("Statement...");
				//System.out.println(statement);
				if(statement instanceof ReturnStatement){
					returnPos = i-1;
					break;
				}
			}
			if(returnPos>0){
				statementBeforeReturn = (Statement) statements.get(returnPos);
			}else{
				statementBeforeReturn = (Statement) statements.get(0);
				/*if(Statement.METHOD_INVOCATION == statementBeforeReturn.getNodeType()){
					
				}*/
			}
			
		} else if(parent instanceof IfStatement){
			statementBeforeReturn = (IfStatement) parent;
			//System.out.println("statementBeforeReturn: "+statementBeforeReturn);
		} else{
			System.out.println("Unknown statement !!!");
		}
		MethodLogInspectionResult result = new MethodLogInspectionResult();
		results.add(result);
		if(currentPackage!=null){
			result.setPackageName(currentPackage.getName().getFullyQualifiedName());
		}
		result.setClassName(currentClassName);
		result.setMethodName(currentMethodName);
		// check first statement
		String expectedFormat = currentClassName + "#" + currentMethodName + ":end";
		if (statementBeforeReturn instanceof IfStatement) {
			LogInspectionResult r = LogStatementUtils.isLevelCheckedLogStatement((IfStatement) statementBeforeReturn,
					expectedFormat);
			result.setLogInspectionResult(r);
		} else if (statementBeforeReturn instanceof ExpressionStatement) {
			LogInspectionResult r = LogStatementUtils.isLogStatement((ExpressionStatement) statementBeforeReturn,
					expectedFormat);
			result.setLogInspectionResult(r);
		}
		
		return true;
	}

	public int getEmptyMethodCount() {
		return emptyMethodCount;
	}

	public List<MethodLogInspectionResult> getResults() {
		return results;
	}
	
}