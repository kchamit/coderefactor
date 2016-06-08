package com.ibm.cleancode.framework.utility;

import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

public class ExclusionJavaSourceWalker extends JavaSourceWalker {

	public ExclusionJavaSourceWalker(String... exclusions) {
		super(FileFilterUtils.directoryFileFilter(), new AndFileFilter(FileFilterUtils.suffixFileFilter(".java"),
				new NotFileFilter(new WildcardFileFilter(exclusions))), -1);
	}

}
