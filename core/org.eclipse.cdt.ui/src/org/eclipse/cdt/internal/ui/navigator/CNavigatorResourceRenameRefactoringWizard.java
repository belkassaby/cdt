package org.eclipse.cdt.internal.ui.navigator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class CNavigatorResourceRenameRefactoringWizard extends RefactoringWizard {

	private final String fInputPageDescription;
	private String fInitialSetting;

	public CNavigatorResourceRenameRefactoringWizard(Refactoring refactoring, String defaultPageTitle,
			String inputPageDescription, String initialValue) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE);
		setDefaultPageTitle(defaultPageTitle);
		fInputPageDescription = inputPageDescription;
		this.fInitialSetting = initialValue;
		Assert.isNotNull(this.fInitialSetting);
	}

	@Override
	protected void addUserInputPages() {
		UserInputWizardPage page = new CNavigatorResourceRenameRefactoringInputPage(fInputPageDescription, true, fInitialSetting);
		addPage(page);
	}

}
