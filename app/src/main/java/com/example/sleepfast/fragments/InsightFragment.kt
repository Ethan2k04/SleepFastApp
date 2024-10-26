package com.example.sleepfast.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.sleepfast.R
import com.example.sleepfast.fragments.secondary.OverviewFragment
import com.example.sleepfast.fragments.secondary.SleepPlanFragment
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.view.updateLayoutParams

class InsightFragment : Fragment() {
    private lateinit var indicatorLine: View
    private lateinit var overviewTab: TextView
    private lateinit var sleepPlanTab: TextView

    private val offset = 30

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_insight, container, false)

        // Initialize views
        overviewTab = view.findViewById(R.id.overviewTab)
        sleepPlanTab = view.findViewById(R.id.sleepPlanTab)
        indicatorLine = view.findViewById(R.id.indicatorLine)

        // moveIndicator(overviewTab)

        // Set click listeners for the tabs
        overviewTab.setOnClickListener {
            loadFragment(OverviewFragment()) // Load OverviewFragment
            moveIndicator(overviewTab) // Move indicator to Overview tab
        }

        sleepPlanTab.setOnClickListener {
            loadFragment(SleepPlanFragment()) // Load SleepPlanFragment
            moveIndicator(sleepPlanTab) // Move indicator to SleepPlan tab
        }

        return view
    }

    private fun loadFragment(fragment: Fragment) {
        // Replace the fragment inside the container
        val fragmentTransaction: FragmentTransaction = parentFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container_insight, fragment)
        fragmentTransaction.commit()
    }

    private fun moveIndicator(tab: TextView) {
        // Move the indicator to align with the selected tab
        indicatorLine.updateLayoutParams<RelativeLayout.LayoutParams> {
            marginStart = tab.left + offset
        }
    }
}
