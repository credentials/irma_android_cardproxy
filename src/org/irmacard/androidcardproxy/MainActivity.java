package org.irmacard.androidcardproxy;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

import net.sf.scuba.smartcards.CardServiceException;
import net.sf.scuba.smartcards.IsoDepCardService;
import net.sf.scuba.smartcards.ProtocolCommand;
import net.sf.scuba.smartcards.ProtocolResponse;
import net.sf.scuba.smartcards.ProtocolResponses;
import net.sf.scuba.smartcards.ResponseAPDU;
import org.apache.http.entity.StringEntity;
import org.irmacard.android.util.pindialog.EnterPINDialogFragment;
import org.irmacard.android.util.pindialog.EnterPINDialogFragment.PINDialogListener;
import org.irmacard.androidcardproxy.messages.EventArguments;
import org.irmacard.androidcardproxy.messages.PinResultArguments;
import org.irmacard.androidcardproxy.messages.ReaderMessage;
import org.irmacard.androidcardproxy.messages.ReaderMessageDeserializer;
import org.irmacard.androidcardproxy.messages.ResponseArguments;
import org.irmacard.androidcardproxy.messages.TransmitCommandSetArguments;
import org.irmacard.idemix.IdemixService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;


public class MainActivity extends Activity implements PINDialogListener {
	private String TAG = "CardProxyMainActivity";
	private NfcAdapter nfcA;
	private PendingIntent mPendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;

	// PIN handling
	private int tries = -1;
	
	// State variables
	private IsoDep lastTag = null;
	
	private int activityState = STATE_IDLE;
	
	// New states
	private static final int STATE_IDLE = 1;
	private static final int STATE_CONNECTING_TO_SERVER = 2;
	private static final int STATE_CONNECTED = 3;
	private static final int STATE_READY = 4;
	private static final int STATE_COMMUNICATING = 5;
	private static final int STATE_WAITING_FOR_PIN = 6;

	// Timer for testing card connectivity
	Timer timer;
	private static final int CARD_POLL_DELAY = 2000;
	
	// Timer for briefly displaying feedback messages on CardProxy
	CountDownTimer cdt;
	private static final int FEEDBACK_SHOW_DELAY = 10000;
	private boolean showingFeedback = false;

	// Counter for number of connection tries
	private static final int MAX_RETRIES = 3;
	private int retry_counter = 0;

	private void setState(int state) {
    	Log.i(TAG,"Set state: " + state);
    	activityState = state;

    	switch (activityState) {
		case STATE_IDLE:
			lastTag = null;
			break;
		default:
			break;
    	}

    	setUIForState();
	}

    private void setUIForState() {
    	int imageResource = 0;
    	int statusTextResource = 0;
    	int feedbackTextResource = 0;

    	switch (activityState) {
		case STATE_IDLE:
			imageResource = R.drawable.irma_icon_place_card_520px;
			statusTextResource = R.string.status_idle;
			break;
		case STATE_CONNECTING_TO_SERVER:
			imageResource = R.drawable.irma_icon_place_card_520px;
			statusTextResource = R.string.status_connecting;
			break;
		case STATE_CONNECTED:
			imageResource = R.drawable.irma_icon_place_card_520px;
			statusTextResource = R.string.status_connected;
			feedbackTextResource = R.string.feedback_waiting_for_card;
			break;
		case STATE_READY:
			imageResource = R.drawable.irma_icon_card_found_520px;
			statusTextResource = R.string.status_ready;
			break;
		case STATE_COMMUNICATING:
			imageResource = R.drawable.irma_icon_card_found_520px;
			statusTextResource = R.string.status_communicating;
			break;
		case STATE_WAITING_FOR_PIN:
			imageResource = R.drawable.irma_icon_card_found_520px;
			statusTextResource = R.string.status_waitingforpin;
			break;
		default:
			break;
		}
    	
    	((TextView)findViewById(R.id.status_text)).setText(statusTextResource);
    	if(!showingFeedback)
    		((ImageView)findViewById(R.id.statusimage)).setImageResource(imageResource);

		if(feedbackTextResource != 0)
			((TextView)findViewById(R.id.status_text)).setText(feedbackTextResource);
	}
	
