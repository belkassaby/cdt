/*******************************************************************************
 * Copyright (c) 2004, 2014 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 * 
 * Contributors: 
 *     Markus Schorn - initial API and implementation 
 *     Emanuel Graf (Institute for Software, HSR Hochschule fuer Technik)
 *     Sergey Prigogin (Google)
 ******************************************************************************/
package org.eclipse.cdt.internal.ui.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.cdt.ui.CUIPlugin;

import org.eclipse.cdt.internal.ui.preferences.CodeStylePreferencePage;
import org.eclipse.cdt.internal.ui.refactoring.rename.CRenameRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.rename.RenameMessages;

/**
 * Input page added to the standard refactoring wizard.
 */
public class CNavigatorResourceRenameRefactoringInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME = "RenameRefactoringPage"; //$NON-NLS-1$

	private static final String UPDATE_REFERENCES = "UPDATE_REFERENCES"; //$NON-NLS-1$
	private static final String SIMPLE_RESOURCE_RENAME = "SIMPLE_RESOURCE_RENAME"; //$NON-NLS-1$

	private Text textField;
	private IFile targetFile;

	private String initialSetting;

	public CNavigatorResourceRenameRefactoringInputPage(String message, boolean isLastUserPage, String initialSetting) {
		super(PAGE_NAME);
		Assert.isNotNull(initialSetting);
		this.initialSetting = initialSetting;
	}

	private RefactoringStatus validateTextField(String text) {
		RefactoringStatus status = new RefactoringStatus();
		CRenameRefactoring refactoring = (CRenameRefactoring) getRefactoring();
		if (isValidIdentifier(text, true))
			refactoring.getProcessor().setReplacementText(text);
		else
			status.addFatalError("The name: " + text + " is not a valid identifier."); //$NON-NLS-1$ //$NON-NLS-2$
		return status;
	}

	private Text createTextInputField(Composite parent, int style) {
		Text ret = new Text(parent, style);
		ret.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				textModified(ret.getText());
			}
		});
		ret.setText(initialSetting);
		this.textField = ret;
		setTextToFullName();
		return ret;
	}

	private void setTextToResourceName() {
		this.targetFile = getResourceFile();
		if (targetFile != null && textField != null) {
			String curr = targetFile.getName();
			textField.setText(curr);
			int i = curr.lastIndexOf('.');

			if (i >= 0) {
				textField.setSelection(0, i);
			} else {
				textField.selectAll();
			}
		}
	}

	private void setTextToFullName() {
		if (textField != null) {
			textField.setText(initialSetting);

			String text = initialSetting;
			int i = text.lastIndexOf('.');
			if (i >= 0) {
				textField.setSelection(i + 1, text.length());
			} else {
				textField.selectAll();
			}
		}
	}

	private void textModified(String text) {
		this.targetFile = getResourceFile();
		if (targetFile != null) {
			if (text.equals("")) { //$NON-NLS-1$
				setPageComplete(false);
				setErrorMessage(null);
				setMessage(null);
				return;
			}
			if (text.equals(targetFile.getName())) {
				setPageComplete(false);
				setErrorMessage(null);
				setMessage(null);
				return;
			}

			setPageComplete(validateTextField(text));
		}
		if (text.equals("")) { //$NON-NLS-1$
			setPageComplete(false);
			setErrorMessage(null);
			setMessage(null);
			return;
		}
		if (text.equals(initialSetting)) {
			setPageComplete(false);
			setErrorMessage(null);
			setMessage(null);
			return;
		}

		setPageComplete(validateTextField(text));
	}

	@Override
	public void createControl(Composite parent) {
		Composite superComposite = new Composite(parent, SWT.NONE);
		setControl(superComposite);
		initializeDialogUnits(superComposite);

		superComposite.setLayout(new GridLayout());
		Composite composite = new Composite(superComposite, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridLayout layout = new GridLayout();
		layout.numColumns = 4;
		layout.verticalSpacing = 8;
		composite.setLayout(layout);

		Label label = new Label(composite, SWT.NONE);
		label.setText("New &value:"); //$NON-NLS-1$

		final Text text = createTextInputField(composite, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = convertWidthInCharsToPixels(25);
		gd.horizontalSpan = 3;
		text.setLayoutData(gd);

		Button updateReferencesButton = addOptionalUpdateReferencesCheckbox(composite);
		this.targetFile = getResourceFile();
		if (targetFile != null) {
			addResourceRenameCheckbox(composite, updateReferencesButton);
		}

		// link to open preference page
		Link link = new Link(composite, SWT.NONE);
		link.setText("<a>" + RenameMessages.CRenameRefactoringInputPage_link_preference + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(),
						CodeStylePreferencePage.PREF_ID, null, null);
				dialog.open();
			}
		});

		Dialog.applyDialogFont(superComposite);
	}

	protected Button addResourceRenameCheckbox(Composite result,
			final Button updateReferencesButton) {
		final Button resourceRename = new Button(result, SWT.CHECK);
		resourceRename.setText("&Simple Resource Rename / Change Extension?"); //$NON-NLS-1$

		IPreferenceStore preferences = CUIPlugin.getDefault().getPreferenceStore();
		preferences.setDefault(SIMPLE_RESOURCE_RENAME, false); // Default
																// is
																// always
																// false
																// to
																// rename
																// resources.
		boolean simpleResourceRenameBool = preferences.getBoolean(SIMPLE_RESOURCE_RENAME);
		resourceRename.setSelection(simpleResourceRenameBool);
		resourceRename.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IPreferenceStore preferences = CUIPlugin.getDefault().getPreferenceStore();
				boolean simpleResourceRenameBool = resourceRename.getSelection();
				updateReferencesButton.setVisible(!simpleResourceRenameBool);
				preferences.setValue(SIMPLE_RESOURCE_RENAME, simpleResourceRenameBool);

				// Must be the last thing.
				if (simpleResourceRenameBool) {
					setTextToResourceName();
				} else {
					setTextToFullName();
				}
			}

		});
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		resourceRename.setLayoutData(gridData);
		updateReferencesButton.setVisible(!simpleResourceRenameBool);
		if (simpleResourceRenameBool) {
			setTextToResourceName();
		}
		return resourceRename;
	}

	private Button addOptionalUpdateReferencesCheckbox(Composite result) {
		final Button updateReferences = new Button(result, SWT.CHECK);
		updateReferences.setText("&Update References?"); //$NON-NLS-1$

		IPreferenceStore preferences = CUIPlugin.getDefault().getPreferenceStore();
		preferences.setDefault(UPDATE_REFERENCES, true); // Default is
															// always
															// true to
															// update
															// references.
		boolean updateRefs = preferences.getBoolean(UPDATE_REFERENCES);
		updateReferences.setSelection(updateRefs);
		updateReferences.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IPreferenceStore preferences = CUIPlugin.getDefault().getPreferenceStore();
				boolean updateRefs = updateReferences.getSelection();
				preferences.setValue(UPDATE_REFERENCES, updateRefs);
			}
		});
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		updateReferences.setLayoutData(gridData);
		return updateReferences;
	}

	private IFile getResourceFile() {
		CRenameRefactoring refactoring = (CRenameRefactoring) getRefactoring();
		if (refactoring == null)
			return null;
		return refactoring.getProcessor().getArgument().getSourceFile();
	}

	/**
	 * Tests whether each character in the given string is a valid identifier.
	 *
	 * @param str
	 * @return <code>true</code> if the given string is a word
	 */
	public static boolean isValidIdentifier(final String str, boolean acceptPoint) {
		if (str == null)
			return false;
		int len = str.length();
		if (len == 0)
			return false;

		char c = '\0';
		boolean lastWasPoint = false;
		for (int i = 0; i < len; i++) {
			c = str.charAt(i);
			if (i == 0) {
				if (!Character.isJavaIdentifierStart(c)) {
					return false;
				}
			} else {
				if (!Character.isJavaIdentifierPart(c)) {
					if (acceptPoint && c == '.') {
						if (lastWasPoint) {
							return false; // can't have 2 consecutive dots.
						}
						lastWasPoint = true;
						continue;
					}
					return false;
				}
			}
			lastWasPoint = false;

		}
		if (c == '.') {
			// if the last char is a point, don't accept it (i.e.: only accept
			// at middle).
			return false;
		}
		return true;
	}

}
