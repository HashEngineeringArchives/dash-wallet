package de.schildbach.wallet.ui.invite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.schildbach.wallet.data.*
import de.schildbach.wallet.ui.SingleLiveEvent
import java.util.ArrayList

class InvitesAdapter(private val itemClickListener: OnItemClickListener,
                     private val filterClick: SingleLiveEvent<InvitesHistoryViewModel.Filter>)
    : RecyclerView.Adapter<InvitesHistoryViewHolder>(),
        InvitesHeaderViewHolder.OnFilterListener {

    companion object {
        const val INVITE_HEADER = 1
        const val INVITE = 2
        const val EMPTY_HISTORY = 3
        const val INVITE_CREATE = 4
        //val headerId: Sha256Hash = Sha256Hash.of(Sha256Hash.ZERO_HASH.bytes)
        //val emptyId: Sha256Hash = Sha256Hash.of(headerId.bytes)
        //val createId: Sha256Hash = Sha256Hash.of(BigInteger.valueOf(INVITE_CREATE.toLong()).toByteArray())
        val headerInvite = InvitationItem(INVITE_HEADER)
        val emptyInvite = InvitationItem(EMPTY_HISTORY)
        val createInvite = InvitationItem(INVITE_CREATE)
    }

    private var filter = InvitesHistoryViewModel.Filter.ALL

    interface OnItemClickListener {
        fun onItemClicked(view: View, invitationItem: InvitationItem)
    }

    var history: List<InvitationItem> = arrayListOf()
        set(value) {
            field = value
            filteredResults.clear()
            filteredResults.add(createInvite)
            filteredResults.add(headerInvite)
            if (value.isNotEmpty()) {
                filteredResults.addAll(value)
            } else {
                filteredResults.add(emptyInvite)
            }
            notifyDataSetChanged()
        }
    var filteredResults: MutableList<InvitationItem> = arrayListOf()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitesHistoryViewHolder {
        return when (viewType) {
            INVITE_HEADER -> InvitesHeaderViewHolder(LayoutInflater.from(parent.context), this, parent)
            INVITE -> InviteViewHolder(LayoutInflater.from(parent.context), parent)
            EMPTY_HISTORY -> InviteEmptyViewHolder(LayoutInflater.from(parent.context), parent)
            INVITE_CREATE -> CreateInviteViewHolder(LayoutInflater.from(parent.context), parent)
            else -> throw IllegalArgumentException("Invalid viewType $viewType")
        }
    }

    override fun getItemCount(): Int {
        return filteredResults.size
    }

    fun getItem(position: Int): InvitationItem {
        return filteredResults[position]
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun onBindViewHolder(holder: InvitesHistoryViewHolder, position: Int) {
        val inviteItem = getItem(position)
        when (getItemViewType(position)) {
            INVITE_HEADER -> {
                (holder as InvitesHeaderViewHolder).bind(null, filter, filterClick)
            }
            INVITE -> {
                (holder as InviteViewHolder).bind(inviteItem.invitation, position)
            }
            EMPTY_HISTORY -> {
                (holder as InviteEmptyViewHolder).bind(filter)
            }
            INVITE_CREATE -> {
                (holder as CreateInviteViewHolder).bind()
            }
            else -> throw IllegalArgumentException("Invalid viewType ${getItemViewType(position)}")
        }
        holder.itemView.setOnClickListener {
            itemClickListener.run {
                onItemClicked(it, inviteItem)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = filteredResults[position]
        return item.type
    }

    override fun onFilter(filter: InvitesHistoryViewModel.Filter) {
        this.filter = filter
        filter()
    }

    private fun filter() {
        val resultTransactions: MutableList<InvitationItem> = ArrayList()
        //add header
        resultTransactions.add(createInvite)
        resultTransactions.add(headerInvite)
        for (inviteItem in history) {
            if (inviteItem.type == INVITE) {
                when (filter) {
                    InvitesHistoryViewModel.Filter.ALL -> resultTransactions.add(inviteItem)
                    InvitesHistoryViewModel.Filter.PENDING -> {
                        if (inviteItem.invitation!!.acceptedAt == 0L) {
                            resultTransactions.add(inviteItem)
                        }
                    }
                    InvitesHistoryViewModel.Filter.CLAIMED -> {
                        if (inviteItem.invitation!!.acceptedAt != 0L) {
                            resultTransactions.add(inviteItem)
                        }
                    }
                }
            }
        }
        filteredResults.clear()
        filteredResults.addAll(resultTransactions)
        if (filteredResults.size == 1) {
            filteredResults.add(emptyInvite)
        }

        notifyDataSetChanged()
    }
}