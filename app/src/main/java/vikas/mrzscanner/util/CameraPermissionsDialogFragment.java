package vikas.mrzscanner.util;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;


import vikas.mrzscanner.R;





public class CameraPermissionsDialogFragment extends DialogFragment {

    private Context context;
    private CameraPermissionsGrantedCallback listener;


    private AlertDialog settingsDialog;
    private AlertDialog retryDialog;

    public static CameraPermissionsDialogFragment newInstance() {
        return new CameraPermissionsDialogFragment();
    }

    public CameraPermissionsDialogFragment() {}

    @Override public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof CameraPermissionsGrantedCallback) {
            listener = (CameraPermissionsGrantedCallback) context;
        }

    }

    public CameraPermissionsGrantedCallback getListener() {
        return listener;
    }

    public void setListener(CameraPermissionsGrantedCallback listener) {
        this.listener = listener;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle);
        setCancelable(false);
        requestNecessaryPermissions();
    }



    @Override public void onDetach() {
        super.onDetach();
        context = null;
        listener = null;
    }



    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    //permissions have been accepted
                    if (listener != null) {
                        listener.onCameraPermissionGranted();
                        dismiss();
                    }

                } else {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        showAppSettingsDialog();
                    } else {
                        showRetryDialog();
                    }


                }
            });

    private void requestNecessaryPermissions() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void showAppSettingsDialog() {
        settingsDialog = new AlertDialog.Builder(context)
                .setTitle("Permissions Required")
                .setMessage("In order to take picture access to the camera and storage is needed. Please enable these permissions from the app settings.")
               .setCancelable(false)
                .setPositiveButton("App Settings", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", context.getApplicationContext().getPackageName(), null);
                        intent.setData(uri);
                        context.startActivity(intent);
                        dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                        if(listener!=null)
                        listener.onCameraPermissionRejected();
                    }
                }).create();
        settingsDialog.show();
    }

    private void showRetryDialog() {
        retryDialog = new AlertDialog.Builder(context)
                .setTitle("Permissions Declined")
                .setMessage("In order to take picture access to the camera and storage is needed.")
                .setCancelable(false)
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialogInterface, int i) {
                        requestNecessaryPermissions();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                        if(listener != null)
                            listener.onCameraPermissionRejected();
                    }
                }).create();
        retryDialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(settingsDialog!=null && settingsDialog.isShowing())
        {
            settingsDialog.dismiss();
            settingsDialog = null;
        }

        if(retryDialog != null && retryDialog.isShowing())
        {
            retryDialog.dismiss();
            retryDialog = null;
        }
    }

    public interface CameraPermissionsGrantedCallback {
        void onCameraPermissionGranted();
        void onCameraPermissionRejected();
    }
}
