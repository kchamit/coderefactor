package com.ibm.cleancode.framework.utility;

import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

public class InclusionJavaSourceWalker extends JavaSourceWalker {

	public InclusionJavaSourceWalker(String... inclusion) {
		super(FileFilterUtils.directoryFileFilter(), new AndFileFilter(FileFilterUtils.suffixFileFilter(".java"),
				new WildcardFileFilter(inclusion)), -1);
	}

}
