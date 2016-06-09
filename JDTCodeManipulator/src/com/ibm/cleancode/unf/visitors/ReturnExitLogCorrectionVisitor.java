package com.ibm.cleancode.unf.visitors;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import com.ibm.cleancode.framework.processor.RewriteVisitor;
import com.ibm.cleancode.unf.visitors.LogInspectionResult.LogType;

public class ReturnExitLogCorrectionVisitor extends RewriteVisitor {
	private static final Logger EXIT_LOG_HIGH_CONFIDENCE = Logger.getLogger("MethodExitLogHighConfidence");
	private static final Logger EXIT_LOG_LOW_CONFIDENCE = Logger.getLogger("MethodExitLogLowConfidence");
	private static final Logger EXIT_LOG_COMPLIANT = Logger.getLogger("MethodExitLogCompliant");
	private static final Logger LOGGER = Logger.getLogger(ReturnExitLogCorrectionVisitor.class);
	private final List<MethodLogInspectionResult> results;
	private int issueCount = 0;
	private int lowConfidenceMethodEndLog = 0;
	private int highConfidenceMethodEndLog = 0;
	private int compliantMethodEndLog = 0;
	private MethodDeclaration currentMethodDeclaration = null;
	private PackageDeclaration currentPackage;
	private String currentClassName;
	private String currentMethodName;
	private boolean applyMethodFilter;

	/**
	 * @return the lowConfidenceMethodEndLog
	 */
	public int getLowConfidenceMethodEndLog() {
		return lowConfidenceMethodEndLog;
	}

	/**
	 * @return the highConfidenceMethodEndLog
	 */
	public int getHighConfidenceMethodEndLog() {
		return highConfidenceMethodEndLog;
	}

	/**
	 * @return the compliantMethodEndLog
	 */
	public int getCompliantMethodEndLog() {
		return compliantMethodEndLog;
	}
	// do rewrite
	boolean doRewrite = false;

	public ReturnExitLogCorrectionVisitor(boolean doRewrite) {
		results = new ArrayList<MethodLogInspectionResult>();
		this.doRewrite = doRewrite;
	}

	public boolean visit(MethodDeclaration method) {
		currentMethodDeclaration = method;
		String className = null;
		ASTNode parent = method.getParent();
		if (parent instanceof TypeDeclaration) {
			className = ((TypeDeclaration) parent).getName().getIdentifier();
			if(className.contains("_")){
				LOGGER.info("Auto generated class " + className);
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
		currentMethodDeclaration = null;
		currentClassName = null;
		currentMethodName = null;
	}
	
	public boolean visit(PackageDeclaration packageDecl) {
		currentPackage = packageDecl;
		return true;
	}

	public boolean visit(TypeDeclaration type) {
		if (type.isInterface()) {
			LOGGER.info("Skipping interface: " + type.getName().getFullyQualifiedName());
			return false;
		}
		return true;
	}
	
	public boolean visit(ReturnStatement returnStatement) {
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
				if(statement instanceof ReturnStatement){
					returnPos = i;
					break;
				}
			}
			if(returnPos>0){
				statementBeforeReturn = (Statement) statements.get(returnPos-1);
			}else{
				statementBeforeReturn = (Statement) statements.get(0);
			}
			
		} else if(parent instanceof IfStatement){
			statementBeforeReturn = (IfStatement) parent;
		} else{
			LOGGER.info("Unknown statement !!!");
			return true;
		}
		Expression ex = null;
		boolean isMethodInvocation = false;
		if(statementBeforeReturn instanceof IfStatement){
			Statement thenStatement = ((IfStatement) statementBeforeReturn).getThenStatement();
			if(thenStatement instanceof Block){
				thenStatement = (Block)thenStatement;
				@SuppressWarnings("unchecked")
				List<Statement> statements = ((Block) thenStatement).statements();
				if(statements != null && statements.size() == 1){
					if(statements.get(0) instanceof ExpressionStatement){
						ex = ((ExpressionStatement)statements.get(0)).getExpression();
					}else{
						return true;
					}					
				}else{
					// more than one statement can't be a log wrapper
					return true;
				}
			}			
			if(ex instanceof MethodInvocation){
				isMethodInvocation = true;
			}else{
				LOGGER.info("Not a logger !!!");
				return true;
			}
		}else if(statementBeforeReturn instanceof ExpressionStatement){
			ex = ((ExpressionStatement)statementBeforeReturn).getExpression();			
			if(ex instanceof MethodInvocation){
				isMethodInvocation = true;
			}else{
				LOGGER.info("Not a logger, may need to put logger manually");
				return true;
			}
		}else{
			LOGGER.info("Not a logger, may need to put logger manually");
			return true;
		}
		
		if(isMethodInvocation){
			if (LogStatementUtils.isLoggerCall((MethodInvocation)ex)) {
				// to see if wrapped in if statement
				ASTNode suscepectedLogCheckWrapper = ((MethodInvocation)ex).getParent().getParent().getParent();
				// extract log level
				String logLevel = ((MethodInvocation)ex).getName().getFullyQualifiedName().toUpperCase();
				// handle method entry log
				handleMethodExitLogStatement((MethodInvocation)ex, suscepectedLogCheckWrapper, logLevel);
			}
		}
		
		return true;
	}
	
