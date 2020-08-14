package de.schildbach.wallet.ui.dashpay

import androidx.lifecycle.LiveData
import de.schildbach.wallet.WalletApplication
import de.schildbach.wallet.data.NotificationItem
import de.schildbach.wallet.data.NotificationItemContact
import de.schildbach.wallet.data.UsernameSortOrderBy
import de.schildbach.wallet.livedata.Resource
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class NotificationsLiveData(protected val walletApplication: WalletApplication, protected val platformRepo: PlatformRepo) : LiveData<Resource<List<NotificationItem>>>(), OnContactsUpdated {
    private var listening = false
    protected var query = ""

    override fun onActive() {
        maybeAddEventListener()
        searchNotifications(query)
    }

    override fun onInactive() {
        maybeRemoveEventListener()
    }

    private fun maybeAddEventListener() {
        if (!listening && hasActiveObservers()) {
            platformRepo.addContactsUpdatedListener(this)
            listening = true
        }
    }

    private fun maybeRemoveEventListener() {
        if (listening) {
            platformRepo.removeContactsUpdatedListener(this)
            listening = false
        }
    }

    override fun onContactsUpdated() {
        searchNotifications(query)
    }

    open fun searchNotifications(text: String = "") {
        query = text
        GlobalScope.launch {
            val results = arrayListOf<NotificationItem>()
            val contactRequests = platformRepo.searchContacts(query, UsernameSortOrderBy.DATE_ADDED)

            //TODO: gather other notification types
            // * invitations
            // * payments are not included
            // * other

            contactRequests.data?.forEach {
                results.add(NotificationItemContact(it))
            }
            postValue(Resource.success(results))
        }
    }
}