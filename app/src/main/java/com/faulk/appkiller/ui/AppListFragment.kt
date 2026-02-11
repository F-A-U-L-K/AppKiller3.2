package com.faulk.appkiller.ui

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.faulk.appkiller.adapter.AppListAdapter
import com.faulk.appkiller.databinding.FragmentAppListBinding
import com.faulk.appkiller.viewmodel.AppKillerViewModel
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class AppListFragment : Fragment() {

    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppKillerViewModel by activityViewModels()

    private lateinit var appAdapter: AppListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        appAdapter = AppListAdapter(
            onAppChecked = { app, _ ->
                viewModel.toggleAppSelection(app)
            },
            onManageClicked = { app ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", app.packageName, null)
                    }
                    startActivity(intent)
                } catch (_: Exception) {}
            }
        )
        binding.recyclerViewFragment.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = appAdapter
        }
    }

    private fun setupObservers() {
        val position = arguments?.getInt(ARG_POSITION) ?: 0

        viewModel.categorizedApps.observe(viewLifecycleOwner) { categorized ->
            val appsList = if (position == 0) categorized.userApps else categorized.systemApps
            appAdapter.submitList(appsList)

            binding.emptyViewList.visibility = if (appsList.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_POSITION = "position"

        fun newInstance(position: Int): AppListFragment {
            val fragment = AppListFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }
}
