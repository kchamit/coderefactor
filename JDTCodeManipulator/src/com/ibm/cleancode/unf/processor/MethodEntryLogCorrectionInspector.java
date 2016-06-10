package com.ibm.cleancode.unf.processor;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.ibm.cleancode.framework.processor.SourceCodeProcessor;
import com.ibm.cleancode.framework.utility.InclusionJavaSourceWalker;
import com.ibm.cleancode.unf.visitors.MethodEntryLogCorrectionVisitor;

public class MethodEntryLogCorrectionInspector {
	private static final Logger LOGGER = Logger.getLogger(MethodEntryLogCorrectionInspector.class);

	public static void main(String[] args) throws Exception {
		for (String project : RSAProjectHelper.getWave1Projects()) {
			Boolean doRewrite = true;
			LOGGER.info("Processing :" + project);
			InclusionJavaSourceWalker sourceWalker = new InclusionJavaSourceWalker("*ManagedBean.java",
					"*Mediator.java", "*Helper.java", "*FactoryImpl.java", "*Mapper.java");
			List<File> sourceFiles = sourceWalker.getFiles("C:/Dev/UNF-Rsa-SIT3/" + project);
			MethodEntryLogCorrectionVisitor visitor = new MethodEntryLogCorrectionVisitor(doRewrite);
			SourceCodeProcessor processor = new SourceCodeProcessor(visitor);
			for (File sourceFile : sourceFiles) {
				String sourceCode = FileUtils.readFileToString(sourceFile, "cp1252");
				String modifiedSource = processor.start(sourceCode);
				if (doRewrite) {
					FileUtils.write(sourceFile, modifiedSource, "cp1252");
				}
			}
			LOGGER.info("Finished processing Low/High/Compliant/Added Confidece Method Start: "
					+ visitor.getLowConfidenceMethodStartLog() + "/" + visitor.getHighConfidenceMethodStartLog() + "/"
					+ visitor.getCompliantMethodStartLog() + "/" + visitor.getAddedCount() + " issues in " + project);

		}
		// MethodEntryLogCorrectionVisitor visitor = new
		// MethodEntryLogCorrectionVisitor(true);
		// SourceCodeProcessor processor = new SourceCodeProcessor(visitor);
		// File file = new File(
		// "C:/Dev/UNF-Rsa-SIT3/RsaDynamicWeb/src/com/unifirst/rsa/account/webui/managedbean/UpdateAccountSiteOrderManagedBean.java");
		// String sourceCode = FileUtils.readFileToString(file, "cp1252");
		// FileUtils.write(file, processor.start(sourceCode), "cp1252");
		//
		// System.out.println(visitor.getAddedCount());
	}
}
