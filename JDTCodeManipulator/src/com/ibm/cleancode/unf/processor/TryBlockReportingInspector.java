package com.ibm.cleancode.unf.processor;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.ibm.cleancode.framework.processor.SourceCodeProcessor;
import com.ibm.cleancode.framework.utility.InclusionJavaSourceWalker;
import com.ibm.cleancode.unf.visitors.TryBlockReportingVisitor;

public class TryBlockReportingInspector {
	private static final Logger LOGGER = Logger.getLogger(CatchClauseReportingFactoryImplInspector.class);

	public static void main(String[] args) throws Exception {
		for (String project : RSAProjectHelper.getAllButBillingProjects()) {
			InclusionJavaSourceWalker sourceWalker = new InclusionJavaSourceWalker("*Mediator.java");
			List<File> sourceFiles = sourceWalker.getFiles("C:/Dev/UNF-Rsa-SIT3/" + project);
			TryBlockReportingVisitor visitor = new TryBlockReportingVisitor();
			SourceCodeProcessor processor = new SourceCodeProcessor(visitor);
			for (File sourceFile : sourceFiles) {
				String sourceCode = FileUtils.readFileToString(sourceFile, "cp1252");
				processor.start(sourceCode);
			}
			if (visitor.getLowConfidenceCatchClauseCount() != 0 || visitor.getHighConfidenceCatchClauseCount() != 0) {
				LOGGER.info(project + "/" + visitor.getLowConfidenceCatchClauseCount() + "/"
						+ visitor.getHighConfidenceCatchClauseCount() + "/" + 0);
			}
		}
	}
}
