package com.ktmb.pts.ui.credentials.view

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ktmb.pts.R
import com.ktmb.pts.base.BaseActivity
import com.ktmb.pts.databinding.ActivityLoginBinding
import com.ktmb.pts.databinding.ActivitySettingsBinding
import com.ktmb.pts.ui.credentials.viewmodel.LoginViewModel
import com.ktmb.pts.ui.credentials.viewmodel.SettingsViewModel
import com.ktmb.pts.ui.start.view.SplashActivity
import com.ktmb.pts.utilities.AccountManager
import com.ktmb.pts.utilities.DialogManager
import com.ktmb.pts.utilities.Status

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_settings
        )

        viewModel = ViewModelProvider.NewInstanceFactory().create(SettingsViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
    }

    override fun setToolbar() {
        super.setToolbar()
        supportActionBar?.title = getString(R.string.label_settings)
    }

    fun logout(view: View) {
        viewModel.logout().observe(this, Observer {
            it?.let { resource ->
                when (resource.status) {
                    Status.LOADING -> {
                        dialog = DialogManager.showLoading(this)
                    }
                    Status.ERROR -> {
                        dialog?.dismiss()
                        AccountManager.clear()
                        startActivity(SplashActivity.newIntent(this))
                    }
                    Status.SUCCESS -> {
                        dialog?.dismiss()
                        AccountManager.clear()
                        startActivity(SplashActivity.newIntent(this))
                    }
                }
            }
        })
    }
}