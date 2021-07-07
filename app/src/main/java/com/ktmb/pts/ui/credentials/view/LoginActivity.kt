package com.ktmb.pts.ui.credentials.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ktmb.pts.R
import com.ktmb.pts.base.BaseActivity
import com.ktmb.pts.databinding.ActivityLoginBinding
import com.ktmb.pts.ui.credentials.viewmodel.LoginViewModel
import com.ktmb.pts.ui.start.view.SplashActivity
import com.ktmb.pts.ui.start.viewmodel.SplashViewModel
import com.ktmb.pts.utilities.AccountManager
import com.ktmb.pts.utilities.DialogManager
import com.ktmb.pts.utilities.LogManager
import com.ktmb.pts.utilities.Status

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    companion object {
        private const val TAG = "LOGIN_ACTIVITY"

        fun newIntent(context: Context): Intent {
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_login
        )

        viewModel = ViewModelProvider.NewInstanceFactory().create(LoginViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
    }

    fun login(view: View) {
        val ktmbId = binding.etKtmbId.text.toString()
        val password = binding.etPassword.text.toString()

        viewModel.login(ktmbId, password).observe(this, Observer {
            it?.let { resource ->
                when (resource.status) {
                    Status.LOADING ->
                        dialog = DialogManager.showLoading(this)
                    Status.ERROR -> {
                        dialog?.dismiss()
                        DialogManager.showErrorDialog(this, it.message)
                    }
                    Status.SUCCESS -> {
                        dialog?.dismiss()
                        AccountManager.saveAuthToken(resource.data?.token!!)
                        AccountManager.saveUserInfo(resource.data)
                        startActivity(SplashActivity.newIntent(this))
                    }
                }
            }
        })
    }
}