package com.ktmb.pts.utilities

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import androidx.databinding.DataBindingUtil
import com.ktmb.pts.R
import com.ktmb.pts.databinding.ViewDialogConfirmationBinding

object DialogManager {

    fun showAlertDialog(
        context: Context,
        title: String, message: String,
        positiveMessage: String? = null,
        positiveAction: View.OnClickListener? = null
    ): Dialog {
        val dialog = Dialog(context, R.style.PrimaryDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val binding = DataBindingUtil.inflate<ViewDialogConfirmationBinding>(
            LayoutInflater.from(context),
            R.layout.view_dialog_confirmation, null, false
        )

        binding.tvTitle.text = title
        binding.tvMessage.text = message

        binding.btnPositive.text = positiveMessage ?: context.getString(R.string.label_okay)
        if (positiveAction != null) {
            binding.btnPositive.setOnClickListener(positiveAction)
        } else {
            binding.btnPositive.setOnClickListener {
                dialog.dismiss()
            }
        }

        binding.btnNegative.visibility = View.GONE

        dialog.setContentView(binding.root)
        dialog.show()

        return dialog
    }

    fun showAlertDialog(context: Context, title: String, message: String): Dialog {
        return showAlertDialog(context, title, message, null, null)
    }

    fun showErrorDialog(
        context: Context,
        message: String,
        title: String? = null,
        positiveMessage: String? = null,
        positiveAction: View.OnClickListener? = null
    ) : Dialog {
        return showAlertDialog(context, context.getString(R.string.error_default_title), message, null, null)
    }

    fun showConfirmationDialog(
        context: Context,
        title: String, message: String,
        positiveMessage: String? = null,
        positiveAction: View.OnClickListener,
        negativeMessage: String? = null,
        negativeAction: View.OnClickListener? = null
    ): Dialog {
        val dialog = Dialog(context, R.style.PrimaryDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val binding = DataBindingUtil.inflate<ViewDialogConfirmationBinding>(
            LayoutInflater.from(context),
            R.layout.view_dialog_confirmation, null, false
        )

        binding.tvTitle.text = title
        binding.tvMessage.text = message

        binding.btnPositive.text = positiveMessage ?: context.getString(R.string.label_yes)
        binding.btnPositive.setOnClickListener(positiveAction)

        binding.btnNegative.text = negativeMessage ?: context.getString(R.string.label_no)
        if (negativeAction == null) {
            binding.btnNegative.setOnClickListener {
                dialog.dismiss()
            }
        } else {
            binding.btnNegative.setOnClickListener(negativeAction)
        }

        dialog.setContentView(binding.root)
        dialog.show()

        return dialog

    }

}