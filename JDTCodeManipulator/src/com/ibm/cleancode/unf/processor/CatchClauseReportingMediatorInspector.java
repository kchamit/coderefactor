package com.ibm.cleancode.unf.processor;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.ibm.cleancode.framework.processor.SourceCodeProcessor;
import com.ibm.cleancode.framework.utility.InclusionJavaSourceWalker;
import com.ibm.cleancode.unf.visitors.CatchClauseReportingMediatorVisitor;

public class CatchClauseReportingMediatorInspector {
	private static final Logger LOGGER = Logger.getLogger(CatchClauseReportingMediatorInspector.class);

	public static void main(String[] args) throws Exception {
		for (String project : RSAProjectHelper.getAllProjects()) {
			InclusionJavaSourceWalker sourceWalker = new InclusionJavaSourceWalker("*Helper.java");
			List<File> sourceFiles = sourceWalker.getFiles("C:/Dev/UNF-Rsa-SIT3/" + project);
			CatchClauseReportingMediatorVisitor visitor = new CatchClauseReportingMediatorVisitor();
			SourceCodeProcessor processor = new SourceCodeProcessor(visitor);
			for (File sourceFile : sourceFiles) {
				String sourceCode = FileUtils.readFileToString(sourceFile, "cp1252");
				processor.start(sourceCode);
			}
			LOGGER.info(project + "/" + visitor.getLowConfidenceCatchClauseCount() + "/"
					+ visitor.getHighConfidenceCatchClauseCount() + "/" + visitor.getCompliantCatchClauseCount() + "/"
					+ visitor.getEmptyCatchClauseCount());

		}
	}
}
