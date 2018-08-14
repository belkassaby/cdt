package org.eclipse.cdt.internal.ui.refactoring.rename.resources;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Display;

import org.eclipse.cdt.internal.ui.refactoring.rename.CRenameProcessor;

public class CRenameRefactoring {

	private static final Logger logger = Logger.getLogger(CRenameRefactoring.class.getName());
	public static String rename() {
		try {
//			List<RefactoringRequest> actualRequests = request.getRequests();
//			if (actualRequests.size() == 1) {
//				RefactoringRequest req = actualRequests.get(0);

				// Note: if it's already a ModuleRenameRefactoringRequest, no
				// need to change anything.
//				if (!(req.isModuleRenameRefactoringRequest())) {
//
//					// Note: if we're renaming an import, we must change to the
//					// appropriate req
//					ICRefactoring pyRefactoring = AbstractCRefactoring.getCRefactoring();
//					ItemPointer[] pointers = pyRefactoring.findDefinition(req);
//					for (ItemPointer pointer : pointers) {
//						Definition definition = pointer.definition;
//						if (RefactorProcessFactory.isModuleRename(definition)) {
//							try {
//								request = new CRefactoringRequest(new ModuleRenameRefactoringRequest(
//										definition.module.getFile(), req.nature, null));
//							} catch (IOException e) {
//								throw new RuntimeException(e);
//							}
//						}
//					}
//				}
//			}
			// TODO add argument to rename processor
			CRenameProcessor entryPoint = new CRenameProcessor(null, null);
			RenameRefactoring renameRefactoring = new RenameRefactoring(entryPoint);
//			request.fillInitialNameAndOffset();

			String title = "Rename";
//			if (request instanceof MultiModuleMoveRefactoringRequest) {
//				MultiModuleMoveRefactoringRequest multiModuleMoveRefactoringRequest = (MultiModuleMoveRefactoringRequest) request;
//				title = "Move To package (project: "
//						+ multiModuleMoveRefactoringRequest.getTarget().getProject().getName() + ")";
//			}
			final CRenameRefactoringWizard wizard = new CRenameRefactoringWizard(renameRefactoring, title,
					"inputPageDescription");
			try {
				RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
				op.run(Display.getDefault().getActiveShell(), "Rename Refactor Action");
			} catch (InterruptedException e) {
				// do nothing. User action got cancelled
			}
		} catch (Exception e) {
			logger.log(new LogRecord(Level.SEVERE, e.getMessage()));
		}
		return null;
	}

}
