package org.irmacard.androidcardproxy;

import java.util.List;

import service.ProtocolCommand;

public class CommandSet {
    public List<ProtocolCommand> commands;
    public String responseurl;
    public boolean usePIN;
    public String confirmationMessage;
    public boolean askConfirmation = false;
}