	private void setFeedback(String message, String state) {
    	int imageResource = 0;

    	setUIForState();

		if (state.equals("success")) {
			imageResource = R.drawable.irma_icon_ok_520px;
		} if (state.equals("warning")) {
			imageResource = R.drawable.irma_icon_warning_520px;
		} if (state.equals("failure")) {
			imageResource = R.drawable.irma_icon_missing_520px;
		}

    	((TextView)findViewById(R.id.feedback_text)).setText(message);

    	if(imageResource != 0) {
    		((ImageView)findViewById(R.id.statusimage)).setImageResource(imageResource);
    		showingFeedback = true;
    	}

		if(cdt != null)
			cdt.cancel();

		cdt = new CountDownTimer(FEEDBACK_SHOW_DELAY, 1000) {
			public void onTick(long millisUntilFinished) {
			}

			public void onFinish() {
				clearFeedback();
			}
		}.start();
	}

	private void clearFeedback() {
		showingFeedback = false;
		((TextView)findViewById(R.id.feedback_text)).setText("");
		setUIForState();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
        // NFC stuff
        nfcA = NfcAdapter.getDefaultAdapter(getApplicationContext());
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Setup an intent filter for all TECH based dispatches
        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        mFilters = new IntentFilter[] { tech };

        // Setup a tech list for all IsoDep cards
        mTechLists = new String[][] { new String[] { IsoDep.class.getName() } };

	    setState(STATE_IDLE);

	    timer = new Timer();
	    timer.scheduleAtFixedRate(new CardPollerTask(), CARD_POLL_DELAY, CARD_POLL_DELAY);
	}


	@Override
	protected void onPause() {
		super.onPause();
    	if (nfcA != null) {
    		nfcA.disableForegroundDispatch(this);
    	}
	}
	
	@Override
	protected void onResume() {
        super.onResume();
        Log.i(TAG, "Action: " + getIntent().getAction());
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        } else if (Intent.ACTION_VIEW.equals(getIntent().getAction()) && "cardproxy".equals(getIntent().getScheme())) {
        	// TODO: this is legacy code to have the cardproxy app respond to cardproxy:// urls. This doesn't
        	// work anymore, should check whether we want te re-enable it.
        	Uri uri = getIntent().getData();
        	String startURL = "http://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
        	gotoConnectingState(startURL);
        }
        if (nfcA != null) {
        	nfcA.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
        }
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	private static final int MESSAGE_STARTGET = 1;
	String currentReaderURL = "";
	int currentHandlers = 0;
	
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STARTGET:
				Log.i(TAG,"MESSAGE_STARTGET received in handler!");
				AsyncHttpClient client = new AsyncHttpClient();
				client.setTimeout(50000); // timeout of 50 seconds
				client.setUserAgent("org.irmacard.androidcardproxy");
				
				client.get(MainActivity.this, currentReaderURL, new AsyncHttpResponseHandler() {
					@Override
					public void onSuccess(int arg0, String responseData) {
						if (!responseData.equals("")) {
							//Toast.makeText(MainActivity.this, responseData, Toast.LENGTH_SHORT).show();
							handleChannelData(responseData);
						}
						
						// Do a new request, but only if no new requests have started
						// in the mean time
						if (currentHandlers <= 1) {
							Message newMsg = new Message();
							newMsg.what = MESSAGE_STARTGET;
							if(!(activityState == STATE_IDLE))
									handler.sendMessageDelayed(newMsg, 200);
						}
					}
					@Override
					public void onFailure(Throwable arg0, String arg1) {
						if(activityState != STATE_CONNECTING_TO_SERVER) {
							retry_counter = 0;
							return;
						}

						retry_counter += 1;

						// We should try again, but only if no new requests have started
						// and we should wait a bit longer
						if (currentHandlers <= 1 && retry_counter < MAX_RETRIES) {
							Message newMsg = new Message();
							setFeedback("Trying to reach server again...", "none");
							newMsg.what = MESSAGE_STARTGET;
							handler.sendMessageDelayed(newMsg, 5000);
						} else {
							retry_counter = 0;
							setFeedback("Failed to connect to server", "warning");
							setState(STATE_IDLE);
						}
						
					}
					public void onStart() {
						currentHandlers += 1;
					};
					public void onFinish() {
						currentHandlers -= 1;
					};
				});
			
