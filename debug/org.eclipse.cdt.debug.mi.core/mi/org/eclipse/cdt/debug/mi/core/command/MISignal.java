/*
 *(c) Copyright QNX Software Systems Ltd. 2002.
 * All Rights Reserved.
 * 
 */

package org.eclipse.cdt.debug.mi.core.command;

import org.eclipse.cdt.debug.mi.core.output.MIOutput;
import org.eclipse.cdt.debug.mi.core.output.MIResultRecord;



/**
 * 
 *    signal SIGUSR1
 *
 */
public class MISignal extends CLICommand {

	MIOutput out;

	public MISignal(String arg) {
		super("signal " + arg); //$NON-NLS-1$
	}

	/**
	 *  This is a CLI command contraly to
	 *  the -exec-continue or -exec-run
	 *  it does not return so we have to fake
	 *  a return value. We return "^running"
	 */
	public MIOutput getMIOutput() {
		if (out == null) {
			out =  new MIOutput();
			MIResultRecord rr = new MIResultRecord();
			rr.setToken(getToken());
			rr.setResultClass(MIResultRecord.RUNNING);
			out.setMIResultRecord(rr);
		}
		return out;
	}

}
