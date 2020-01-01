/*
 * Copyright 2019 Dash Core Group
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

package de.schildbach.wallet.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Switch
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import de.schildbach.wallet.WalletApplication
import de.schildbach.wallet_test.R
import org.bitcoinj.wallet.Wallet


class SecurityActivity : BaseMenuActivity(), AbstractPINDialogFragment.WalletProvider {

    companion object {
        private const val AUTH_REQUEST_CODE_BACKUP = 1
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_security
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.security_title)
        val hideBalanceOnLaunch = findViewById<Switch>(R.id.hide_balance_switch)
        hideBalanceOnLaunch.isChecked = configuration.hideBalance
        hideBalanceOnLaunch.setOnCheckedChangeListener { _, hideBalanceOnLaunch ->
            configuration.hideBalance = hideBalanceOnLaunch
        }

        val checkPinSharedModel: CheckPinSharedModel = ViewModelProviders.of(this).get(CheckPinSharedModel::class.java)
        checkPinSharedModel.onCorrectPinCallback.observe(this, Observer<Pair<Int?, String?>> { (data, _) ->
            if (data == AUTH_REQUEST_CODE_BACKUP) {
                val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                    BackupWalletDialogFragment.show(supportFragmentManager)
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
                }
            }
        })
    }

    fun backupWallet(view: View) {
        CheckPinDialog.show(this, AUTH_REQUEST_CODE_BACKUP)
    }

    fun viewRecoveryPhrase(view: View) {
        BackupWalletToSeedDialogFragment.show(supportFragmentManager)
    }

    fun changePin(view: View) {
        startActivity(SetPinActivity.createIntent(this, R.string.wallet_options_encrypt_keys_change, true))
    }

    fun openAdvancedSecurity(view: View) {
        startActivity(Intent(this, AdvancedSecurityActivity::class.java))
    }

    fun resetWallet(view: View) {
        ResetWalletDialog.newInstance().show(supportFragmentManager, "reset_wallet_dialog")
    }

    // required by UnlockWalletDialogFragment
    override fun onWalletUpgradeComplete(password: String?) {

    }

    override fun getWallet(): Wallet {
        return WalletApplication.getInstance().wallet
    }
}
