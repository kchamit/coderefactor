package com.ibm.cleancode.unf.processor;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.ibm.cleancode.framework.processor.SourceCodeProcessor;
import com.ibm.cleancode.framework.utility.ExclusionJavaSourceWalker;
import com.ibm.cleancode.unf.visitors.CatchClauseReportingVisitor;

public class CatchClauseReportingInspector {
	private static final Logger LOGGER = Logger.getLogger(CatchClauseReportingFactoryImplInspector.class);

	public static void main(String[] args) throws Exception {
		for (String project : RSAProjectHelper.getAllButBillingProjects()) {
			ExclusionJavaSourceWalker sourceWalker = new ExclusionJavaSourceWalker("*FactoryImpl.java",
					"*FactoryDelegate.java", "*Mediator.java", "*ManagedBean.java", "*Helper.java", "*Adapter.java");
			List<File> sourceFiles = sourceWalker.getFiles("C:/Dev/UNF-Rsa-SIT3/" + project);
			CatchClauseReportingVisitor visitor = new CatchClauseReportingVisitor();
			SourceCodeProcessor processor = new SourceCodeProcessor(visitor);
			for (File sourceFile : sourceFiles) {
				String sourceCode = FileUtils.readFileToString(sourceFile, "cp1252");
				processor.start(sourceCode);
			}
			LOGGER.info(project + "/" + visitor.getLowConfidenceCatchClauseCount() + "/"
					+ visitor.getHighConfidenceCatchClauseCount() + "/" + 0);

		}
	}
}