	private boolean handleMethodExitLogStatement(MethodInvocation es, ASTNode suscepectedLogCheckWrapper, String logLevel){
		if(currentMethodDeclaration != null){
			
			MethodLogInspectionResult result = new MethodLogInspectionResult();
			results.add(result);
			if(currentPackage!=null){
				result.setPackageName(currentPackage.getName().getFullyQualifiedName());
			}
			result.setClassName(currentClassName);
			result.setMethodName(currentMethodName);
			
			String expectedFormat = currentClassName + "#" + currentMethodName + ":end";
			
			// level check method name
			String levelCheckMethodName = null;
			LogInspectionResult r = null;
			if (suscepectedLogCheckWrapper instanceof IfStatement) {
				Expression exp = ((IfStatement)suscepectedLogCheckWrapper).getExpression();
				MethodInvocation m = null;
				if(exp instanceof MethodInvocation){
					m = (MethodInvocation)exp;
				}else{
					return true;
				}				
				levelCheckMethodName = m.getName().getFullyQualifiedName();
				r = isLevelCheckedLogStatement((IfStatement) suscepectedLogCheckWrapper, expectedFormat, levelCheckMethodName, logLevel);
				result.setLogInspectionResult(r);
			} else if (suscepectedLogCheckWrapper instanceof ExpressionStatement) {
				Expression exp = ((Expression)suscepectedLogCheckWrapper);
				MethodInvocation m = null;
				if(exp instanceof MethodInvocation){
					m = (MethodInvocation)exp;
				}else{
					return true;
				}
				levelCheckMethodName = m.getName().getFullyQualifiedName();
				r = isLogStatement((ExpressionStatement) suscepectedLogCheckWrapper, expectedFormat, levelCheckMethodName, logLevel, null);
				result.setLogInspectionResult(r);
			}else{
				System.err.println("Orphan logger at " + getCompilationUnit().getLineNumber(es.getStartPosition() - 1) + " in " + getSourceClassName() + ": "
						+ currentMethodDeclaration.getName().getFullyQualifiedName());
				LOGGER.info("Orphan logger at " + getCompilationUnit().getLineNumber(es.getStartPosition() - 1) + " in " + getSourceClassName() + ": "
						+ currentMethodDeclaration.getName().getFullyQualifiedName());
			}
			
		}		
		return false;
	}

	public List<MethodLogInspectionResult> getResults() {
		return results;
	}

	public int getIssueCount() {
		return issueCount;
	}
	
	private LogInspectionResult isLevelCheckedLogStatement(IfStatement statement, String expectedDebugStatement, String levelCheckMethodName, String logLevel) {
		LogInspectionResult inspectionResult = null;
		// check if the statement is a if statement and it check for log
		// level
		if (LogStatementUtils.isLogLevelCheckStatement(statement)) {
			Statement thenStatement = statement.getThenStatement();
			Statement insideIfStatement = null;
			// see if it is a block statement
			if (thenStatement instanceof Block) {
				Block suspectedLogBlock = (Block) thenStatement;
				int blockSizeInsideLogWrapper = suspectedLogBlock.statements().size();
				if (blockSizeInsideLogWrapper == 1) {
					insideIfStatement = (Statement) suspectedLogBlock.statements().get(0);
				}else {
					lowConfidenceMethodEndLog++;
					EXIT_LOG_LOW_CONFIDENCE.info("at "
							+ getCompilationUnit().getLineNumber(statement.getStartPosition() - 1) + " in "
							+ getSourceClassName() + ":"
							+ currentMethodDeclaration.getName().getFullyQualifiedName());
					EXIT_LOG_LOW_CONFIDENCE.info(statement);
				}
			} else {
				insideIfStatement = (Statement) thenStatement;
				lowConfidenceMethodEndLog++;
				EXIT_LOG_LOW_CONFIDENCE.info("at "
						+ getCompilationUnit().getLineNumber(statement.getStartPosition() - 1) + " in "
						+ getSourceClassName() + ":"
						+ currentMethodDeclaration.getName().getFullyQualifiedName());
				EXIT_LOG_LOW_CONFIDENCE.info(statement);
			}
			// check the statement to see if it is a log
			// statement
			if (insideIfStatement != null && insideIfStatement instanceof ExpressionStatement) {
				
				LogInspectionResult r = isLogStatement((ExpressionStatement) insideIfStatement, expectedDebugStatement, levelCheckMethodName, logLevel, statement);
				if (r.isLogStatement()) {
					inspectionResult = r;
					inspectionResult.setLogLevelCheck(true);
				}
			}
			
		}else{
			lowConfidenceMethodEndLog++;
			EXIT_LOG_LOW_CONFIDENCE.info("at "
					+ getCompilationUnit().getLineNumber(statement.getStartPosition() - 1) + " in "
					+ getSourceClassName() + ":"
					+ currentMethodDeclaration.getName().getFullyQualifiedName());
			EXIT_LOG_LOW_CONFIDENCE.info(statement);
		}
		return inspectionResult;
	}

