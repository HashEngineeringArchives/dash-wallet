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

package de.schildbach.wallet.ui.dashpay

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import de.schildbach.wallet.AppDatabase
import de.schildbach.wallet.WalletApplication
import de.schildbach.wallet.data.DashPayContactRequest
import de.schildbach.wallet.data.UsernameSearchResult
import de.schildbach.wallet.data.UsernameSortOrderBy
import de.schildbach.wallet.livedata.Resource
import de.schildbach.wallet.livedata.Status
import de.schildbach.wallet.ui.DashPayUserActivity
import de.schildbach.wallet.ui.GlobalFooterActivity
import de.schildbach.wallet.ui.SearchUserActivity
import de.schildbach.wallet_test.R
import kotlinx.android.synthetic.main.fragment_contacts.*


class ContactsActivity : GlobalFooterActivity(), TextWatcher,
        ContactSearchResultsAdapter.Listener,
        ContactSearchResultsAdapter.OnItemClickListener {

    companion object {
        private const val EXTRA_MODE = "extra_mode"

        const val MODE_SEARCH_CONTACTS = 0
        const val MODE_SELECT_CONTACT = 1
        const val MODE_VIEW_REQUESTS = 2

        @JvmStatic
        fun createIntent(context: Context, mode: Int): Intent {
            val intent = Intent(context, ContactsActivity::class.java)
            intent.putExtra(EXTRA_MODE, mode)
            return intent
        }
    }

    private lateinit var dashPayViewModel: DashPayViewModel
    private lateinit var walletApplication: WalletApplication
    private var handler: Handler = Handler()
    private lateinit var searchContactsRunnable: Runnable
    private val contactsAdapter: ContactSearchResultsAdapter = ContactSearchResultsAdapter(this)
    private var query = ""
    private var blockchainIdentityId: String? = null
    private var direction = UsernameSortOrderBy.USERNAME
    private var mode = MODE_SEARCH_CONTACTS
    private var currentPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        walletApplication = application as WalletApplication

        if (intent.extras != null && intent.extras!!.containsKey(EXTRA_MODE)) {
            mode = intent.extras.getInt(EXTRA_MODE)
        }

        if (mode == MODE_SEARCH_CONTACTS) {
            setContentViewWithFooter(R.layout.activity_contacts_root)
            activateContactsButton()
        } else {
            setContentView(R.layout.activity_contacts_root)
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        contacts_rv.layoutManager = LinearLayoutManager(this)
        contacts_rv.adapter = this.contactsAdapter
        this.contactsAdapter.itemClickListener = this

        initViewModel()

        if (mode == MODE_VIEW_REQUESTS) {
            search.visibility = View.GONE
            icon.visibility = View.GONE
            setTitle(R.string.contact_requests_title)
        } else {
            // search should be available for all other modes
            search.addTextChangedListener(this)
            search.visibility = View.VISIBLE
            icon.visibility = View.VISIBLE
            setTitle(R.string.contacts_title)
        }

        searchContacts()
    }

    private fun initViewModel() {
        dashPayViewModel = ViewModelProvider(this).get(DashPayViewModel::class.java)
        dashPayViewModel.searchContactsLiveData.observe(this, Observer {
            if (Status.SUCCESS == it.status) {
                if (it.data != null) {
                    processResults(it.data)
                }
            }
        })
        AppDatabase.getAppDatabase().blockchainIdentityDataDao().load().observe(this, Observer {
            if (it != null) {
                //TODO: we don't have an easy way of getting the identity id (userId)
                val tx = walletApplication.wallet.getTransaction(it.creditFundingTxId)
                val cftx = walletApplication.wallet.getCreditFundingTransaction(tx)
                blockchainIdentityId = cftx.creditBurnIdentityIdentifier.toStringBase58()
            }
        })

        dashPayViewModel.getContactRequestLiveData.observe(this, object : Observer<Resource<DashPayContactRequest>> {
            override fun onChanged(it: Resource<DashPayContactRequest>?) {
                if (it != null && currentPosition != -1) {
                    when (it.status) {
                        Status.LOADING -> {

                        }
                        Status.ERROR -> {
                            var msg = it.message
                            if (msg == null) {
                                msg = "!!Error!!  ${it.exception!!.message}"
                            }
                            Toast.makeText(this@ContactsActivity, msg, Toast.LENGTH_LONG).show()
                        }
                        Status.SUCCESS -> {
                            // update the data
                            contactsAdapter.results[currentPosition].usernameSearchResult!!.toContactRequest = it.data!!
                            when (mode) {
                                MODE_VIEW_REQUESTS -> {
                                    contactsAdapter.results.removeAt(currentPosition)
                                    contactsAdapter.notifyItemRemoved(currentPosition)
                                }
                                MODE_SEARCH_CONTACTS -> {
                                    // instead of removing the contact request and add a contact
                                    // just reload all the items
                                    searchContacts()
                                }
                                MODE_SELECT_CONTACT -> {
                                    // will there be contact requests in this mode?
                                }
                                else -> {
                                    throw IllegalStateException("invalid mode for ContactsActivity")
                                }
                            }

                            currentPosition = -1

                        }
                    }
                }
            }
        })
    }

    private fun processResults(data: List<UsernameSearchResult>) {

        val results = ArrayList<ContactSearchResultsAdapter.ViewItem>()
        // process the requests
        val requests = if (mode != MODE_SELECT_CONTACT)
            data.filter { r -> r.isPendingRequest }.toMutableList()
        else ArrayList()

        val requestCount = requests.size
        if (mode != MODE_VIEW_REQUESTS) {
            while (requests.size > 3) {
                requests.remove(requests[requests.size - 1])
            }
        }

        if (requests.isNotEmpty() && mode != MODE_VIEW_REQUESTS)
            results.add(ContactSearchResultsAdapter.ViewItem(null, ContactSearchResultsAdapter.CONTACT_REQUEST_HEADER, requestCount = requestCount))
        requests.forEach { r -> results.add(ContactSearchResultsAdapter.ViewItem(r, ContactSearchResultsAdapter.CONTACT_REQUEST)) }

        // process contacts
        val contacts = if (mode != MODE_VIEW_REQUESTS)
            data.filter { r -> r.requestSent && r.requestReceived }
        else ArrayList()

        if (contacts.isNotEmpty())
            results.add(ContactSearchResultsAdapter.ViewItem(null, ContactSearchResultsAdapter.CONTACT_HEADER))
        contacts.forEach { r -> results.add(ContactSearchResultsAdapter.ViewItem(r, ContactSearchResultsAdapter.CONTACT)) }

        contactsAdapter.results = results
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (mode == MODE_SEARCH_CONTACTS) {
            menuInflater.inflate(R.menu.contacts_menu, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSortOrderChanged(direction: UsernameSortOrderBy) {
        this.direction = direction
        searchContacts()
    }

    private fun searchContacts() {
        if (this::searchContactsRunnable.isInitialized) {
            handler.removeCallbacks(searchContactsRunnable)
        }

        searchContactsRunnable = Runnable {
            dashPayViewModel.searchContacts(query, direction)
        }
        handler.postDelayed(searchContactsRunnable, 500)
    }

    override fun afterTextChanged(s: Editable?) {
        s?.let {
            query = it.toString()
            searchContacts()
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun onItemClicked(view: View, usernameSearchResult: UsernameSearchResult) {
        startActivityForResult(DashPayUserActivity.createIntent(this,
                usernameSearchResult.username, usernameSearchResult.dashPayProfile, contactRequestSent = usernameSearchResult.requestSent,
                contactRequestReceived = usernameSearchResult.requestReceived), DashPayUserActivity.REQUEST_CODE_DEFAULT)
    }


    override fun onGotoClick() {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.contacts_add_contact -> {
                startActivity(Intent(this, SearchUserActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewAllRequests() {
        startActivity(createIntent(this, MODE_VIEW_REQUESTS))
    }

    override fun onAcceptRequest(usernameSearchResult: UsernameSearchResult, position: Int) {
        if (currentPosition == -1) {
            currentPosition = position
            dashPayViewModel.sendContactRequest(usernameSearchResult.fromContactRequest!!.userId)
        }
    }

    override fun onIgnoreRequest(usernameSearchResult: UsernameSearchResult, position: Int) {
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DashPayUserActivity.REQUEST_CODE_DEFAULT && resultCode == DashPayUserActivity.RESULT_CODE_CHANGED) {
            searchContacts()
        }
    }
}
