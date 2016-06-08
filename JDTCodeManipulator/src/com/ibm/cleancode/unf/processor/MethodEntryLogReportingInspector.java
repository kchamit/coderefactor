package com.ibm.cleancode.unf.processor;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.ibm.cleancode.framework.processor.SourceCodeProcessor;
import com.ibm.cleancode.framework.utility.JavaSourceWalker;
import com.ibm.cleancode.unf.visitors.MethodEntryLogReportingVisitor;
import com.ibm.cleancode.unf.visitors.MethodLogInspectionResult;

public class MethodEntryLogReportingInspector {

	public static void main(String[] args) throws Exception {
		PrintWriter resultWriter = new PrintWriter(new File("MethodEntryLogReportingInspector.csv"));
		resultWriter
				.println("Project, Package, Class Name, Method Name, Log Statement, Log Level Check, Format Compliant, Log Type, Log Level");

		for (String project : RSAProjectHelper.getAllProjects()) {
			MethodEntryLogReportingVisitor visitor = new MethodEntryLogReportingVisitor();
			SourceCodeProcessor processor = new SourceCodeProcessor(visitor);
			JavaSourceWalker sourceWalker = new JavaSourceWalker();
			List<File> sourceFiles = sourceWalker.getFiles("C:/Dev/UNF-Rsa-SIT3/" + project);
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