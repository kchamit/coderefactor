package com.ibm.cleancode.unf.visitors;

public class LogInspectionResult {
	private boolean isLogStatement;
	private boolean isLogLevelCheck;
	private boolean isFormatCompliant;
	private LogType logType;
	private String logLevel;

	public boolean isLogStatement() {
		return isLogStatement;
	}

	public void setLogStatement(boolean isLogStatement) {
		this.isLogStatement = isLogStatement;
	}

	public boolean isLogLevelCheck() {
		return isLogLevelCheck;
	}

	public void setLogLevelCheck(boolean isLogLevelCheck) {
		this.isLogLevelCheck = isLogLevelCheck;
	}

	public boolean isFormatCompliant() {
		return isFormatCompliant;
	}

	public void setFormatCompliant(boolean isFormatCompliant) {
		this.isFormatCompliant = isFormatCompliant;
	}

	public LogType getLogType() {
		return logType;
	}

	public void setLogType(LogType logType) {
		this.logType = logType;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(isLogStatement ? "Y" : "N").append(',').append(isLogLevelCheck ? "Y" : "N").append(',')
				.append(isFormatCompliant ? "Y" : "N").append(',').append(logType != null ? logType : "").append(',')
				.append(logLevel != null ? logLevel : "");
		return builder.toString();
	}

	public static enum LogType {
		SERVICE, DIRECT, SERVICE_SUSPECT, DIRECT_SUSPECT, UNKNOWN;
	}

}
