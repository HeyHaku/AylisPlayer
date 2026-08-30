package com.aylis.comp.online.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import com.aylis.comp.online.repository.OnlineItem
import com.aylis.comp.online.repository.OnlineTrack
import com.aylis.comp.online.repository.Shelf

class ShelfAdapter(
    private val context: android.content.Context,
    private val onItemClick: (OnlineItem, List<OnlineItem>, String) -> Unit,
    private val onShelfTitleClick: ((Shelf) -> Unit)? = null
) : ListAdapter<Shelf, RecyclerView.ViewHolder>(ShelfDiffCallback()) {
    private val tracksPool = RecyclerView.RecycledViewPool()
    private val cardsPool = RecyclerView.RecycledViewPool()
    private val endlessPool = RecyclerView.RecycledViewPool()

    init {
        tracksPool.setMaxRecycledViews(0, 25)
        cardsPool.setMaxRecycledViews(0, 25)
        cardsPool.setMaxRecycledViews(1, 25)
        endlessPool.setMaxRecycledViews(0, 40)
    }

    override fun getItemViewType(position: Int): Int {
        val shelf = getItem(position)
        val isTracks = shelf.items.isNotEmpty() && shelf.items.all { it is OnlineTrack }
        if (shelf.title == context.getString(R.string.online_infinite_recommendations)) return 3
        if (isTracks) return 1
        if (shelf.isImmersive) return 2
        return 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 3) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_endless_recommendations, parent, false)
            return EndlessViewHolder(view, endlessPool)
        }
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shelf, parent, false)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerShelfItems)
        val density = parent.context.resources.displayMetrics.density
        val lp = recycler.layoutParams
        if (viewType == 1) {
            lp.height = (290 * density).toInt()
        } else {
            lp.height = (210 * density).toInt()
        }
        recycler.layoutParams = lp
        return ShelfViewHolder(view, tracksPool, cardsPool, onShelfTitleClick)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val shelf = getItem(position) ?: return
        
        if (holder is ShelfViewHolder) {
            holder.bind(shelf, onItemClick)
        } else if (holder is EndlessViewHolder) {
            holder.bind(shelf, onItemClick)
        }
    }

    class ShelfViewHolder(
        itemView: View,
        private val tracksPool: RecyclerView.RecycledViewPool,
        private val cardsPool: RecyclerView.RecycledViewPool,
        private val onTitleClick: ((Shelf) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val layoutShelfHeader: View = itemView.findViewById(R.id.layoutShelfHeader)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvShelfTitle)
        val recyclerItems: RecyclerView = itemView.findViewById(R.id.recyclerShelfItems)

        private var currentIsTracks: Boolean? = null
        private var currentShelf: Shelf? = null
        private var innerAdapter: RecyclerView.Adapter<*>? = null

        fun bind(shelf: Shelf, onItemClick: (OnlineItem, List<OnlineItem>, String) -> Unit) {
            currentShelf = shelf
            val ctx = itemView.context
            val localizedTitle = when {
                shelf.title.equals("Quick picks", ignoreCase = true) || shelf.title.equals("Быстрые выборы", ignoreCase = true) || shelf.title.equals("Быстрые", ignoreCase = true) || shelf.title.equals("Быстрый выбор", ignoreCase = true) || shelf.title.equals("Начать радио", ignoreCase = true) || shelf.title.equals("Start radio", ignoreCase = true) || shelf.title.equals("Радио", ignoreCase = true) -> ctx.getString(R.string.online_quick_picks)
                shelf.title.equals("Listen again", ignoreCase = true) || shelf.title.equals("Послушать снова", ignoreCase = true) -> ctx.getString(R.string.online_listen_again)
                shelf.title.equals("Mixes", ignoreCase = true) || shelf.title.equals("Миксы", ignoreCase = true) -> ctx.getString(R.string.online_mixes)
                shelf.title.equals("Forgotten favorites", ignoreCase = true) || shelf.title.equals("Забытые фавориты", ignoreCase = true) -> ctx.getString(R.string.online_forgotten_favorites)
                shelf.title.startsWith("Similar to", ignoreCase = true) -> shelf.title.replace(Regex("(?i)Similar to"), ctx.getString(R.string.online_similar_to))
                else -> shelf.title
            }
            tvTitle.text = localizedTitle
            
            layoutShelfHeader.setOnClickListener {
                onTitleClick?.invoke(shelf)
            }

            val isTracks = shelf.items.isNotEmpty() && shelf.items.all { it is OnlineTrack }
            
            // Recreate adapter if type changed or it doesn't exist
            if (innerAdapter == null || (isTracks && innerAdapter !is OnlineTracksAdapter) || (!isTracks && innerAdapter !is OnlineTracksCardAdapter)) {
                if (isTracks) {
                    innerAdapter = OnlineTracksAdapter(true) { list, index -> 
                        val currentItems = currentShelf?.items ?: emptyList()
                        onItemClick(list[index], currentItems, currentShelf?.title ?: "") 
                    }
                } else {
                    innerAdapter = OnlineTracksCardAdapter(shelf.isImmersive) { item, _ -> 
                        val currentItems = currentShelf?.items ?: emptyList()
                        onItemClick(item, currentItems, currentShelf?.title ?: "") 
                    }
                }
            }
            
            // Update adapter data
            if (isTracks) {
                (innerAdapter as OnlineTracksAdapter).submitList(shelf.items.filterIsInstance<OnlineTrack>())
            } else {
                (innerAdapter as OnlineTracksCardAdapter).submitList(shelf.items)
            }

            recyclerItems.setRecycledViewPool(if (isTracks) tracksPool else cardsPool)
            
            if (currentIsTracks != isTracks) {
                if (isTracks) {
                    val layoutManager = androidx.recyclerview.widget.GridLayoutManager(
                        itemView.context, 3, androidx.recyclerview.widget.GridLayoutManager.HORIZONTAL, false
                    )
                    layoutManager.initialPrefetchItemCount = 9
                    recyclerItems.layoutManager = layoutManager
                } else {
                    val layoutManager = androidx.recyclerview.widget.LinearLayoutManager(itemView.context, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
                    layoutManager.initialPrefetchItemCount = 4
                    recyclerItems.layoutManager = layoutManager
                }
                recyclerItems.setItemViewCacheSize(8)
                recyclerItems.itemAnimator = null
                recyclerItems.isNestedScrollingEnabled = false
                currentIsTracks = isTracks
            } else if (!isTracks) {
                // Ensure layout manager matches immersive state if it's cards
                val immersiveAdapter = innerAdapter as OnlineTracksCardAdapter
                val prevImmersive = (recyclerItems.adapter as? OnlineTracksCardAdapter)?.isImmersive
                if (prevImmersive != immersiveAdapter.isImmersive) {
                    val layoutManager = androidx.recyclerview.widget.LinearLayoutManager(itemView.context, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
                    recyclerItems.layoutManager = layoutManager
                    recyclerItems.itemAnimator = null
                    recyclerItems.isNestedScrollingEnabled = false
                }
            }
            
            if (recyclerItems.adapter !== innerAdapter) {
                recyclerItems.swapAdapter(innerAdapter, true)
            }
        }
    }

    class EndlessViewHolder(
        itemView: View,
        private val pool: RecyclerView.RecycledViewPool
    ) : RecyclerView.ViewHolder(itemView) {
        val recyclerItems: RecyclerView = itemView.findViewById(R.id.recyclerEndlessItems)
        private var isInitialized = false
        private var innerAdapter: OnlineQuickPicksAdapter? = null
        private var currentShelf: Shelf? = null
        
        fun bind(shelf: Shelf, onItemClick: (OnlineItem, List<OnlineItem>, String) -> Unit) {
            currentShelf = shelf
            if (innerAdapter == null) {
                innerAdapter = OnlineQuickPicksAdapter { list, index -> 
                    val currentItems = currentShelf?.items ?: emptyList()
                    onItemClick(list[index], currentItems, currentShelf?.title ?: "") 
                }
            }
            innerAdapter?.submitList(shelf.items.filterIsInstance<OnlineTrack>())
            if (!isInitialized) {
                recyclerItems.setRecycledViewPool(pool)
                val layoutManager = androidx.recyclerview.widget.GridLayoutManager(itemView.context, 3)
                recyclerItems.layoutManager = layoutManager
                recyclerItems.itemAnimator = null
                isInitialized = true
            }
            if (recyclerItems.adapter !== innerAdapter) {
                recyclerItems.swapAdapter(innerAdapter, true)
            }
        }
    }

    class ShelfDiffCallback : DiffUtil.ItemCallback<Shelf>() {
        override fun areItemsTheSame(oldItem: Shelf, newItem: Shelf): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: Shelf, newItem: Shelf): Boolean {
            return oldItem == newItem
        }
    }
}
