package org.irmacard.androidcardproxy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

public class EnterPINDialogFragment extends DialogFragment {
	
	public interface PINDialogListener {
		public void onPINEntry(String pincode);
        public void onPINCancel();
	}

    PINDialogListener mListener;
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_pinentry, null);
        builder.setView(dialogView)
        	.setTitle("Enter PIN")
	           .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int id) {
	                   EditText et = (EditText)dialogView.findViewById(R.id.pincode);
	                   String pincodeText = et.getText().toString();
	                   mListener.onPINEntry(pincodeText);
	               }
	           })
	           .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int id) {
	            	   mListener.onPINCancel();
	               }
	           });
        // Create the AlertDialog object and return it
        Dialog dialog = builder.create();
        // Make sure that the keyboard is always shown and doesn't require an additional touch
        // to focus the TextEdit view.
        dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return dialog;
    }
    
    // Override the Fragment.onAttach() method to instantiate the PINDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (PINDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement PINDialogListener");
        }
    }
}
