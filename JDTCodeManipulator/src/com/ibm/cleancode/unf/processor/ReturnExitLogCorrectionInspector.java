package com.ibm.cleancode.unf.processor;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.ibm.cleancode.framework.processor.SourceCodeProcessor;
import com.ibm.cleancode.framework.utility.JavaSourceWalker;
import com.ibm.cleancode.unf.visitors.ReturnExitLogCorrectionVisitor;

public class ReturnExitLogCorrectionInspector {
	private static final Logger LOGGER = Logger.getLogger(ReturnExitLogCorrectionInspector.class);

	public static void main(String[] args) throws Exception {
		boolean runSample = true;
		for (String project : RSAProjectHelper.getCurrentRun()) {
			LOGGER.info("Processing : " + project);
			JavaSourceWalker sourceWalker = new JavaSourceWalker();
			List<File> sourceFiles = sourceWalker.getFiles("C:/Molay/ProjectWS/RSA/DEV_SIT3/" + project);
			
			ReturnExitLogCorrectionVisitor visitor = new ReturnExitLogCorrectionVisitor(true);
			SourceCodeProcessor processor = new SourceCodeProcessor(visitor);
			for (File sourceFile : sourceFiles) {
				String sourceCode = FileUtils.readFileToString(sourceFile, "cp1252");
				String modifiedSource = processor.start(sourceCode);
				FileUtils.write(sourceFile, modifiedSource, "cp1252");
			}
			LOGGER.info("Finished processing: " + visitor.getIssueCount() + " issues in " + project);
			LOGGER.info("Finished processing Low/High/Compliant Confidece Method End: "
					+ visitor.getLowConfidenceMethodEndLog() + "/" + visitor.getHighConfidenceMethodEndLog() + "/"
					+ visitor.getCompliantMethodEndLog() + " issues in " + project);

		}
	}
}
