/**********************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.cdt.core.build.managed.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.cdt.core.build.managed.ITarget;
import org.eclipse.cdt.core.build.managed.ITool;
import org.eclipse.cdt.core.build.managed.ManagedBuildManager;

/**
 * 
 */
public class AllBuildTests extends TestCase {

	public AllBuildTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite();
		
		suite.addTest(new AllBuildTests("testExtensions"));
		
		return suite;
	}

	public void testThatAlwaysFails() {
		assertTrue(false);
	}
	
	/**
	 * Navigates through the build info as defined in the extensions
	 * defined in this plugin
	 */
	public void testExtensions() {
		// Note secret null parameter which means just extensions
		ITarget[] targets = ManagedBuildManager.getAvailableTargets(null);
		
		ITarget target = targets[0];
		assertEquals(target.getName(), "Linux");
		ITool[] tools = target.getTools();
		ITool tool = tools[0];
		assertEquals(tool.getName(), "Compiler");
	}
}
