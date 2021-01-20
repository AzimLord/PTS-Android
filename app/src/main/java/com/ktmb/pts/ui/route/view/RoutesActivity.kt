package com.ktmb.pts.ui.route.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ktmb.pts.R
import com.ktmb.pts.base.BaseActivity
import com.ktmb.pts.data.model.Route
import com.ktmb.pts.databinding.ActivityRoutesBinding
import com.ktmb.pts.ui.route.viewmodel.RouteViewModel
import com.ktmb.pts.utilities.DialogManager
import com.ktmb.pts.utilities.Status
import kotlinx.android.synthetic.main.view_toolbar.*
import kotlinx.android.synthetic.main.view_toolbar.view.*

class RoutesActivity : BaseActivity(), RouteAdapter.OnRouteClickListener {

    private lateinit var binding: ActivityRoutesBinding
    private lateinit var viewModel: RouteViewModel
    private lateinit var adapter: RouteAdapter

    companion object {
        const val EXTRA_ROUTE = "route"

        fun newIntent(context: Context): Intent {
            return Intent(context, RoutesActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_routes)
        viewModel = ViewModelProvider.NewInstanceFactory().create(RouteViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        adapter = RouteAdapter(this, this)
        binding.adapter = adapter

        getRoutes()
    }

    override fun setToolbar() {
        super.setToolbar()
        supportActionBar?.title = "Routes"
    }

    override fun onRouteClick(position: Int, route: Route) {
        val intent = Intent()
        intent.putExtra(EXTRA_ROUTE, route)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun getRoutes() {
        viewModel.getRoutes().observe(this, Observer {
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
                        resource.data?.let { routes ->
                            adapter.addRoutes(routes)
                        }
                    }
                }
            }
        })
    }

}