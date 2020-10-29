package de.schildbach.wallet.ui.dashpay.work

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.schildbach.wallet.WalletApplication
import de.schildbach.wallet.data.DashPayProfile
import de.schildbach.wallet.ui.dashpay.PlatformRepo
import de.schildbach.wallet.ui.security.SecurityGuard
import kotlinx.coroutines.delay
import org.bitcoinj.crypto.KeyCrypterException
import org.bouncycastle.crypto.params.KeyParameter
import java.io.IOException
import java.security.GeneralSecurityException

class UpdateProfileWorker(context: Context, parameters: WorkerParameters)
    : BaseWorker(context, parameters) {

    companion object {
        const val KEY_PASSWORD = "UpdateProfileRequestWorker.PASSWORD"
        const val KEY_DISPLAY_NAME = "UpdateProfileRequestWorker.DISPLAY_NAME"
        const val KEY_PUBLIC_MESSAGE = "UpdateProfileRequestWorker.PUBLIC_MESSAGE"
        const val KEY_AVATAR_URL = "UpdateProfileRequestWorker.AVATAR_URL"
        const val KEY_USER_ID = "UpdateProfileRequestWorker.KEY_USER_ID"
        const val KEY_CREATED_AT = "UpdateProfileRequestWorker.CREATED_AT"
    }

    private val platformRepo = PlatformRepo.getInstance()

    override suspend fun doWorkWithBaseProgress(): Result {
        val displayName = inputData.getString(KEY_DISPLAY_NAME)?:""
        val publicMessage = inputData.getString(KEY_PUBLIC_MESSAGE)?:""
        val avatarUrl = inputData.getString(KEY_AVATAR_URL)?:""
        if (!inputData.keyValueMap.containsKey(KEY_CREATED_AT))
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "missing KEY_CREATED_AT parameter"))
        val createdAt = inputData.getLong(KEY_CREATED_AT, 0L)
        val blockchainIdentity = platformRepo.getBlockchainIdentity()!!
        val dashPayProfile = DashPayProfile(blockchainIdentity.uniqueIdString,
                blockchainIdentity.getUniqueUsername(),
                displayName,
                publicMessage,
                avatarUrl,
                createdAt
        )

        val encryptionKey: KeyParameter
        try {
            val password = SecurityGuard().retrievePassword()
            encryptionKey = WalletApplication.getInstance().wallet!!.keyCrypter!!.deriveKey(password)
        } catch (ex: KeyCrypterException) {
            val msg = formatExceptionMessage("derive encryption key", ex)
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to msg))
        } catch (ex: Exception) {
            when (ex) {
                is GeneralSecurityException,
                is IOException -> {
                    val msg = formatExceptionMessage("retrieve password", ex)
                    return Result.failure(workDataOf(KEY_ERROR_MESSAGE to msg))
                }
                else -> throw ex
            }
        }

        return try {
            val profileRequestResult = platformRepo.broadcastUpdatedProfile(dashPayProfile, encryptionKey)
            Result.success(workDataOf(
                    KEY_USER_ID to profileRequestResult.userId
            ))
        } catch (ex: Exception) {
            Result.failure(workDataOf(
                    KEY_ERROR_MESSAGE to formatExceptionMessage("create/update profile", ex)))
        }
    }
}