	private void rewriteLog(MethodInvocation es, String expectedDebugStatement, String levelCheckMethodName, String logLevel, LogInspectionResult inspectionResult, IfStatement wraperIfStatement) {
		if (doRewrite) {
			ListRewrite listRewrite = getAstRewrite().getListRewrite(es, MethodInvocation.ARGUMENTS_PROPERTY);
			StringLiteral logStaramentLiteral = (StringLiteral) getAstRewrite().createStringPlaceholder(
					"\"" + expectedDebugStatement + "\"", ASTNode.STRING_LITERAL);
			listRewrite.replace(
					(ASTNode) listRewrite.getOriginalList().get(listRewrite.getOriginalList().size() - 1),
					logStaramentLiteral, null);

			// check if log level needs correction
			if (wraperIfStatement != null) {
				if (!StringUtils.contains(levelCheckMethodName, "Info") && inspectionResult != null) {
					if (inspectionResult.getLogType() == LogType.DIRECT) {
						getAstRewrite().set(wraperIfStatement.getExpression(), MethodInvocation.NAME_PROPERTY,
								getAstRewrite().getAST().newSimpleName("isInfoEnabled"), null);
					} else if (inspectionResult.getLogType() == LogType.SERVICE) {
						getAstRewrite().set(wraperIfStatement.getExpression(), MethodInvocation.NAME_PROPERTY,
								getAstRewrite().getAST().newSimpleName("isInfoLevelEnable"), null);
					}
				} else {
					// TODO wrap
				}
				if (!logLevel.equals("INFO")) {
					// change logger call
					getAstRewrite().set(es, MethodInvocation.NAME_PROPERTY, getAstRewrite().getAST().newSimpleName("info"), null);
				}
			}

		}
	}
	
