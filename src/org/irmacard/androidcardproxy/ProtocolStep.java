package org.irmacard.androidcardproxy;

import java.util.List;

import service.ProtocolCommand;

public class ProtocolStep {
    public List<ProtocolCommand> commands = null;
    public String responseurl = null;
    public boolean usePIN = false;
    public String feedbackMessage = null;
    public String confirmationMessage = null;
    public boolean askConfirmation = false;
    public boolean protocolDone = false;
    public String result = null;
    public String data = null;   
}
