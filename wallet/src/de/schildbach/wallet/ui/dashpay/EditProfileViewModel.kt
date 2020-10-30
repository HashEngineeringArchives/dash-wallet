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
import android.os.Environment
import androidx.lifecycle.viewModelScope
import de.schildbach.wallet.Constants
import de.schildbach.wallet.data.DashPayProfile
import de.schildbach.wallet.ui.SingleLiveEvent
import de.schildbach.wallet.ui.dashpay.work.UpdateProfileOperation
import de.schildbach.wallet.ui.dashpay.work.UpdateProfileStatusLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel

class EditProfileViewModel(application: Application) : BaseProfileViewModel(application) {

    private val log = LoggerFactory.getLogger(EditProfileViewModel::class.java)

    val profilePictureFile by lazy {
        try {
            val storageDir: File = application.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            File(storageDir, Constants.Files.PROFILE_PICTURE_FILENAME)
        } catch (ex: IOException) {
            log.error(ex.message, ex)
            null
        }
    }

    val onTmpPictureReadyForEditEvent = SingleLiveEvent<File>()

    lateinit var tmpPictureFile: File

    fun createTmpPictureFile(): Boolean = try {
        val storageDir: File = walletApplication.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        tmpPictureFile = File.createTempFile("profileimagetmp", ".jpg", storageDir).apply {
            deleteOnExit()
        }
        true
    } catch (ex: IOException) {
        log.error(ex.message, ex)
        false
    }

    val updateProfileRequestState = UpdateProfileStatusLiveData(application)

    fun broadcastUpdateProfile(displayName: String, publicMessage: String, avatarUrl: String) {
        val dashPayProfile = dashPayProfileData.value!!
        val updatedProfile = DashPayProfile(dashPayProfile.userId, dashPayProfile.username,
                displayName, publicMessage, avatarUrl,
                dashPayProfile.createdAt, dashPayProfile.updatedAt)
        UpdateProfileOperation(walletApplication)
                .create(updatedProfile)
                .enqueue()
    }

    fun saveTmpAsProfilePicture() {
        copyFile(tmpPictureFile, profilePictureFile!!)
    }

    fun saveAsProfilePictureTmp(picturePath: String) {
        viewModelScope.launch() {
            copyFile(File(picturePath), tmpPictureFile)
            onTmpPictureReadyForEditEvent.call(tmpPictureFile)
        }
    }

    private fun copyFile(srcFile: File, outFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inStream = FileInputStream(srcFile)
                val outStream = FileOutputStream(outFile)
                val inChannel: FileChannel = inStream.channel
                val outChannel: FileChannel = outStream.channel
                inChannel.transferTo(0, inChannel.size(), outChannel)
                inStream.close()
                outStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}