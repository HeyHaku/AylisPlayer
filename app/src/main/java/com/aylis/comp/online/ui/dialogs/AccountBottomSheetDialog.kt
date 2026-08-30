package com.aylis.comp.online.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import com.aylis.comp.online.managers.AuthManager
import com.aylis.comp.online.repository.UserAccount
import com.aylis.comp.online.ui.AuthDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AccountBottomSheetDialog : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_account_switcher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvAccounts = view.findViewById<RecyclerView>(R.id.rvAccounts)
        rvAccounts.layoutManager = LinearLayoutManager(context)
        val adapter = AccountAdapter()
        rvAccounts.adapter = adapter
        adapter.submitList(AuthManager.getAllAccounts())

        view.findViewById<View>(R.id.btnAddAccount).setOnClickListener {
            dismiss()
            AuthDialog(requireContext()) {
                // AuthManager will automatically handle state updates and flow emission
            }.show()
        }

        view.findViewById<View>(R.id.btnGuestMode).setOnClickListener {
            AuthManager.logout()
            dismiss()
        }
    }

    private inner class AccountAdapter : RecyclerView.Adapter<AccountAdapter.ViewHolder>() {
        private var items = listOf<UserAccount>()

        fun submitList(list: List<UserAccount>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val account = items[position]
            val isActive = AuthManager.getActiveAccount()?.id == account.id
            
            holder.tvName.text = account.name
            holder.tvEmail.text = account.email ?: ""
            holder.tvEmail.visibility = if (account.email.isNullOrEmpty()) View.GONE else View.VISIBLE
            
            if (account.name.isNotBlank()) {
                holder.tvAvatarInitial.text = account.name.first().uppercase()
                holder.tvAvatarInitial.visibility = View.VISIBLE
                holder.ivAvatarGuest.visibility = View.GONE
            } else {
                holder.tvAvatarInitial.visibility = View.GONE
                holder.ivAvatarGuest.visibility = View.VISIBLE
            }

            holder.ivActive.visibility = if (isActive) View.VISIBLE else View.GONE
            
            holder.itemView.setOnClickListener {
                if (!isActive) {
                    AuthManager.setActiveAccount(account.id)
                }
                dismiss()
            }
            
            holder.ivDelete.setOnClickListener {
                AuthManager.removeAccount(account.id)
                submitList(AuthManager.getAllAccounts())
                if (AuthManager.getAllAccounts().isEmpty()) {
                    dismiss()
                }
            }
        }

        override fun getItemCount() = items.size
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvAvatarInitial: TextView = view.findViewById(R.id.tvAvatarInitial)
            val ivAvatarGuest: ImageView = view.findViewById(R.id.ivAvatarGuest)
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvEmail: TextView = view.findViewById(R.id.tvEmail)
            val ivActive: ImageView = view.findViewById(R.id.ivActive)
            val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
        }
    }
}
