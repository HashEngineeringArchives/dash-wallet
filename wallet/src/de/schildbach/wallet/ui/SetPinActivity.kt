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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.ViewSwitcher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import de.schildbach.wallet.WalletApplication
import de.schildbach.wallet.livedata.Status
import de.schildbach.wallet.ui.preference.PinRetryController
import de.schildbach.wallet.ui.widget.NumericKeyboardView
import de.schildbach.wallet.ui.widget.PinPreviewView
import de.schildbach.wallet_test.R

private const val FINGERPRINT_REQUEST_SEED = 1
private const val FINGERPRINT_REQUEST_WALLET = 2

class SetPinActivity : SessionActivity() {

    private lateinit var numericKeyboardView: NumericKeyboardView
    private lateinit var confirmButtonView: View
    private lateinit var viewModel: SetPinViewModel
    private lateinit var checkPinSharedModel: CheckPinSharedModel
    private lateinit var pinProgressSwitcherView: ViewSwitcher
    private lateinit var pinPreviewView: PinPreviewView
    private lateinit var pageTitleView: TextView
    private lateinit var pageMessageView: TextView

    private lateinit var pinRetryController: PinRetryController

    val pin = arrayListOf<Int>()
    var seed = listOf<String>()

    private var changePin = false

    private enum class State {
        DECRYPT,
        DECRYPTING,
        SET_PIN,
        CHANGE_PIN,
        CONFIRM_PIN,
        INVALID_PIN,
        ENCRYPTING,
        LOCKED
    }

    private var state = State.SET_PIN

    companion object {

        private const val EXTRA_TITLE_RES_ID = "extra_title_res_id"
        private const val EXTRA_PASSWORD = "extra_password"
        private const val CHANGE_PIN = "change_pin"

        @JvmOverloads
        @JvmStatic
        fun createIntent(context: Context, titleResId: Int,
                         changePin: Boolean = false, password: String? = null): Intent {
            val intent = Intent(context, SetPinActivity::class.java)
            intent.putExtra(EXTRA_TITLE_RES_ID, titleResId)
            intent.putExtra(CHANGE_PIN, changePin)
            intent.putExtra(EXTRA_PASSWORD, password)
            return intent
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_pin)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        setTitle(intent.getIntExtra(EXTRA_TITLE_RES_ID, R.string.set_pin_create_new_wallet))

        initView()
        initViewModel()

        pinRetryController = PinRetryController.getInstance()

        val walletApplication = application as WalletApplication
        if (walletApplication.wallet.isEncrypted) {
            val password = intent.getStringExtra(EXTRA_PASSWORD)
            changePin = intent.getBooleanExtra(CHANGE_PIN, false)
            if (password != null) {
                viewModel.decryptKeys(password)
            } else {
                if (changePin) {
                    if (pinRetryController.isLocked) {
                        setState(State.LOCKED)
                    } else {
                        setState(State.CHANGE_PIN)
                    }
                } else {
                    setState(State.DECRYPT)
                }
            }
        } else {
            seed = walletApplication.wallet.keyChainSeed.mnemonicCode!!
        }
    }

    private fun initView() {
        pinProgressSwitcherView = findViewById(R.id.pin_progress_switcher)
        pinPreviewView = findViewById(R.id.pin_preview)
        pageTitleView = findViewById(R.id.page_title)
        pageMessageView = findViewById(R.id.message)
        confirmButtonView = findViewById(R.id.btn_confirm)
        numericKeyboardView = findViewById(R.id.numeric_keyboard)

        pinPreviewView.setTextColor(R.color.dash_light_gray)
        pinPreviewView.hideForgotPinAction()

        numericKeyboardView.setFunctionEnabled(false)
        numericKeyboardView.onKeyboardActionListener = object : NumericKeyboardView.OnKeyboardActionListener {

            override fun onNumber(number: Int) {
                if (pinRetryController.isLocked) {
                    return
                }

                if (pin.size < 4 || state == State.DECRYPT) {
                    pin.add(number)
                    pinPreviewView.next()
                }

                if (state == State.DECRYPT) {
                    if (pin.size == viewModel.pin.size || (state == State.CONFIRM_PIN && pin.size > viewModel.pin.size)) {
                        nextStep()
                    }
                } else {
                    if (pin.size == 4) {
                        nextStep()
                    }
                }
            }

            override fun onBack(longClick: Boolean) {
                if (pin.size > 0) {
                    pin.removeAt(pin.lastIndex)
                    pinPreviewView.prev()
                }
            }

            override fun onFunction() {

            }
        }
        confirmButtonView.setOnClickListener {
            if (pin.size == 0) {
                pinPreviewView.shake()
            } else {
                nextStep()
            }
        }
    }

