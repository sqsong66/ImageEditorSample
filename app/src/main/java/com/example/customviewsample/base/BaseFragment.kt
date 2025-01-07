package com.example.customviewsample.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.launch

abstract class BaseFragment<V : ViewBinding>(open val block: (LayoutInflater, ViewGroup?, Boolean) -> V) : Fragment() {

    private var _binding: V? = null
    private var isInitialLoaded = false
    protected var isBindingDestroyed = false

    protected val binding get() = _binding!!

    abstract fun initData(savedInstanceState: Bundle?)

    open fun beforeInflateView() {}

    open fun observeData() {}

    open fun loadInitialData() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        beforeInflateView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = block(inflater, container, false)
        isBindingDestroyed = false
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData(savedInstanceState)
        childFragmentManager.addFragmentOnAttachListener { _, fragment -> onFragmentAttached(fragment) }
        observeData()
    }

    protected open fun onFragmentAttached(fragment: Fragment) {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isBindingDestroyed = true
    }

    override fun onResume() {
        super.onResume()
        if (!isInitialLoaded) {
            loadInitialData()
            isInitialLoaded = true
        }
    }

    protected fun launchCollect(block: suspend () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            block()
        }
    }


    fun isBindingAvailable(): Boolean {
        return _binding != null && !isBindingDestroyed && isAdded && !isRemoving && !isDetached && view != null
    }
}