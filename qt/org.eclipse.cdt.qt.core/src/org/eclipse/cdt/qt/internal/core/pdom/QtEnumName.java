/*
 * Copyright (c) 2013 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.cdt.qt.internal.core.pdom;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.core.runtime.CoreException;

public class QtEnumName extends AbstractQObjectFieldName implements IQtASTName  {

	private final IASTName cppEnumName;
	private final boolean isFlag;

	public QtEnumName(QObjectName qobjName, IASTName ast, String qtEnumName, IASTName cppEnumName, QtASTImageLocation location, boolean isFlag) {
		super(qobjName, ast, qtEnumName, location);
		this.cppEnumName = cppEnumName;
		this.isFlag = isFlag;
	}

	public boolean isFlag() {
		return isFlag;
	}

	@Override
	public QtPDOMBinding createPDOMBinding(QtPDOMLinkage linkage) throws CoreException {
		return new QtPDOMQEnum(linkage, getOwner(linkage), this, cppEnumName);
	}
}