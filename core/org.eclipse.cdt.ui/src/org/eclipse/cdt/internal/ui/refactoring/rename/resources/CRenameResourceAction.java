/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.eclipse.cdt.internal.ui.refactoring.rename.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.RenameResourceAction;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

import org.eclipse.cdt.core.CProjectNature;

public class CRenameResourceAction extends RenameResourceAction {

	private static final Logger logger = Logger.getLogger(CRenameResourceAction.class.getName());
	private ISelectionProvider provider;

	private List<IResource> selected;
	private IFolder renamedFolder;
	private List<IResource> preResources;

	private Shell shell;

	public CRenameResourceAction(Shell shell, ISelectionProvider selectionProvider) {
		super(shell);
		this.shell = shell;
		this.provider = selectionProvider;
	}

	/**
	 * Return the new name to be given to the target resource.
	 *
	 * @return java.lang.String
	 * @param resource
	 *            the resource to query status on
	 *
	 *            Fix from platform: was not checking return from dialog.open
	 */
	@Override
	protected String queryNewResourceName(final IResource resource) {
		final IWorkspace workspace = IDEWorkbenchPlugin.getPluginWorkspace();
		final IPath prefix = resource.getFullPath().removeLastSegments(1);
		IInputValidator validator = new IInputValidator() {
			@Override
			public String isValid(String string) {
				if (resource.getName().equals(string)) {
					return IDEWorkbenchMessages.RenameResourceAction_nameMustBeDifferent;
				}
				IStatus status = workspace.validateName(string, resource.getType());
				if (!status.isOK()) {
					return status.getMessage();
				}
				if (workspace.getRoot().exists(prefix.append(string))) {
					return IDEWorkbenchMessages.RenameResourceAction_nameExists;
				}
				return null;
			}
		};

		InputDialog dialog = new InputDialog(shell,
				IDEWorkbenchMessages.RenameResourceAction_inputDialogTitle,
				IDEWorkbenchMessages.RenameResourceAction_inputDialogMessage, resource.getName(), validator);
		dialog.setBlockOnOpen(true);
		if (dialog.open() == Window.OK) {
			return dialog.getValue();
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.action.Action#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		selected = new ArrayList<IResource>();

		ISelection selection = provider.getSelection();
		if (!selection.isEmpty()) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			if (sSelection.size() >= 1) {
				Iterator iterator = sSelection.iterator();
				while (iterator.hasNext()) {
					Object element = iterator.next();
					if (element instanceof IAdaptable) {
						IAdaptable adaptable = (IAdaptable) element;
						IResource resource = adaptable.getAdapter(IResource.class);
						if (resource != null && resource.isAccessible()) {
							selected.add(resource);
							continue;
						}
					}
					// one of the elements did not satisfy the condition
					selected = null;
					return false;
				}
			}
		}
		return true;
	}

	@Override
	protected List<IResource> getSelectedResources() {
		return selected;
	}

	@Override
	public IStructuredSelection getStructuredSelection() {
		return new StructuredSelection(selected);
	}

	/*
	 * (non-Javadoc) Method declared on IAction.
	 */
	@Override
	public void run() {
		if (!isEnabled()) { // will also update the list of resources (main
							// change from the DeleteResourceAction)
			return;
		}
		IEditorPart[] dirtyEditors = Helpers.checkValidateState();
		List<IResource> resources = getSelectedResources();

		if (resources.size() != 1) {
			Shell shell = Display.getDefault().getActiveShell();
			MessageDialog.openWarning(shell, "Can only rename one element.", //$NON-NLS-1$
					"One element must be selected for rename."); //$NON-NLS-1$
			return;
		}

		IResource r = resources.get(0);
		if (r instanceof IFile) {
			for (IEditorPart iEditorPart : dirtyEditors) {
				IEditorInput editorInput = iEditorPart.getEditorInput();
				Object input = editorInput.getAdapter(IResource.class);
				if (r.equals(input)) {
					iEditorPart.doSave(null);
				}
			}
		} else if (r instanceof IFolder) {
			try {
				renamedFolder = (IFolder) r;
				preResources = new ArrayList<IResource>();
				IResource[] members = renamedFolder.getParent().members();
				for (IResource m : members) {
					preResources.add(m);
				}
			} catch (CoreException e) {
				logger.log(new LogRecord(Level.SEVERE,
						"Unexpected error reading parent properties:" + e.getMessage())); //$NON-NLS-1$
				renamedFolder = null;
				preResources = null;
			}
		} else {
			renamedFolder = null;
			preResources = null;
		}

		IProject project = r.getProject();
		CProjectNature n = null;
		try {
			n = CProjectNature.getCNature(project, null);
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (n != null) {
			if (r instanceof IFile) {
				// If it is a file which does not end with .py, don't try to do
				// a regular refactoring.
			} else {
				try {
					IFile file = null;
					boolean foundAsInit = false;
//					if (r instanceof IContainer) {
//						file = getFolderInit((IContainer) r);
//						foundAsInit = true;
//					} else 
					if (r instanceof IFile) {
						file = (IFile) r;
					}

					if (file != null && file.exists()) {
						// It's a directory without an __init__.py file,
						// just keep going...
//						RefactoringRequest request = new ModuleRenameRefactoringRequest(
//								file.getLocation().toFile(), n, null);
//						if (!foundAsInit) {
//							// If we have found it as an __init__ when
//							// renaming a module, we won't
//							// set the related IFile (because we don't want
//							// to provide a 'simple rename'
//							// in this case -- as if he did actually select
//							// the __init__, only the simple
//							// rename would be provided in the first place).
//							request.setFileResource(file);
//						}
						CRenameRefactoring.rename();
						// i.e.: if it was a module inside the pythonpath
						// (as we resolved the name), don't go the default
						// route and do a refactoring request to rename it)!
						return;
					}

				} catch (Exception e) {
					logger.log(new LogRecord(Level.SEVERE, e.getMessage()));
				}
			}
		}

		super.run();
		renamedFolder = null;
		preResources = null;
	}

//	/**
//	 * @param root
//	 *            this is the folder we're checking
//	 * @return true if it is a folder with an __init__ python file
//	 */
//	public static IFile getFolderInit(IContainer root) {
//		// Checking for existence of a specific file is much faster than listing
//		// a directory!
//		String[] validInitFiles = FileTypesPreferences.getValidInitFiles();
//		int len = validInitFiles.length;
//		for (int i = 0; i < len; i++) {
//			String init = validInitFiles[i];
//			IFile f = root.getFile(new Path(init));
//			if (f.exists()) {
//				return f;
//			}
//		}
//
//		return null;
//	}
//
//	/**
//	 * @return whether an IFile is a valid source file given its extension
//	 */
//	public static boolean isValidSourceFile(IFile file) {
//		String ext = file.getFileExtension();
//		if (ext == null) { // no extension
//			return false;
//		}
//		ext = ext.toLowerCase();
//		String[] validSourceFiles = FileTypesPreferences.getValidSourceFiles();
//		int len = validSourceFiles.length;
//		for (int i = 0; i < len; i++) {
//			String end = validSourceFiles[i];
//			if (ext.equals(end)) {
//				return true;
//			}
//		}
//		if (ext.equals(".pypredef")) {
//			return true;
//		}
//		return false;
//	}

}
