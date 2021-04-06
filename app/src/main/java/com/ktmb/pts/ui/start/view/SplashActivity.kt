package com.ktmb.pts.ui.start.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import com.ktmb.pts.R
import com.ktmb.pts.base.BaseActivity
import com.ktmb.pts.databinding.ActivitySplashBinding
import com.ktmb.pts.ui.main.view.MainActivity
import com.ktmb.pts.ui.start.viewmodel.SplashViewModel
import com.ktmb.pts.utilities.*

class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var viewModel: SplashViewModel

    companion object {
        private const val REQUEST_LOCATION_ACCESS = 1000

        fun newIntent(context: Context): Intent {
            val intent = Intent(context, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_splash
        )

        viewModel = ViewModelProvider.NewInstanceFactory().create(SplashViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        init()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_ACCESS -> {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ),
                        REQUEST_LOCATION_ACCESS
                    )
                } else {
                    init()
                }
            }
        }
    }

    override fun onRetryButtonClick(view: View) {
        super.onRetryButtonClick(view)
        if (!NavigationManager.isLocationPermissionGranted(this)) {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri: Uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        } else {
            init()
        }
    }

    private fun init() {
        when {
            !NavigationManager.isLocationPermissionGranted(this) -> {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ),
                    REQUEST_LOCATION_ACCESS
                )
                viewModel.showError(
                    errorTitle = getString(R.string.error_permission_title),
                    errorMessage = getString(R.string.error_permission_location_message),
                    showTryAgain = true,
                    tryAgainMessage = getString(R.string.label_go_to_settings)
                )
            }
            else -> {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener {
                        if (!AccountManager.isFirebaseTokenAlreadyStored(it)) {
                            saveToken(it)
                        } else {
                            getReportTypes()
                        }
                    }
                    .addOnFailureListener {
                        Log.e("Firebase Messaging", it.toString())
                        getReportTypes()
                    }
            }
        }
    }

    private fun saveToken(token: String) {
        viewModel.hideError()
        viewModel.saveFirebaseToken(token).observe(this, Observer {
            it?.let { resource ->
                when (resource.status) {
                    Status.LOADING ->
                        viewModel.showProgress()
                    Status.ERROR -> {
                        viewModel.hideProgress()
                        getReportTypes()
                    }
                    Status.SUCCESS -> {
                        viewModel.hideProgress()
                        AccountManager.saveFirebaseToken(resource.data?.token)
                        getReportTypes()
                    }
                }
            }
        })
    }

    private fun getReportTypes() {
        viewModel.hideError()
        viewModel.getReportTypes().observe(this, Observer {
            it?.let { resource ->
                when (resource.status) {
                    Status.LOADING ->
                        viewModel.showProgress()
                    Status.ERROR -> {
                        viewModel.hideProgress()
                        viewModel.showError(errorMessage = it.message, showTryAgain = true)
                    }
                    Status.SUCCESS -> {
                        viewModel.hideProgress()
                        getTracks()
                    }
                }
            }
        })
    }

    private fun getTracks() {
        if (ConfigManager.getTracks() == null) {
            viewModel.hideError()
            viewModel.getTracks().observe(this, Observer {
                it?.let { resource ->
                    when (resource.status) {
                        Status.LOADING ->
                            viewModel.showProgress()
                        Status.ERROR -> {
                            viewModel.hideProgress()
                            viewModel.showError(errorMessage = it.message, showTryAgain = true)
                        }
                        Status.SUCCESS -> {
                            viewModel.hideProgress()
                            proceed()
                        }
                    }
                }
            })
        } else {
            proceed()
        }
    }

    private fun proceed() {
        startActivity(MainActivity.newIntent(this))
    }
}