/*
 * Copyright 2020 Dash Core Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schildbach.wallet.ui.dashpay

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import de.schildbach.wallet.AppDatabase
import de.schildbach.wallet.WalletApplication
import de.schildbach.wallet.data.BlockchainIdentityData
import de.schildbach.wallet.data.DashPayProfile
import de.schildbach.wallet.ui.dashpay.work.UpdateProfileOperation

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val walletApplication = application as WalletApplication

    // Use the database instead of PlatformRepo.getBlockchainIdentity, which
    // won't be initialized if there is no username registered
    val blockchainIdentityData = AppDatabase.getAppDatabase()
            .blockchainIdentityDataDaoAsync().load()

    val blockchainIdentity: BlockchainIdentityData?
        get() = blockchainIdentityData.value

    // this must be observed after blockchainIdentityData is observed
    private lateinit var _dashPayProfileData: LiveData<DashPayProfile?>
    val dashPayProfileData: LiveData<DashPayProfile?>
        get() {
            if (!this::_dashPayProfileData.isInitialized) {
                _dashPayProfileData = AppDatabase.getAppDatabase()
                        .dashPayProfileDaoAsync()
                        .loadByUserIdDistinct(blockchainIdentity!!.userId!!)
            }
            return _dashPayProfileData
        }

    val updateProfileRequestState = UpdateProfileOperation.operationStatus(application)

    fun broadcastUpdateProfile(dashPayProfile: DashPayProfile) {
        UpdateProfileOperation(walletApplication)
                .create(dashPayProfile)
                .enqueue()
    }

    // TODO: Should this be a live data?
    var localProfileImageUri: String
        get() = walletApplication.configuration.localProfilePictureUri
        set(value) {
            walletApplication.configuration.localProfilePictureUri = value
        }


}