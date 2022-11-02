package vikas.mrzscanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import vikas.mrzscanner.util.CameraPermissionsDialogFragment

class LaunchActivity : AppCompatActivity(),
    CameraPermissionsDialogFragment.CameraPermissionsGrantedCallback {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)




        findViewById<Button>(R.id.fab).setOnClickListener { view ->
            checkForPermission()
        }
    }


    fun checkForPermission() {
        if (!isCameraPermissionGranted(this)) {
            val cameraDialog: CameraPermissionsDialogFragment =
                CameraPermissionsDialogFragment.newInstance()
            cameraDialog.setListener(this)
            cameraDialog.show(
                getSupportFragmentManager(),
                CameraPermissionsDialogFragment::class.java.getName()
            )
        } else
            onCameraPermissionGranted()
    }


    fun isCameraPermissionGranted(context: Context?): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) true else ContextCompat.checkSelfPermission(
            context!!, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCameraPermissionGranted() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onCameraPermissionRejected() {
        //Do nothing
    }
}