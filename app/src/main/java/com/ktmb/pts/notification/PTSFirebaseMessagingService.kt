package com.ktmb.pts.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ktmb.pts.data.model.Report
import com.ktmb.pts.data.repository.AccountRepo
import com.ktmb.pts.data.request.NotificationTokenRequest
import com.ktmb.pts.utilities.AccountManager
import com.ktmb.pts.utilities.LogManager
import com.ktmb.pts.utilities.NavigationManager

class PTSFirebaseMessagingService: FirebaseMessagingService() {

    private val accountRepo = AccountRepo()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val type = remoteMessage.data["type"]
        val body = remoteMessage.data["body"]

        LogManager.log(remoteMessage.data.toString())

        when (type) {
            NotificationType.REPORT.name -> {
                val report = Gson().fromJson<Report>(body, Report::class.java)
                NavigationManager.newReport(report)
            }
            NotificationType.REPORTS.name -> {
                val type = object : TypeToken<List<Report>>() {}.type
                val reports = ArrayList(Gson().fromJson<List<Report>>(body, type))
                NavigationManager.newReports(reports)
            }
            NotificationType.REPORT_DELETED.name -> {
                val report = Gson().fromJson<Report>(body, Report::class.java)
                NavigationManager.deleteReport(report)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        if (!AccountManager.isFirebaseTokenAlreadyStored(token)) {
            val request = NotificationTokenRequest(token)
            accountRepo.saveNotificationTokenCall(request).execute()
        }
    }
}