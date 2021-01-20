package com.ktmb.pts.ui.report.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ktmb.pts.R
import com.ktmb.pts.animation.RevealAnimation
import com.ktmb.pts.base.BaseActivity
import com.ktmb.pts.data.model.ReportType
import com.ktmb.pts.data.request.NewReportRequest
import com.ktmb.pts.databinding.ActivityNewReportBinding
import com.ktmb.pts.ui.report.viewmodel.NewReportViewModel
import com.ktmb.pts.utilities.ConfigManager
import com.ktmb.pts.utilities.DialogManager
import com.ktmb.pts.utilities.LogManager
import com.ktmb.pts.utilities.Status


class NewReportActivity : BaseActivity(), ReportTypeAdapter.OnReportTypeClickListener {

    private lateinit var binding: ActivityNewReportBinding
    private lateinit var viewModel: NewReportViewModel
    private lateinit var adapter: ReportTypeAdapter

    private var latitude: Double? = null
    private var longitude: Double? = null
    private var reportTypes: ArrayList<ReportType>? = null

    companion object {
        private const val EXTRA_LATITUDE = "latitude"
        private const val EXTRA_LONGITUDE = "longitude"

        fun newIntent(context: Context, latitude: Double, longitude: Double): Intent {
            val intent = Intent(context, NewReportActivity::class.java)
            intent.putExtra(EXTRA_LATITUDE, latitude)
            intent.putExtra(EXTRA_LONGITUDE, longitude)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_new_report
        )

        revealAnimation = RevealAnimation(binding.root, intent, this)

        viewModel = ViewModelProvider.NewInstanceFactory().create(NewReportViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        adapter = ReportTypeAdapter(this, this)
        binding.adapter = adapter

        latitude = intent.extras?.getDouble(EXTRA_LATITUDE)
        longitude = intent.extras?.getDouble(EXTRA_LONGITUDE)

        if (latitude == null && longitude == null) {
            LogManager.log("Latitude & longitude is null", localClassName)
            viewModel.showError(
                showTryAgain = true,
                tryAgainMessage = getString(R.string.label_okay)
            )
        } else {
            init()
        }
    }

    override fun setToolbar() {
        super.setToolbar()
        supportActionBar?.title = "Send a report"
    }

    override fun onRetryButtonClick(view: View) {
        super.onRetryButtonClick(view)

        when {
            latitude == null && longitude == null -> {
                revealAnimation.unRevealActivity()
            }
            reportTypes == null -> {
                revealAnimation.unRevealActivity()
            }
            else -> {
                // Do nothing
            }
        }
    }

    override fun onReportTypeClick(position: Int, reportType: ReportType) {
        createReport(reportType)
    }

    private fun init() {
        reportTypes = ConfigManager.getReportTypes()

        if (reportTypes == null) {
            LogManager.log("Report types is null", localClassName)
            viewModel.showError(
                showTryAgain = true,
                tryAgainMessage = getString(R.string.label_okay)
            )
        } else {
            adapter.addReportTypes(reportTypes!!)
        }
    }

    private fun createReport(reportType: ReportType) {
        if (latitude == null && longitude == null) {
            DialogManager.showErrorDialog(
                this,
                title = getString(R.string.error_gps_not_available_title),
                message = getString(R.string.error_gps_not_available_message),
                positiveAction = View.OnClickListener {
                    finish()
                }
            )
        } else {
            val request = NewReportRequest(latitude!!, longitude!!, reportType.id)
            LogManager.log(request.toString(), localClassName)

            viewModel.newReport(request).observe(this, Observer {
                it?.let { resource ->
                    when (resource.status) {
                        Status.LOADING ->
                            viewModel.showProgress()
                        Status.ERROR -> {
                            viewModel.hideProgress()
                            DialogManager.showErrorDialog(
                                this,
                                getString(R.string.error_default_message)
                            )
                        }
                        Status.SUCCESS -> {
                            viewModel.hideProgress()
                            setResult(Activity.RESULT_OK)
                            revealAnimation.unRevealActivity()
                        }
                    }
                }
            })
        }
    }
}