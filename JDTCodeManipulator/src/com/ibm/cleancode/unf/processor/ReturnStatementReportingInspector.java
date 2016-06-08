package com.ibm.cleancode.unf.processor;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.ibm.cleancode.framework.processor.SourceCodeProcessor;
import com.ibm.cleancode.framework.utility.JavaSourceWalker;
import com.ibm.cleancode.unf.visitors.MethodEntryLogReportingVisitor;
import com.ibm.cleancode.unf.visitors.MethodLogInspectionResult;
import com.ibm.cleancode.unf.visitors.ReturnStatementReportingVisitor;

public class ReturnStatementReportingInspector {

	public static void main(String[] args) throws Exception {
		for (String project : RSAProjectHelper.getAllProjects()) {
			ReturnStatementReportingVisitor visitor = new ReturnStatementReportingVisitor();
			SourceCodeProcessor processor = new SourceCodeProcessor(visitor);
			JavaSourceWalker sourceWalker = new JavaSourceWalker();
			List<File> sourceFiles = sourceWalker.getFiles("C:/Dev/UNF-Rsa-SIT3/" + project);
			for (File sourceFile : sourceFiles) {
				String sourceCode = FileUtils.readFileToString(sourceFile, "cp1252");
				processor.start(sourceCode);
			}
		}
	}
	// 632 empty methods in last analysis
}