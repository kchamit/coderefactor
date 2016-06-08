package com.ibm.cleancode.unf.processor;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.ibm.cleancode.framework.processor.SourceCodeProcessor;
import com.ibm.cleancode.framework.utility.JavaSourceWalker;
import com.ibm.cleancode.unf.visitors.MethodLogInspectionResult;
import com.ibm.cleancode.unf.visitors.ReturnExitLogReportingVisitor;

public class ReturnExitLogReportingInspector {

	public static void main(String[] args) throws Exception {
		PrintWriter resultWriter = new PrintWriter(new File("ReturnExitLogReportingInspector.csv"));
		resultWriter
				.println("Project, Package, Class Name, Method Name, Log Statement, Log Level Check, Format Compliant, Log Type, Log Level");

		for (String project : RSAProjectHelper.getCurrentRun()) {
			ReturnExitLogReportingVisitor visitor = new ReturnExitLogReportingVisitor();
			SourceCodeProcessor processor = new SourceCodeProcessor(visitor);
			JavaSourceWalker sourceWalker = new JavaSourceWalker();
			List<File> sourceFiles = sourceWalker.getFiles("C:/Molay/ProjectWS/RSA/DEV_SIT3/" + project);
			for (File sourceFile : sourceFiles) {
				String sourceCode = FileUtils.readFileToString(sourceFile, "cp1252");
				processor.start(sourceCode);
			}
			List<MethodLogInspectionResult> results = visitor.getResults();
			for (MethodLogInspectionResult result : results) {
				resultWriter.print(project + ",");
				resultWriter.println(result);
			}
		}
		resultWriter.flush();
		resultWriter.close();
	}
	// 632 empty methods in last analysis
}