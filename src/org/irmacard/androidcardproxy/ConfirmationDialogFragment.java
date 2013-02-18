package org.irmacard.androidcardproxy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class ConfirmationDialogFragment extends DialogFragment {
	
	public interface ConfirmationDialogListener {
		public void onConfirmationPositive();
		public void onConfirmationNegative();
	}
	
	ConfirmationDialogListener mListener;
	
	public static ConfirmationDialogFragment newInstance(String message) {
		ConfirmationDialogFragment f = new ConfirmationDialogFragment();
		Bundle args = new Bundle();
		args.putString("message", message);
		f.setArguments(args);
		return f;
	}
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getArguments().getString("message"))
         	   .setTitle("Please confirm")
               .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       mListener.onConfirmationPositive();
                   }
               })
               .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       mListener.onConfirmationNegative();
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
    // Override the Fragment.onAttach() method to instantiate the ConfirmationDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the ConfirmationDialogListener so we can send events to the host
            mListener = (ConfirmationDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement PINDialogListener");
        }
    }
}

