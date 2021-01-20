package com.ktmb.pts.ui.route.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.ktmb.pts.R
import com.ktmb.pts.data.model.Route
import com.ktmb.pts.databinding.ItemRouteBinding

class RouteAdapter(private val context: Context, private val onRouteClickListener: OnRouteClickListener? = null): RecyclerView.Adapter<RouteAdapter.ViewHolder>() {

    private val routes = ArrayList<Route>()

    fun addRoutes(routes: ArrayList<Route>) {
        this.routes.addAll(routes)
        notifyDataSetChanged()
    }

    fun clear() {
        this.routes.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemRouteBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.item_route,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return routes.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(routes[position], position)
    }

    inner class ViewHolder(private val binding: ItemRouteBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(route: Route, position: Int) {
            binding.tvRouteCode.text = route.code
            binding.tvFrom.text = route.fromStation.name
            binding.tvTo.text = route.toStation.name

            onRouteClickListener?.let {
                binding.root.setOnClickListener { _ ->
                    it.onRouteClick(position, route)
                }
            }
        }

    }

    interface OnRouteClickListener {
        fun onRouteClick(position: Int, route: Route)
    }

}