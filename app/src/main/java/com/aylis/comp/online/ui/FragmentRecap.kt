package com.aylis.comp.online.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.aylis.R
import com.aylis.comp.online.managers.StatsManager

class FragmentRecap : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_online_recap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadStats(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadStats(it) }
    }

    private fun loadStats(view: View) {
        val tvTotalTime = view.findViewById<TextView>(R.id.tvTotalTime)
        val tvTotalPlays = view.findViewById<TextView>(R.id.tvTotalPlays)
        val tvAvgPlays = view.findViewById<TextView>(R.id.tvAvgPlays)
        val tvTopTrackTitle = view.findViewById<TextView>(R.id.tvTopTrackTitle)
        val tvTopTrackArtist = view.findViewById<TextView>(R.id.tvTopTrackArtist)
        val chartContainer = view.findViewById<LinearLayout>(R.id.chartContainer)

        val totalMs = StatsManager.getTotalTimeMs()
        val hours = (totalMs / (1000 * 60 * 60)).toInt()
        val mins = ((totalMs / (1000 * 60)) % 60).toInt()
        tvTotalTime.text = getString(R.string.online_recap_time_format, hours, mins)
        tvTotalPlays.text = StatsManager.getTotalPlays().toString()
        
        val avgPlays = StatsManager.getAvgPlaysPerDay()
        val avgHours = ((totalMs / Math.max(1, StatsManager.getTotalPlays())) * avgPlays / (1000 * 60 * 60)).toInt()
        val avgMins = (((totalMs / Math.max(1, StatsManager.getTotalPlays())) * avgPlays / (1000 * 60)) % 60).toInt()
        tvAvgPlays.text = if (avgHours > 0) getString(R.string.online_recap_time_format, avgHours, avgMins) else getString(R.string.online_recap_time_format_mins, avgMins)

        val topTrackParts = StatsManager.getTopTrack().split("\n")
        tvTopTrackTitle.text = topTrackParts.getOrNull(0) ?: getString(R.string.online_recap_none)
        tvTopTrackArtist.text = topTrackParts.getOrNull(1) ?: getString(R.string.online_recap_none)

        // Build chart
        chartContainer.removeAllViews()
        val dailyTimes = StatsManager.getLast7DaysTimeMs()
        val maxTime = Math.max(dailyTimes.maxOrNull() ?: 1L, 1L)
        
        for (i in 0 until 7) {
            val barLayout = LinearLayout(requireContext())
            val lp = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
            )
            barLayout.layoutParams = lp
            barLayout.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            
            val barView = androidx.cardview.widget.CardView(requireContext())
            val heightPct = Math.max(0.05f, dailyTimes[i].toFloat() / maxTime.toFloat())
            val heightPx = (120 * resources.displayMetrics.density * heightPct).toInt()
            val barLp = LinearLayout.LayoutParams(
                (24 * resources.displayMetrics.density).toInt(), heightPx
            )
            barLp.bottomMargin = (8 * resources.displayMetrics.density).toInt()
            barView.layoutParams = barLp
            
            barView.radius = (12 * resources.displayMetrics.density)
            barView.setCardBackgroundColor(android.graphics.Color.parseColor(if (i == 6) "#1C6B50" else "#508774"))
            barView.cardElevation = 0f
            
            barLayout.addView(barView)
            chartContainer.addView(barLayout)
        }
    }

    companion object {
        fun newInstance() = FragmentRecap()
    }
}
