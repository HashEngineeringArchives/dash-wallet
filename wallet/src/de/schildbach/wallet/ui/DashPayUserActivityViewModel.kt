/*
 * Copyright 2020 Dash Core Group.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.ui

import android.app.Application
import androidx.lifecycle.*
import de.schildbach.wallet.AppDatabase
import de.schildbach.wallet.WalletApplication
import de.schildbach.wallet.data.UsernameSearchResult
import de.schildbach.wallet.ui.dashpay.NotificationsForUserLiveData
import de.schildbach.wallet.ui.dashpay.PlatformRepo
import de.schildbach.wallet.ui.dashpay.work.SendContactRequestOperation
import kotlinx.coroutines.Dispatchers
import org.slf4j.LoggerFactory

class DashPayUserActivityViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val log = LoggerFactory.getLogger(DashPayUserActivityViewModel::class.java)
    }

    private val platformRepo = PlatformRepo.getInstance()
    private val walletApplication = application as WalletApplication

    lateinit var userData: UsernameSearchResult

    val userLiveData by lazy {
        liveData(Dispatchers.IO) {
            userData = platformRepo.getUser(userData.username).first()
            sendContactRequestState
            emit(userData)
        }
    }

    val sendContactRequestState by lazy {
        SendContactRequestOperation.operationStatus(application, userData.dashPayProfile.userId)
    }

    fun sendContactRequest() {
        SendContactRequestOperation(walletApplication)
                .create(userData.dashPayProfile.userId)
                .enqueue()
    }

    val notificationsForUser = NotificationsForUserLiveData(walletApplication, platformRepo, viewModelScope)
    fun initNotificationsForUser() {
        notificationsForUser.userId = userData.dashPayProfile.userId
    }
}