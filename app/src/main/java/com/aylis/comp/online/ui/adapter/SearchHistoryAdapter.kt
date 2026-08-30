package com.aylis.comp.online.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R

class SearchHistoryAdapter(
    private var historyList: List<String>,
    private val onItemClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {

    fun submitList(newList: List<String>) {
        historyList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]
        holder.tvQuery.text = item

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount(): Int = historyList.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuery: TextView = view.findViewById(R.id.tvHistoryQuery)
        val btnDelete: ImageView = view.findViewById(R.id.btnHistoryDelete)
    }
}