    private fun nextStep() {
        if (state == State.CONFIRM_PIN) {
            if (pin == viewModel.pin) {
                Handler().postDelayed({
                    viewModel.encryptKeys(changePin)
                }, 200)
            } else {
                pinPreviewView.shake()
                setState(State.CONFIRM_PIN)
            }
        } else {
            viewModel.setPin(pin)
            if (state == State.DECRYPT || state == State.CHANGE_PIN || state == State.INVALID_PIN) {
                viewModel.decryptKeys()
            } else {
                setState(State.CONFIRM_PIN)
            }
        }
    }

    private fun setState(newState: State) {
        when (newState) {
            State.DECRYPT -> {
                pinPreviewView.mode = PinPreviewView.PinType.EXTENDED
                pageTitleView.setText(R.string.set_pin_enter_pin)
                if (pinProgressSwitcherView.currentView.id == R.id.progress) {
                    pinProgressSwitcherView.showPrevious()
                }
                pageMessageView.visibility = View.GONE
                numericKeyboardView.visibility = View.VISIBLE
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                confirmButtonView.visibility = View.VISIBLE
                viewModel.pin.clear()
                pin.clear()
            }
            State.CHANGE_PIN, State.INVALID_PIN -> {
                pinPreviewView.mode = PinPreviewView.PinType.STANDARD
                pageTitleView.setText(R.string.set_pin_enter_pin)
                if (pinProgressSwitcherView.currentView.id == R.id.progress) {
                    pinProgressSwitcherView.showPrevious()
                }
                pageMessageView.visibility = View.GONE
                numericKeyboardView.visibility = View.VISIBLE
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                confirmButtonView.visibility = View.GONE
                viewModel.pin.clear()
                pin.clear()
                if (newState == State.INVALID_PIN) {
                    pinPreviewView.shake()
                    pinPreviewView.badPin(pinRetryController.getRemainingAttemptsMessage(this))
                }
            }
            State.SET_PIN -> {
                pinPreviewView.mode = PinPreviewView.PinType.STANDARD
                pageTitleView.setText(R.string.set_pin_set_pin)
                if (pinProgressSwitcherView.currentView.id == R.id.progress) {
                    pinProgressSwitcherView.showPrevious()
                }
                pageMessageView.visibility = View.VISIBLE
                numericKeyboardView.visibility = View.VISIBLE
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                confirmButtonView.visibility = View.GONE
                pinPreviewView.clear()
                viewModel.pin.clear()
                pin.clear()
            }
            State.CONFIRM_PIN -> {
                if (pinProgressSwitcherView.currentView.id == R.id.progress) {
                    pinProgressSwitcherView.showPrevious()
                }
                pageMessageView.visibility = View.VISIBLE
                numericKeyboardView.visibility = View.VISIBLE
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                confirmButtonView.visibility = View.GONE
                Handler().postDelayed({
                    pinPreviewView.clear()
                    pageTitleView.setText(R.string.set_pin_confirm_pin)
                }, 200)
                pin.clear()
            }
            State.ENCRYPTING -> {
                pageTitleView.setText(R.string.set_pin_encrypting)
                if (pinProgressSwitcherView.currentView.id == R.id.pin_preview) {
                    pinProgressSwitcherView.showNext()
                }
                pageMessageView.visibility = View.INVISIBLE
                numericKeyboardView.visibility = View.INVISIBLE
                supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                confirmButtonView.visibility = View.GONE
            }
            State.DECRYPTING -> {
                pageTitleView.setText(R.string.set_pin_decrypting)
                if (pinProgressSwitcherView.currentView.id == R.id.pin_preview) {
                    pinProgressSwitcherView.showNext()
                }
                pageMessageView.visibility = View.INVISIBLE
                numericKeyboardView.visibility = View.INVISIBLE
                supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                confirmButtonView.visibility = View.GONE
            }
            State.LOCKED -> {
                viewModel.pin.clear()
                pin.clear()
                pinPreviewView.clear()
                pageTitleView.setText(R.string.wallet_lock_wallet_disabled)
                pageMessageView.text = pinRetryController.getWalletTemporaryLockedMessage(this)
                pageMessageView.visibility = View.VISIBLE
                pinProgressSwitcherView.visibility = View.GONE
                numericKeyboardView.visibility = View.INVISIBLE
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            }
        }
        state = newState
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(SetPinViewModel::class.java)
        viewModel.encryptWalletLiveData.observe(this, Observer {
            when (it.status) {
                Status.ERROR -> {
                    pinRetryController.failedAttempt(viewModel.getPinAsString())
                    if (pinRetryController.isLocked) {
                        setState(State.LOCKED)
                    } else {
                        if (state == State.DECRYPTING) {
                            setState(if (changePin) State.INVALID_PIN else State.DECRYPT)
                        } else {
                            android.widget.Toast.makeText(this, "Encrypting error", android.widget.Toast.LENGTH_LONG).show()
                            setState(State.CONFIRM_PIN)
                        }
                    }
                }
                Status.LOADING -> {
                    setState(if (state == State.CONFIRM_PIN) State.ENCRYPTING else State.DECRYPTING)
                }
                Status.SUCCESS -> {
                    if (state == State.DECRYPTING) {
                        val walletApplication = application as WalletApplication
                        seed = walletApplication.wallet.keyChainSeed.mnemonicCode!!
                        setState(State.SET_PIN)
                    } else {
                        if (changePin) {
                            finish()
                        } else {
                            viewModel.initWallet()
                        }
                    }
                }
            }
        })
        viewModel.startNextActivity.observe(this, Observer {

            val requestCode = if (it) FINGERPRINT_REQUEST_SEED else FINGERPRINT_REQUEST_WALLET
            if (EnableFingerprintDialog.shouldBeShown(this@SetPinActivity)) {
                EnableFingerprintDialog.show(viewModel.getPinAsString(), requestCode, supportFragmentManager)
            } else {
                performNextStep(requestCode)
            }
        })
        checkPinSharedModel = ViewModelProviders.of(this).get(CheckPinSharedModel::class.java)
        checkPinSharedModel.onCorrectPinCallback.observe(this, Observer {
            performNextStep(it.first)
        })
    }

    private fun startActivityNewTask(intent: Intent) {
        intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        when {
            pin.size > 0 -> setState(state)
            state == State.CONFIRM_PIN -> setState(State.SET_PIN)
            else -> if (state != State.ENCRYPTING && state != State.DECRYPTING) {
                finish()
            }
        }
    }

    private fun performNextStep(requestCode: Int) {
        when (requestCode) {
            FINGERPRINT_REQUEST_SEED -> startVerifySeedActivity()
            FINGERPRINT_REQUEST_WALLET -> goHome()
        }
        (application as WalletApplication).maybeStartAutoLogoutTimer()
    }

    private fun startVerifySeedActivity() {
        val intent = VerifySeedActivity.createIntent(this, seed.toTypedArray())
        startActivityNewTask(intent)
    }

    private fun goHome() {
        val intent = Intent(this, WalletActivity::class.java)
        startActivityNewTask(intent)
    }
}