	private LogInspectionResult isLogStatement(ExpressionStatement s, String expectedLogStatement, String levelCheckMethodName, String logLevel, IfStatement statement) {
		LogInspectionResult inspectionResult = new LogInspectionResult();
		Expression e = s.getExpression();
		if (e instanceof MethodInvocation) {
			MethodInvocation mie = (MethodInvocation) e;
			// check if it is a log statement
			String logMethodName = mie.getName().getFullyQualifiedName();
			if (LogStatementUtils.isLoggerCall(mie)) {
				inspectionResult.setLogStatement(true);
				inspectionResult.setLogLevel(logMethodName);
				// check if it is logging service log or a direct logger call
				// check the logger statement for compliance
				LogType logType = LogStatementUtils.getLogType(mie);
				inspectionResult.setLogType(logType);
				// check if format is correct
				// this is typically used for method start end logs
				if (expectedLogStatement != null) {
					@SuppressWarnings("rawtypes")
					List arguments = mie.arguments();
					String debugStatement = null;
					int expectedArgCount = 1;
					// for service log call type log call should have max 2
					// arguments
					if (logType == LogType.SERVICE) {
						expectedArgCount = 2;
					}		

					int argSize = arguments.size();
					if (argSize == 1 && argSize == expectedArgCount && arguments.get(0) instanceof StringLiteral) {
						StringLiteral l = (StringLiteral) arguments.get(0);
						debugStatement = l.getLiteralValue();
						if(isPossibleMehtodEndLogLiteral(debugStatement.trim())){
							if(logLevel.equals("INFO") && "isInfoEnabled".equals(levelCheckMethodName)){
								if (expectedLogStatement.equals(debugStatement)) {
									inspectionResult.setFormatCompliant(true);
									compliantMethodEndLog++;
									EXIT_LOG_COMPLIANT.info("at " + getCompilationUnit().getLineNumber(s.getStartPosition() - 1)
											+ " in " + getSourceClassName() + ":"
											+ currentMethodDeclaration.getName().getFullyQualifiedName());
									EXIT_LOG_COMPLIANT.info(s);
								}else{
									inspectionResult.setFormatCompliant(false);
									highConfidenceMethodEndLog++;
									EXIT_LOG_HIGH_CONFIDENCE.info("at "
											+ getCompilationUnit().getLineNumber(s.getStartPosition() - 1) + " in "
											+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
									EXIT_LOG_HIGH_CONFIDENCE.info(s);
									rewriteLog(mie, expectedLogStatement, levelCheckMethodName, logLevel, inspectionResult, statement);
								}
							}else {
								inspectionResult.setFormatCompliant(false);
								highConfidenceMethodEndLog++;
								EXIT_LOG_HIGH_CONFIDENCE.info("at "
										+ getCompilationUnit().getLineNumber(s.getStartPosition() - 1) + " in "
										+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
								EXIT_LOG_HIGH_CONFIDENCE.info(s);
								rewriteLog(mie, expectedLogStatement, levelCheckMethodName, logLevel, inspectionResult, statement);
							}
						} else{
							inspectionResult.setFormatCompliant(false);
							lowConfidenceMethodEndLog++;
							EXIT_LOG_LOW_CONFIDENCE.info("at "
									+ getCompilationUnit().getLineNumber(s.getStartPosition() - 1) + " in "
									+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
							EXIT_LOG_LOW_CONFIDENCE.info(s);
						}											
					} else if (argSize == 2 && argSize == expectedArgCount && arguments.get(1) instanceof StringLiteral) {
						StringLiteral l = (StringLiteral) arguments.get(1);
						debugStatement = l.getLiteralValue();
						if(isPossibleMehtodEndLogLiteral(debugStatement.trim())){
							if(logLevel.equals("INFO") && "isInfoLevelEnable".equals(levelCheckMethodName)){
								if (expectedLogStatement.equals(debugStatement)) {
									inspectionResult.setFormatCompliant(true);
									compliantMethodEndLog++;
									EXIT_LOG_COMPLIANT.info("at " + getCompilationUnit().getLineNumber(s.getStartPosition() - 1)
											+ " in " + getSourceClassName() + ":"
											+ currentMethodDeclaration.getName().getFullyQualifiedName());
									EXIT_LOG_COMPLIANT.info(s);
								}else{
									inspectionResult.setFormatCompliant(false);
									highConfidenceMethodEndLog++;
									EXIT_LOG_HIGH_CONFIDENCE.info("at "
											+ getCompilationUnit().getLineNumber(s.getStartPosition() - 1) + " in "
											+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
									EXIT_LOG_HIGH_CONFIDENCE.info(s);
									rewriteLog(mie, expectedLogStatement, levelCheckMethodName, logLevel, inspectionResult, statement);
								}
							} else {
								inspectionResult.setFormatCompliant(false);
								highConfidenceMethodEndLog++;
								EXIT_LOG_HIGH_CONFIDENCE.info("at "
										+ getCompilationUnit().getLineNumber(s.getStartPosition() - 1) + " in "
										+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
								EXIT_LOG_HIGH_CONFIDENCE.info(s);
								rewriteLog(mie, expectedLogStatement, levelCheckMethodName, logLevel, inspectionResult, statement);
							}	
						} else{
							inspectionResult.setFormatCompliant(false);
							lowConfidenceMethodEndLog++;
							EXIT_LOG_LOW_CONFIDENCE.info("at "
									+ getCompilationUnit().getLineNumber(s.getStartPosition() - 1) + " in "
									+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
							EXIT_LOG_LOW_CONFIDENCE.info(s);
						}
											
					} else {
						inspectionResult.setFormatCompliant(false);
						lowConfidenceMethodEndLog++;
						EXIT_LOG_LOW_CONFIDENCE.info("at "
								+ getCompilationUnit().getLineNumber(s.getStartPosition() - 1) + " in "
								+ getSourceClassName() + ":" + currentMethodDeclaration.getName().getFullyQualifiedName());
						EXIT_LOG_LOW_CONFIDENCE.info(s);
					}
				}
			}
		}else{
			lowConfidenceMethodEndLog++;
			EXIT_LOG_LOW_CONFIDENCE.info("at "
					+ getCompilationUnit().getLineNumber(s.getStartPosition() - 1) + " in "
					+ getSourceClassName() + ":"
					+ currentMethodDeclaration.getName().getFullyQualifiedName());
			EXIT_LOG_LOW_CONFIDENCE.info(s);
		}
		return inspectionResult;
	}
	
	private boolean isPossibleMehtodEndLogLiteral(String s) {
		return StringUtils.containsIgnoreCase(s, "finish") || StringUtils.containsIgnoreCase(s, "return")
				|| StringUtils.containsIgnoreCase(s, "end") || StringUtils.containsIgnoreCase(s, "exit")  || StringUtils.containsIgnoreCase(s, "complete");
	}

}