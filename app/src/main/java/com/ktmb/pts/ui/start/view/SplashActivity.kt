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
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.ktmb.pts.R
import com.ktmb.pts.base.BaseActivity
import com.ktmb.pts.data.model.AppUpdate
import com.ktmb.pts.databinding.ActivitySplashBinding
import com.ktmb.pts.ui.main.view.MainActivity
import com.ktmb.pts.ui.start.viewmodel.SplashViewModel
import com.ktmb.pts.utilities.*
import java.io.File


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
    }

    override fun onStart() {
        super.onStart()
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
                            checkAppUpdate()
                        }
                    }
                    .addOnFailureListener {
                        Log.e("Firebase Messaging", it.toString())
                        checkAppUpdate()
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
                        checkAppUpdate()
                    }
                    Status.SUCCESS -> {
                        viewModel.hideProgress()
                        AccountManager.saveFirebaseToken(resource.data?.token)
                        checkAppUpdate()
                    }
                }
            }
        })
    }

    private fun checkAppUpdate() {
        viewModel.hideError()

        if (!packageManager.canRequestPackageInstalls()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${applicationContext.packageName}")
                    )
                )
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:" + applicationContext.packageName)
                startActivity(intent)
            }
        } else {
            viewModel.checkAppUpdate().observe(this, Observer {
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
                            if (resource.data == null) {
                                viewModel.showError(errorMessage = it.message, showTryAgain = true)
                            } else {
                                val appUpdate = resource.data
                                val isUpdateAvailable =
                                    Utilities.AppUpdateHelper.isUpdateAvailable(this, appUpdate)
                                if (isUpdateAvailable) {
                                    dialog = DialogManager.showAlertDialog(this,
                                        getString(R.string.dialog_title_app_update),
                                        getString(R.string.dialog_message_app_update),
                                        getString(R.string.label_app_update_update),
                                        View.OnClickListener {
                                            downloadAPK(appUpdate)
                                            dialog?.dismiss()
                                        })
                                } else {
                                    getReportTypes()
                                }
                            }
                        }
                    }
                }
            })
        }
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

    private fun downloadAPK(appUpdate: AppUpdate) {
        val dirPath = "$cacheDir/apk/"
        PRDownloader.download(appUpdate.downloadUrl, dirPath, appUpdate.latestVersion)
            .build()
            .setOnProgressListener {
                viewModel.showProgress()
            }
            .start(object : OnDownloadListener {
                override fun onDownloadComplete() {
                    viewModel.hideProgress()
                    installAPK("$dirPath${appUpdate.latestVersion}")
                    LogManager.log("Download completed")
                }

                override fun onError(error: Error) {
                    viewModel.hideProgress()
                    dialog = DialogManager.showAlertDialog(
                        this@SplashActivity,
                        getString(R.string.error_default_title),
                        getString(R.string.error_app_update_error_message),
                        getString(R.string.label_try_again),
                        View.OnClickListener {
                            downloadAPK(appUpdate)
                            dialog?.dismiss()
                        }
                    )
                    LogManager.log(
                        message = error.toString(),
                        sendToCrashlytics = true
                    )
                    FirebaseCrashlytics.getInstance().recordException(error.connectionException)
                }

            })
    }

    private fun installAPK(filePath: String) {
        val uri = FileProvider.getUriForFile(
            this,
            applicationContext.packageName.toString() + ".provider",
            File(filePath)
        )
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        startActivity(intent)
    }
}