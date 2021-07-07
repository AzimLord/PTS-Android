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
import com.ktmb.pts.data.model.LocationUpdate
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
    private lateinit var currentLocation: LocationUpdate

    private var reportTypes: ArrayList<ReportType>? = null

    companion object {
        private const val EXTRA_CURRENT_LOCATION = "currentLocation"

        fun newIntent(context: Context, currentLocation: LocationUpdate): Intent {
            val intent = Intent(context, NewReportActivity::class.java)
            intent.putExtra(EXTRA_CURRENT_LOCATION, currentLocation)
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

        currentLocation = intent.extras!!.getParcelable(EXTRA_CURRENT_LOCATION)!!

        init()
    }

    override fun setToolbar() {
        super.setToolbar()
        supportActionBar?.title = getString(R.string.label_send_a_report)
    }

    override fun onRetryButtonClick(view: View) {
        super.onRetryButtonClick(view)
        when {
            reportTypes == null -> {
                revealAnimation.unRevealActivity()
            }
            else -> {
                // Do nothing
            }
        }
    }

    override fun onReportTypeClick(position: Int, reportType: ReportType) {
        if (reportType.id == 0) {
            startActivity(NewReportPathActivity.newIntent(this, currentLocation))
        } else {
            createReport(reportType)
        }
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

        val request = NewReportRequest(currentLocation.newLocation.latitude, currentLocation.newLocation.longitude, reportType.id, currentLocation.trackKey)
        LogManager.log(request.toString(), localClassName)

        viewModel.newReport(request).observe(this, Observer {
            it?.let { resource ->
                when (resource.status) {
                    Status.LOADING ->
                        dialog = DialogManager.showLoading(this)
                    Status.ERROR -> {
                        dialog?.dismiss()
                        DialogManager.showErrorDialog(
                            this,
                            getString(R.string.error_default_message)
                        )
                    }
                    Status.SUCCESS -> {
                        dialog?.dismiss()
                        setResult(Activity.RESULT_OK)
                        revealAnimation.unRevealActivity()
                    }
                }
            }
        })
    }
}