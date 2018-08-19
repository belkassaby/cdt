package org.eclipse.cdt.internal.ui.navigator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.actions.RenameResourceAction;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

import org.eclipse.cdt.core.CProjectNature;

import org.eclipse.cdt.internal.ui.refactoring.rename.CRefactory;

public class CNavigatorRenameResourceAction extends RenameResourceAction {

	private static final Logger logger = Logger.getLogger(CNavigatorRenameResourceAction.class.getName());

	private ISelectionProvider selectionProvider;
	private List<IResource> selected;
	private IShellProvider shell;

	public CNavigatorRenameResourceAction(IShellProvider shell, ISelectionProvider selectionProvider, Tree tree) {
		super(shell, tree);
		this.shell = shell;
		this.selectionProvider = selectionProvider;
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

		InputDialog dialog = new InputDialog(shell.getShell(),
				IDEWorkbenchMessages.RenameResourceAction_inputDialogTitle,
				IDEWorkbenchMessages.RenameResourceAction_inputDialogMessage, resource.getName(), validator);
		dialog.setBlockOnOpen(true);
		if (dialog.open() == Window.OK) {
			return dialog.getValue();
		} else {
			return null;
		}
	}

	@Override
	public boolean isEnabled() {
		selected = new ArrayList<IResource>();

		ISelection selection = selectionProvider.getSelection();
		if (!selection.isEmpty()) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			if (sSelection.size() >= 1) {
				Iterator<?> iterator = sSelection.iterator();
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
		if (selected == null && !isEnabled()) { //will also update the list of resources
		}
		return selected;
	}

	@Override
	public IStructuredSelection getStructuredSelection() {
		return new StructuredSelection(selected);
	}

	@Override
	public void run() {
		if (!isEnabled()) { // will also update the list of resources
			return;
		}
		// IEditorPart[] dirtyEditors = Helpers.checkValidateState();
		List<IResource> resources = getSelectedResources();

		if (resources.size() != 1) {
			Shell shell = Display.getDefault().getActiveShell();
			MessageDialog.openWarning(shell, "Can only rename one element.", //$NON-NLS-1$
					"One element must be selected for rename."); //$NON-NLS-1$
			return;
		}

		IResource r = resources.get(0);

		IProject project = r.getProject();
		CProjectNature n = null;
		try {
			n = CProjectNature.getCNature(project, null);
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		if (n != null) {
			if (r instanceof IFile) {
				System.out.println("refactoring"); //$NON-NLS-1$
				try {
					IFile file = null;
					if (r instanceof IFile) {
						file = (IFile) r;
					}
					if (file != null && file.exists()) {
						CRefactory.getInstance().renameResource(shell.getShell(), selected.get(0));
						return;
					}
				} catch (Exception e) {
					logger.log(new LogRecord(Level.SEVERE, e.getMessage()));
					e.printStackTrace();
				}
			}
		}
		super.run();
	}

}
