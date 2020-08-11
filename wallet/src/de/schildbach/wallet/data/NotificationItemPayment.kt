package de.schildbach.wallet.data

import org.bitcoinj.core.Transaction

data class NotificationItemPayment(val tx: Transaction? = null, override val isNew: Boolean = false)
    : NotificationItem() {

    override fun getId() = tx!!.txId.toString()
    override fun getDate() = tx!!.updateTime.time * 1000
}

