package com.ibm.cleancode.framework.utility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

public class JavaSourceWalker extends DirectoryWalker<File> {

	public JavaSourceWalker() {
		super(FileFilterUtils.directoryFileFilter(), FileFilterUtils.suffixFileFilter(".java"), -1);
	}

	public JavaSourceWalker(IOFileFilter directoryFilter, IOFileFilter fileFilter, int depthLimit) {
		super(directoryFilter, fileFilter, depthLimit);
	}

	@Override
	protected void handleFile(File file, int depth, Collection<File> results) throws IOException {
		results.add(file);
	}

	@Override
	protected boolean handleDirectory(File directory, int depth, Collection<File> results) throws IOException {
		return true;
	}

	public List<File> getFiles(String baseDir) throws IOException {
		List<File> results = new ArrayList<File>();
		walk(new File(baseDir), results);
		return results;
	}
}