				break;

			default:
				break;
			}
		}
	};

	private String currentWriteURL = null;
	private ReaderMessage lastReaderMessage = null;
	
	private void handleChannelData(String data) {
		Gson gson = new GsonBuilder().
				registerTypeAdapter(ProtocolCommand.class, new ProtocolCommandDeserializer()).
				registerTypeAdapter(ReaderMessage.class, new ReaderMessageDeserializer()).
				create();
		if (activityState == STATE_CONNECTING_TO_SERVER) {
			// this is the message that containts the url to write to
			JsonParser p = new JsonParser();
			String write_url = p.parse(data).getAsJsonObject().get("write_url").getAsString();
			currentWriteURL = write_url;
			setState(STATE_CONNECTED);
			// Signal to the other end that we we are ready accept commands
			postMessage(
					new ReaderMessage(ReaderMessage.TYPE_EVENT, ReaderMessage.NAME_EVENT_CARDREADERFOUND, null,
							new EventArguments().withEntry("type", "phone")));
		} else {
			ReaderMessage rm;
			try {
				Log.i(TAG, "Length (real): " + data);
				JsonReader reader = new JsonReader(new StringReader(data));
				reader.setLenient(true);
				rm = gson.fromJson(reader, ReaderMessage.class);
			} catch(Exception e) {
				e.printStackTrace();
				return;
			}
			lastReaderMessage = rm;
			if (rm.type.equals(ReaderMessage.TYPE_COMMAND)) {
				Log.i(TAG, "Got command message");

				if (activityState != STATE_READY) {
					// FIXME: Only when ready can we handle commands
					throw new RuntimeException(
							"Illegal command from server, no card currently connected");
				}

				if (rm.name.equals(ReaderMessage.NAME_COMMAND_AUTHPIN)) {
					askForPIN();
				} else {
					setState(STATE_COMMUNICATING);
					new ProcessReaderMessage().execute(new ReaderInput(lastTag, rm));
				}
			} else if (rm.type.equals(ReaderMessage.TYPE_EVENT)) {
				EventArguments ea = (EventArguments)rm.arguments;
				if (rm.name.equals(ReaderMessage.NAME_EVENT_STATUSUPDATE)) {
					String state = ea.data.get("state");
					String feedback = ea.data.get("feedback");
					if (state != null) {
						setFeedback(feedback, state);
					}
				} else if(rm.name.equals(ReaderMessage.NAME_EVENT_TIMEOUT)) {
					setState(STATE_IDLE);
				} else if(rm.name.equals(ReaderMessage.NAME_EVENT_DONE)) {
					setState(STATE_IDLE);
				}
			}
		}
	}
	
	
	private void postMessage(ReaderMessage rm) {
		if (currentWriteURL != null) {
			Gson gson = new GsonBuilder().
					registerTypeAdapter(ProtocolResponse.class, new ProtocolResponseSerializer()).
					create();
			String data = gson.toJson(rm);
			AsyncHttpClient client = new AsyncHttpClient();
			try {
				client.post(MainActivity.this, currentWriteURL, new StringEntity(data) , "application/json",  new AsyncHttpResponseHandler() {
					@Override
					public void onSuccess(int arg0, String arg1) {
						// TODO: Should there be some simple user feedback?
						super.onSuccess(arg0, arg1);
					}
					@Override
					public void onFailure(Throwable arg0, String arg1) {
						// TODO: Give proper feedback to the user that we are unable to send stuff
						super.onFailure(arg0, arg1);
					}
				});
			} catch (UnsupportedEncodingException e) {
				// Ignore, shouldn't happen ;)
				e.printStackTrace();
			}
		}
	}
	
	public void onMainTouch(View v) {
		if (activityState == STATE_IDLE) {
			lastTag = null;
			startQRScanner("Scan the QR image in the browser.");
		}
	}
	
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }
    
    public void processIntent(Intent intent) {
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    	IsoDep tag = IsoDep.get(tagFromIntent);
    	// Only proces tag when we're actually expecting a card.
    	if (tag != null && activityState == STATE_CONNECTED) {
    		setState(STATE_READY);
    		postMessage(new ReaderMessage(ReaderMessage.TYPE_EVENT, ReaderMessage.NAME_EVENT_CARDFOUND, null));
    		lastTag = tag;
    	}    	
    }

    class CardPollerTask extends TimerTask {
    	/**
    	 * Dirty Hack. Since android doesn't produce events when an NFC card
    	 * is lost, we send a command to the card, and see if it still responds.
    	 * It is important that this command does not affect the card's state.
    	 * 
    	 * FIXME: The command we sent is IRMA dependent, which is dangerous when
    	 * the proxy is used with other cards/protocols.
    	 */
    	public void run() {
			// Only in the ready state do we need to actively check for card
			// presence.
    		if(activityState == STATE_READY) {
    			Log.i("CardPollerTask", "Checking card presence");
				ReaderMessage rm = new ReaderMessage(
						ReaderMessage.TYPE_COMMAND,
						ReaderMessage.NAME_COMMAND_IDLE, "idle");

				new ProcessReaderMessage().execute(new ReaderInput(lastTag, rm));
			}
    	}
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		IntentResult scanResult = IntentIntegrator
				.parseActivityResult(requestCode, resultCode, data);

		// Process the results from the QR-scanning activity
		if (scanResult != null) {
			String contents = scanResult.getContents();
			if (contents != null) {
				gotoConnectingState(contents);
			}
		}
	}
	
	private void gotoConnectingState(String url) {
		Log.i(TAG, "Start channel listening: " + url);
		currentReaderURL = url;
		Message msg = new Message();
		msg.what = MESSAGE_STARTGET;
		setState(STATE_CONNECTING_TO_SERVER);
		handler.sendMessage(msg);
	}
	
	public void askForPIN() {
		setState(STATE_WAITING_FOR_PIN);
		DialogFragment newFragment = EnterPINDialogFragment.getInstance(tries);
	    newFragment.show(getFragmentManager(), "pinentry");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public void startQRScanner(String message) {
		IntentIntegrator integrator = new IntentIntegrator(this);
		integrator.setPrompt(message);
    	integrator.initiateScan();
	}
	
	
	private class ReaderInput {
		public IsoDep tag;
		public ReaderMessage message;
		public String pincode = null;
		public ReaderInput(IsoDep tag, ReaderMessage message) {
			this.tag = tag;
			this.message = message;
		}
		
		public ReaderInput(IsoDep tag, ReaderMessage message, String pincode) {
			this.tag = tag;
			this.message = message;
			this.pincode = pincode;
		}
	}
    
	private class ProcessReaderMessage extends AsyncTask<ReaderInput, Void, ReaderMessage> {
		

		@Override
		protected ReaderMessage doInBackground(ReaderInput... params) {
			ReaderInput input = params[0];
			IsoDep tag = input.tag;
			ReaderMessage rm = input.message;

			// It seems sometimes tag is null afterall
			if(tag == null) {
				Log.e("ReaderMessage", "tag is null, this should not happen!");
				return new ReaderMessage(ReaderMessage.TYPE_EVENT, ReaderMessage.NAME_EVENT_CARDLOST, null);
			}

			// Make sure time-out is long enough (10 seconds)
			tag.setTimeout(10000);
			
			// TODO: The current version of the cardproxy shouldn't depend on idemix terminal, but for now
			// it is convenient.
			IdemixService is = new IdemixService(new IsoDepCardService(tag));
			try {
				if (!is.isOpen()) {
					// TODO: this is dangerous, this call to IdemixService already does a "select applet"
					is.open();
				}
				if (rm.name.equals(ReaderMessage.NAME_COMMAND_AUTHPIN)) {
					if (input.pincode != null) {
						// TODO: this should be done properly, maybe without using IdemixService?
						tries = is.sendCredentialPin(input.pincode.getBytes());

						return new ReaderMessage("response", rm.name, rm.id, new PinResultArguments(tries));
					}
				} else if (rm.name.equals(ReaderMessage.NAME_COMMAND_TRANSMIT)) {
					TransmitCommandSetArguments arg = (TransmitCommandSetArguments)rm.arguments;
					ProtocolResponses responses = new ProtocolResponses();
					for (ProtocolCommand c: arg.commands) {
						ResponseAPDU apdu_response = is.transmit(c.getAPDU());
						responses.put(c.getKey(), 
								new ProtocolResponse(c.getKey(), apdu_response));
						if(apdu_response.getSW() != 0x9000) {
							break;
						}
					}
					return new ReaderMessage(ReaderMessage.TYPE_RESPONSE, rm.name, rm.id, new ResponseArguments(responses));
				} else if (rm.name.equals(ReaderMessage.NAME_COMMAND_IDLE)) {
					// FIXME: IRMA specific implementation,
					// This command is not allowed in normal mode,
					// so it will result in an exception.
					Log.i("READER", "Processing idle command");
					is.getCredentials();
				}
				
			} catch (CardServiceException e) {
				// FIXME: IRMA specific handling of failed command, this is too generic.
				if(e.getMessage().contains("Command failed:") && e.getSW() == 0x6982) {
					return null;
				}
				e.printStackTrace();
				// TODO: maybe also include the information about the exception in the event?
				return new ReaderMessage(ReaderMessage.TYPE_EVENT, ReaderMessage.NAME_EVENT_CARDLOST, null);
			} catch (IllegalStateException e) {
				// This sometimes props up when applications comes out of suspend for now we just ignore this.
				Log.i("READER", "IllegalStateException ignored");
				return null;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(ReaderMessage result) {
			if(result == null)
				return;

			// Update state
			if( result.type.equals(ReaderMessage.TYPE_EVENT) &&
				result.name.equals(ReaderMessage.NAME_EVENT_CARDLOST)) {
				// Connection to the card is lost
				setState(STATE_CONNECTED);
			} else {
				if(activityState == STATE_COMMUNICATING) {
					setState(STATE_READY);
				}
			}

			if (result.name.equals(ReaderMessage.NAME_COMMAND_AUTHPIN)) {
				// Handle pin separately, abort if pin incorrect and more tries
				// left
				PinResultArguments args = (PinResultArguments) result.arguments;
				if (!args.success) {
					if (args.tries > 0) {
						// Still some tries left, asking again
						setState(STATE_WAITING_FOR_PIN);
						askForPIN();
						return; // do not send a response yet.
					} else {
						// FIXME: No more tries left
						// Need to go to error state
					}
				}
			}

			// Post result to browser
			postMessage(result);
		}
	}	

	@Override
	public void onPINEntry(String dialogPincode) {
		// TODO: in the final version, the following debug code should go :)
		Log.i(TAG, "PIN entered: " + dialogPincode);
		setState(STATE_COMMUNICATING);
		new ProcessReaderMessage().execute(new ReaderInput(lastTag, lastReaderMessage, dialogPincode));
	}

	@Override
	public void onPINCancel() {
		Log.i(TAG, "PIN entry canceled!");
		postMessage(
				new ReaderMessage(ReaderMessage.TYPE_RESPONSE, 
						ReaderMessage.NAME_COMMAND_AUTHPIN, 
						lastReaderMessage.id, 
						new ResponseArguments("cancel")));
		
		setState(STATE_READY);
	}
	
	public static class ErrorFeedbackDialogFragment extends DialogFragment {
		public static ErrorFeedbackDialogFragment newInstance(String title, String message) {
			ErrorFeedbackDialogFragment f = new ErrorFeedbackDialogFragment();
			Bundle args = new Bundle();
			args.putString("message", message);
			args.putString("title", title);
			f.setArguments(args);
			return f;
		}
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(getArguments().getString("message"))
			.setTitle(getArguments().getString("title"))
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			});
			return builder.create();
		}
	}

	@Override
	public void onBackPressed() {
		// When we are not in IDLE state, return there
		if(activityState != STATE_IDLE) {
			if(cdt != null)
				cdt.cancel();

			setState(STATE_IDLE);
			clearFeedback();
		} else {
			// We are in Idle, do what we always do
			super.onBackPressed();
		}
	}
}
