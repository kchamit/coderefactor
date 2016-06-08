package com.ibm.cleancode.unf.visitors;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.cleancode.framework.processor.CUAwareASTVisitor;

public class MethodEntryLogReportingExclussionVisitor extends CUAwareASTVisitor {
	private static final Logger LOGGER = Logger.getLogger(MethodEntryLogReportingExclussionVisitor.class);
	private final List<MethodLogInspectionResult> results;
	private int emptyMethodCount = 0;
	private PackageDeclaration currentPackage;

	public MethodEntryLogReportingExclussionVisitor() {
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
		if (!method.isConstructor() && isLogEligible(methodName)) {
			if (method.getBody() != null && method.getBody().statements().size() > 2) {
				MethodLogInspectionResult result = new MethodLogInspectionResult();
				results.add(result);
				if(currentPackage!=null){
					result.setPackageName(currentPackage.getName().getFullyQualifiedName());
				}
				result.setClassName(className);
				result.setMethodName(methodName);
				// check first statement
				Statement firstStatement = (Statement) method.getBody().statements().get(0);
				String expectedFormat = className + "#" + methodName + ":start";
				if (firstStatement instanceof IfStatement) {
					LogInspectionResult r = LogStatementUtils.isLevelCheckedLogStatement((IfStatement) firstStatement,
							expectedFormat);
					result.setLogInspectionResult(r);
				} else if (firstStatement instanceof ExpressionStatement) {
					LogInspectionResult r = LogStatementUtils.isLogStatement((ExpressionStatement) firstStatement,
							expectedFormat);
					result.setLogInspectionResult(r);
				}
			} else {
				LOGGER.warn("Empty method: " + className + "::" + methodName);
				System.out.println("Empty method: " + className + "::" + methodName);
				emptyMethodCount++;
			}

		}else {
			LOGGER.warn("Non eligible method: " + className + "::" + methodName);
			System.out.println("Non eligible method: " + className + "::" + methodName);
		}
		return true;
	}

	public int getEmptyMethodCount() {
		return emptyMethodCount;
	}

	public List<MethodLogInspectionResult> getResults() {
		return results;
	}
	private boolean isLogEligible(String methodName){
		boolean eligible = true;
		if(methodName.matches("(get.*|set.*|equals|hashCode|toString)")){
			eligible = false;
		}		
		return eligible;
	}
}