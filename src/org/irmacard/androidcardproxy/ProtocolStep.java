package org.irmacard.androidcardproxy;

import java.util.List;

import net.sourceforge.scuba.smartcards.ProtocolCommand;

public class ProtocolStep {
	public String status;
    public List<ProtocolCommand> commands;
    public String responseurl;

    public boolean protocolDone = false;

    public boolean usePIN = false;

    public boolean askConfirmation = false;
    public String confirmationMessage;
    
    public String feedbackMessage;
    
    public String result = null;
}
