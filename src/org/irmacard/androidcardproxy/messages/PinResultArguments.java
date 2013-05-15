package org.irmacard.androidcardproxy.messages;

public class PinResultArguments extends ResponseArguments {
	public int tries;
	public boolean success;
	
	public PinResultArguments(int tries) {
		super((tries == -1) ? "success" : "failure");
		success = tries == -1;
		this.tries = tries;
	}
}
