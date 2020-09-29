package de.schildbach.wallet.ui.dashpay

import de.schildbach.wallet.WalletApplication
import de.schildbach.wallet.data.NotificationItem
import de.schildbach.wallet.data.NotificationItemContact
import de.schildbach.wallet.data.UsernameSortOrderBy
import de.schildbach.wallet.livedata.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class NotificationsLiveData(walletApplication: WalletApplication,
                                 platformRepo: PlatformRepo,
                                 protected val scope: CoroutineScope)
    : ContactsBasedLiveData<Resource<List<NotificationItem>>>(walletApplication, platformRepo) {

    var query = ""
        set(value) {
            field = value
            onContactsUpdated()
        }

    override fun onContactsUpdated() {
        scope.launch(Dispatchers.IO) {
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