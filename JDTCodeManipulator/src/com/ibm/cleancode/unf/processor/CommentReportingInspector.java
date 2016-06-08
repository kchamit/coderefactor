package com.ibm.cleancode.unf.processor;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.ibm.cleancode.framework.processor.SourceCodeCommentProcessor;
import com.ibm.cleancode.framework.utility.InclusionJavaSourceWalker;

public class CommentReportingInspector {
	private static final Logger LOGGER = Logger.getLogger(CatchClauseReportingFactoryImplInspector.class);

	public static void main(String[] args) throws Exception {
		for (String project : RSAProjectHelper.getAllProjects()) {
			InclusionJavaSourceWalker sourceWalker = new InclusionJavaSourceWalker("SiteServiceMediator.java");
			List<File> sourceFiles = sourceWalker.getFiles("C:/Dev/UNF-Rsa-SIT3/" + project);
			SourceCodeCommentProcessor processor = new SourceCodeCommentProcessor();
			for (File sourceFile : sourceFiles) {
				String sourceCode = FileUtils.readFileToString(sourceFile, "cp1252");
				processor.start(sourceCode);
			}

		}
	}
}
