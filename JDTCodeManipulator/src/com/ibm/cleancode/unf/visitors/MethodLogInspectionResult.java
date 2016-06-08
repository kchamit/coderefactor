package com.ibm.cleancode.unf.visitors;

public class MethodLogInspectionResult {
	private String packageName;
	private String className;
	private String methodName;
	private LogInspectionResult logInspectionResult;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public LogInspectionResult getLogInspectionResult() {
		return logInspectionResult;
	}

	public void setLogInspectionResult(LogInspectionResult logInspectionResult) {
		this.logInspectionResult = logInspectionResult;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(packageName).append(",").append(className).append(",").append(methodName).append(",")
				.append(logInspectionResult != null ? logInspectionResult.toString() : null);
		return builder.toString();
	}
}
