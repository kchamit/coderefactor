package com.ibm.cleancode.unf.processor;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.ibm.cleancode.framework.processor.SourceCodeProcessor;
import com.ibm.cleancode.framework.utility.ExclusionJavaSourceWalker;
import com.ibm.cleancode.framework.utility.InclusionJavaSourceWalker;
import com.ibm.cleancode.unf.visitors.CatchClauseExacptionEatingCorrectionAllVisitor;

public class CatchClauseExacptionEatingCorrectionAllInspector {
	private static final Logger LOGGER = Logger.getLogger(CatchClauseReportingFactoryImplInspector.class);

	public static void main(String[] args) throws Exception {
		for (String project : RSAProjectHelper.getBillingProjects()) {
			InclusionJavaSourceWalker sourceWalker = new InclusionJavaSourceWalker("*.java");
			List<File> sourceFiles = sourceWalker.getFiles("C:/Dev/UNF-Rsa-SIT3/" + project);
			CatchClauseExacptionEatingCorrectionAllVisitor visitor = new CatchClauseExacptionEatingCorrectionAllVisitor(
					false);
			SourceCodeProcessor processor = new SourceCodeProcessor(visitor);
			for (File sourceFile : sourceFiles) {
				String sourceCode = FileUtils.readFileToString(sourceFile, "cp1252");
				// FileUtils.write(sourceFile, processor.start(sourceCode),
				// "cp1252");
				processor.start(sourceCode);
			}
			LOGGER.info(project + "-" + visitor.getExceptionEaterCatchClause());
		}
	}
}
