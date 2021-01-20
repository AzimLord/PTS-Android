package com.ktmb.pts.ui.report.view

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ktmb.pts.R
import com.ktmb.pts.data.model.ReportType
import com.ktmb.pts.databinding.ItemReportTypeBinding
import com.ktmb.pts.ui.report.viewmodel.ReportTypeViewModel

class ReportTypeAdapter(private val activity: Activity, private val onClick: OnReportTypeClickListener? = null): RecyclerView.Adapter<ReportTypeAdapter.ViewHolder>() {

    private val reportTypes = ArrayList<ReportType>()

    fun addReportTypes(reportTypes: ArrayList<ReportType>) {
        this.reportTypes.addAll(reportTypes)
        notifyDataSetChanged()
    }

    fun clear() {
        this.reportTypes.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportTypeAdapter.ViewHolder {
        val binding: ItemReportTypeBinding = DataBindingUtil.inflate(
            LayoutInflater.from(activity),
            R.layout.item_report_type,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return reportTypes.size
    }

    override fun onBindViewHolder(holder: ReportTypeAdapter.ViewHolder, position: Int) {
        holder.bind(reportTypes[position], position)
    }

    inner class ViewHolder(private val binding: ItemReportTypeBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(reportType: ReportType, position: Int) {
            binding.viewModel = ReportTypeViewModel(reportType)

            Glide.with(activity).load(reportType.imageUrl).into(binding.ivReport)

            onClick?.let {
                binding.root.setOnClickListener { _ ->
                    it.onReportTypeClick(position, reportType)
                }
            }
        }

    }

    interface OnReportTypeClickListener {
        fun onReportTypeClick(position: Int, reportType: ReportType)
    }

}