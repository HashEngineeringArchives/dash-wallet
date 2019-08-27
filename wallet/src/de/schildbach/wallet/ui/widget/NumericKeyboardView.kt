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

package de.schildbach.wallet.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TableLayout
import de.schildbach.wallet_test.R

class NumericKeyboardView(context: Context, attrs: AttributeSet) : TableLayout(context, attrs), View.OnClickListener {

    companion object {
        private val NUMERIC_BUTTONS_RES_ID = arrayOf(
                R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6,
                R.id.btn_7, R.id.btn_8, R.id.btn_9,
                R.id.btn_0)

        private val FUNCTION_BUTTONS_RES_ID = arrayOf(
                R.id.btn_cancel, R.id.btn_back
        )

        private val ALL_BUTTONS_RES_ID = NUMERIC_BUTTONS_RES_ID + FUNCTION_BUTTONS_RES_ID
    }

    init {
        inflate(context, R.layout.numeric_keyboard_view, this)
        for (btnResId in ALL_BUTTONS_RES_ID) {
            findViewById<View>(btnResId).setOnClickListener(this)
        }
    }

    var onKeyboardActionListener: OnKeyboardActionListener? = null

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_cancel -> onCancelClick()
            R.id.btn_back -> onBackClick()
            else -> onNumberClick((v.tag as String).toInt())
        }
    }

    fun setCancelEnabled(enabled: Boolean) {
        findViewById<View>(R.id.btn_cancel).isEnabled = enabled
    }

    private fun onCancelClick() {
        onKeyboardActionListener?.onCancel()
    }

    private fun onBackClick() {
        onKeyboardActionListener?.onBack()
    }

    private fun onNumberClick(number: Int) {
        onKeyboardActionListener?.onNumber(number)
    }

    interface OnKeyboardActionListener {
        fun onNumber(number: Int)
        fun onBack()
        fun onCancel()
    }
}
