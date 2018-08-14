/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.eclipse.cdt.internal.ui.refactoring.rename.resources;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditorExtension2;

/**
 * @author fabioz
 *
 */
public class Helpers {

	public static IEditorPart[] checkValidateState() {
		IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IEditorPart[] dirtyEditors = workbenchWindow.getActivePage().getDirtyEditors();
		for (IEditorPart iEditorPart : dirtyEditors) {
			checkValidateState(iEditorPart);
		}
		return dirtyEditors;
	}

	public static void checkValidateState(IEditorPart iEditorPart) {
		if (iEditorPart instanceof ITextEditorExtension2) {
			ITextEditorExtension2 editor = (ITextEditorExtension2) iEditorPart;
			editor.validateEditorInputState();
		}
	}
}
