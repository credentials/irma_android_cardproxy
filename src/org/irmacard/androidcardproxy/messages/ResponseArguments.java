package org.irmacard.androidcardproxy.messages;

import net.sf.scuba.smartcards.ProtocolResponses;

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
