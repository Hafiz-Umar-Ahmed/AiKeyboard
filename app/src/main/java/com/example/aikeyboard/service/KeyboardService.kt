package com.example.aikeyboard.service

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class KeyboardService : InputMethodService(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val TAG = "KeyboardServiceLog"

    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    override fun onCreateInputView(): View {
        Log.d(TAG, "onCreateInputView: Triggered")

        val composeView = ComposeKeyboardView(this)

        // 1. Attach owners to the Compose View directly
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)

        // 2. CRITICAL FIX: Attach owners to the root Window DecorView
        // Compose's WindowRecomposer requires these at the absolute root of the IME window
        window?.window?.decorView?.let { decorView ->
            Log.d(TAG, "onCreateInputView: Attaching owners to decorView")
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
        }

        return composeView
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate: Service created")
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        handleLifecycleEvent(Lifecycle.Event.ON_START) // Add ON_START here
    }

    // --- FIX: Force keyboard to show on Emulators ---
    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        Log.d(TAG, "onEvaluateInputViewShown: Forcing view to show")
        return true
    }

    // --- FIX: Tie Compose Resume state to the Window visibility ---
    override fun onWindowShown() {
        super.onWindowShown()
        Log.d(TAG, "onWindowShown: Keyboard is now visible on screen")
        handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        Log.d(TAG, "onWindowHidden: Keyboard hidden")
        handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Service destroyed")
        super.onDestroy()
        handleLifecycleEvent(Lifecycle.Event.ON_STOP) // Clean up Compose
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    private fun handleLifecycleEvent(event: Lifecycle.Event) {
        Log.d(TAG, "handleLifecycleEvent: Pushing state $event")
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}