package com.ktmb.pts.utilities

import android.app.Dialog
import android.content.Context
import android.view.View
import com.ktmb.pts.R
import com.ktmb.pts.base.PTS
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

object ErrorManager {

    private var dialog: Dialog? = null

    fun showErrorDialog(
        context: Context,
        t: Throwable?,
        actionMessage: String? = null,
        action: View.OnClickListener? = null
    ): Dialog {
        val message = getErrorMessage(context, t)
        return DialogManager.showAlertDialog(
            context,
            title = context.getString(R.string.error_default_title),
            message = message,
            positiveMessage = actionMessage,
            positiveAction = action
        )
    }

    fun showErrorDialog(
        context: Context,
        response: Response<*>,
        actionMessage: String? = null,
        action: View.OnClickListener? = null
    ): Dialog {
        val message = context.getString(R.string.error_default_message)

        return when (response.code()) {
            401 -> {
                dialog = DialogManager.showAlertDialog(context,
                    title = context.getString(R.string.error_session_expires_title),
                    message = context.getString(R.string.error_session_expires_message),
                    positiveMessage = actionMessage,
                    positiveAction = View.OnClickListener {
                        dialog?.dismiss()
                        //doAsync {
                        //FirebaseInstanceId.getInstance().deleteInstanceId()
                        //}
                        //AccountManager.clear()
                        //context.startActivity(LandingActivity.newIntent(context))
                    })
                dialog!!
            }
            else -> {
                DialogManager.showAlertDialog(
                    context,
                    title = context.getString(R.string.error_default_title),
                    message = getErrorMessage(context, response),
                    positiveAction = action
                )
            }
        }
    }

    @Deprecated("Got new and improve function", ReplaceWith("getErrorMessage(t)"))
    fun getErrorMessage(context: Context, t: Throwable?): String {
        return if (t is IOException) {
            context.getString(R.string.error_connection_message)
        } else {
            context.getString(R.string.error_default_message)
        }
    }

    fun getErrorMessage(t: Throwable?): String {
        return if (t is IOException) {
            PTS.instance.getString(R.string.error_connection_message)
        } else {
            PTS.instance.getString(R.string.error_default_message)
        }
    }

    @Deprecated("Got new and improve function", ReplaceWith("getErrorMessage(response)"))
    fun getErrorMessage(context: Context, response: Response<*>): String {
        try {
            val errorBody = JSONObject(response.errorBody()!!.string())
            var message = ""
            when {
                errorBody.has("error") -> {
                    message = errorBody.optString(
                        "error",
                        context.getString(R.string.error_default_message)
                    )
                }
                errorBody.has("message") -> {
                    message = errorBody.optString(
                        "message",
                        context.getString(R.string.error_default_message)
                    )
                }
                errorBody.has("errors") -> {
                    val errors = errorBody.getJSONObject("errors")
                    val iterator = errors.keys()
                    while (iterator.hasNext()) {
                        val key = iterator.next()
                        val objectArray = errors.getJSONArray(key)
                        for (i in 0 until objectArray.length()) {
                            message += if (i != objectArray.length() - 1) {
                                objectArray.getString(i) + "\n"
                            } else {
                                objectArray.getString(i)
                            }
                        }
                    }
                }
                else -> {
                    message = context.getString(R.string.error_default_message)
                }
            }
            return message
        } catch (e: JSONException) {
            return context.getString(R.string.error_default_message)
        }
    }

    fun getErrorMessage(response: Response<*>): String {
        try {
            val errorBody = JSONObject(response.errorBody()!!.string())
            var message = ""
            when {
                errorBody.has("error") -> {
                    message = errorBody.optString(
                        "error",
                        PTS.instance.getString(R.string.error_default_message)
                    )
                }
                errorBody.has("message") -> {
                    message = errorBody.optString(
                        "message",
                        PTS.instance.getString(R.string.error_default_message)
                    )
                }
                errorBody.has("errors") -> {
                    val errors = errorBody.getJSONObject("errors")
                    val iterator = errors.keys()
                    while (iterator.hasNext()) {
                        val key = iterator.next()
                        val objectArray = errors.getJSONArray(key)
                        for (i in 0 until objectArray.length()) {
                            message += if (i != objectArray.length() - 1) {
                                objectArray.getString(i) + "\n"
                            } else {
                                objectArray.getString(i)
                            }
                        }
                    }
                }
                else -> {
                    message = PTS.instance.getString(R.string.error_default_message)
                }
            }
            return message
        } catch (e: JSONException) {
            return PTS.instance.getString(R.string.error_default_message)
        }
    }
}