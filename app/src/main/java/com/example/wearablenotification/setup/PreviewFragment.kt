package com.example.wearablenotification.setup

import android.Manifest
import android.content.Context.SENSOR_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.wearablenotification.main.MainActivity
import com.example.wearablenotification.R
import kotlinx.android.synthetic.main.fragment_preview.*
import kotlinx.android.synthetic.main.fragment_preview.view.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PreviewFragment : Fragment(), SensorEventListener {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor

    private var sensorX = 0.0f
    private var sensorY = 0.0f
    private var sensorZ = 0.0f

    /*
    private val sensorXMIN = 8.30f
    private val sensorYMIN = -0.50f
    private val sensorYMAX = 0.50f
    private val sensorZMIN = -4.50f
    private val sensorZMAX = 0.50f
     */

    private val sensorXMIN = 9.0f
    private val sensorYMIN = -0.50f
    private val sensorYMAX = 0.50f
    private val sensorZMIN = -3.50f
    private val sensorZMAX = 0.1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(
                REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        sensorManager = activity?.getSystemService(SENSOR_SERVICE) as SensorManager
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onResume() {
        super.onResume()

        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_preview, container, false)

        /*
        view.back_button_preview_fragment.setOnClickListener {
            findNavController().popBackStack()
        }
         */
        view.start_button_preview_fragment.setOnClickListener {

            if(checkAngle()) {
                activity.apply {
                    val intent = Intent(this, MainActivity::class.java)

                    startActivity(intent)
                }
            } else {
                Toast.makeText(activity,
                    "取り付け角度が不適切です．角度を再調整してください",
                    Toast.LENGTH_SHORT).show()
            }

        }

        return view
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                    .setTargetResolution(Size(1920, 1080))
                    .build()
                    .also {
                        it.setSurfaceProvider(preview_view.createSurfaceProvider())
                    }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ActivityCompat.checkSelfPermission(
            requireActivity(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(activity,
                    "カメラの使用が拒否されたため終了しました",
                    Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {

        if(event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            sensorX = event.values[0]
            sensorY = event.values[1]
            sensorZ = event.values[2]

            if(checkAngle()) {
                check_angle_text_view_preview.text = "適切な角度です．STARTボタンを押してください"
            } else {
                when {
                    sensorY < sensorYMIN || sensorY > sensorYMAX -> {
                        check_angle_text_view_preview.text = "カメラが傾いています"
                    }
                    sensorZ > sensorZMAX -> {
                        check_angle_text_view_preview.text = "カメラが下を向いています"
                    }
                    sensorZ < sensorZMIN -> {
                        check_angle_text_view_preview.text = "カメラが上を向いています"
                    }
                    else -> {
                        check_angle_text_view_preview.text = "カメラの角度が不適切です"
                    }
                }
            }

            Log.d(TAG, "X: $sensorX, Y: $sensorY, Z:$sensorZ")
        }
    }

    private fun checkAngle(): Boolean {
        return sensorX > sensorXMIN && sensorY > sensorYMIN && sensorY < sensorYMAX &&
                sensorZ > sensorZMIN && sensorZ < sensorZMAX
    }

    companion object {
        private const val TAG = "PreviewFragment"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}