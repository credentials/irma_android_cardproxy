package org.irmacard.androidcardproxy.messages;

import net.sourceforge.scuba.smartcards.ProtocolResponses;

public class ResponseArguments extends ReaderMessageArguments {
	public String result = null;
	public ProtocolResponses responses = null;
	public ResponseArguments(ProtocolResponses responses) {
		this.responses = responses;
	}
	public ResponseArguments(String result) {
		this.result = result;
	}
}
