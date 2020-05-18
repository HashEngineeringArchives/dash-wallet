package de.schildbach.wallet.ui.dashpay

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import de.schildbach.wallet.AppDatabase
import de.schildbach.wallet.Constants
import de.schildbach.wallet.data.IdentityCreationState
import de.schildbach.wallet_test.R


class CreateIdentityNotification(val service: LifecycleService) {

    private val notificationManager by lazy { service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    fun startServiceForeground() {
        service.startForeground(
                Constants.NOTIFICATION_ID_DASHPAY_CREATE_IDENTITY,
                initialNotification())
        startObservingIdentityCreationState()
    }

    private fun initialNotification(): Notification {
        return createNotification(R.string.processing_home_title, 0, -1)
    }

    private fun displayStep1() {
        updateNotification(R.string.processing_home_title, R.string.processing_home_step_1, 4, 1)
    }

    private fun displayStep2() {
        updateNotification(R.string.processing_home_title, R.string.processing_home_step_2, 4, 2)
    }

    private fun displayStep3() {
        updateNotification(R.string.processing_home_title, R.string.processing_home_step_3, 4, 3)
    }

    private fun displayDone() {
        updateNotification(R.string.processing_done_subtitle, 0, 0, 0)
    }

    private fun displayError() {
        updateNotification(R.string.processing_home_title, R.string.processing_error_title, 0, 0)
    }

    private fun updateNotification(@StringRes titleResId: Int, @StringRes messageResId: Int, progressMax: Int, progress: Int) {
        val notification: Notification = createNotification(titleResId, messageResId, progressMax, progress)
        notificationManager.notify(Constants.NOTIFICATION_ID_DASHPAY_CREATE_IDENTITY, notification)
    }

    private fun createNotification(@StringRes titleResId: Int, @StringRes messageResId: Int = 0, progressMax: Int = 0, progress: Int = 0): Notification {
        return NotificationCompat.Builder(service, Constants.NOTIFICATION_CHANNEL_ID_DASHPAY).run {
            setSmallIcon(R.drawable.ic_dash_d_white_bottom)
            setContentTitle(service.getString(titleResId))
            if (messageResId != 0) {
                setContentText(service.getString(messageResId))
            }
            when {
                (progressMax == -1) -> setProgress(100, 0, true)
                (progressMax > 0) -> setProgress(progressMax, progress, false)
                else -> setProgress(0, 0, false)
            }
            build()
        }
    }

    private fun startObservingIdentityCreationState() = AppDatabase.getAppDatabase()
            .identityCreationStateDao().load().observe(service, Observer {
                if (it != null && it.error) {
                    displayError()
                } else when (it?.state) {
                    IdentityCreationState.State.UPGRADING_WALLET,
                    IdentityCreationState.State.CREDIT_FUNDING_TX_CREATING,
                    IdentityCreationState.State.CREDIT_FUNDING_TX_SENDING,
                    IdentityCreationState.State.CREDIT_FUNDING_TX_SENT,
                    IdentityCreationState.State.CREDIT_FUNDING_TX_CONFIRMED -> {
                        displayStep1()
                    }
                    IdentityCreationState.State.IDENTITY_REGISTERING,
                    IdentityCreationState.State.IDENTITY_REGISTERED -> {
                        displayStep2()
                    }
                    IdentityCreationState.State.PREORDER_REGISTERING,
                    IdentityCreationState.State.PREORDER_REGISTERED,
                    IdentityCreationState.State.USERNAME_REGISTERING -> {
                        displayStep3()
                    }
                    IdentityCreationState.State.USERNAME_REGISTERED -> {
                        displayDone()
                    }
                }
            })
}
