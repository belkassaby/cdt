/*******************************************************************************
 * Copyright (c) 2013, 2013 Andrew Gvozdev and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrew Gvozdev - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.errorparsers.xlc.tests;

import junit.framework.TestCase;

import org.eclipse.cdt.core.IMarkerGenerator;

public class TestCommandOptionNotRecognized extends TestCase {
	String err_msg;
	/**
	 * This function tests parseLine function of the
	 * XlcErrorParser class. A warning message generated by
	 * xlc compiler about command options is given as
	 * input for testing.
	 */
	public void testparseLine()
	{
		XlcErrorParserTester aix = new XlcErrorParserTester();
		aix.parseLine(err_msg);
		assertEquals("", aix.getFileName(0));
		assertEquals(0, aix.getLineNumber(0));
		assertEquals(IMarkerGenerator.SEVERITY_WARNING, aix.getSeverity(0));
		assertEquals("command option 9 is not recognized - passed to ld",aix.getMessage(0));
	}
	public TestCommandOptionNotRecognized(String name)
	{
		super(name);
		err_msg = "/usr/vacpp/bin/xlc: 1501-216 command option 9 is not recognized - passed to ld";
	}
}
