package com.faulk.appkiller.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.faulk.appkiller.data.AppInfo
import com.faulk.appkiller.data.AppType
import com.faulk.appkiller.data.CategorizedApps
import com.faulk.appkiller.repository.AppRepository
import kotlinx.coroutines.launch

class AppKillerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    private val _categorizedApps = MutableLiveData(CategorizedApps(emptyList(), emptyList()))
    val categorizedApps: LiveData<CategorizedApps> = _categorizedApps

    private val _isLoadingUserApps = MutableLiveData(false)
    val isLoadingUserApps: LiveData<Boolean> = _isLoadingUserApps

    private val _isLoadingSystemApps = MutableLiveData(false)
    val isLoadingSystemApps: LiveData<Boolean> = _isLoadingSystemApps

    init {
        loadUserApps()
    }

    fun loadUserApps() {
        viewModelScope.launch {
            _isLoadingUserApps.value = true
            try {
                val userApps = repository.getApps(AppType.USER)
                val current = _categorizedApps.value ?: CategorizedApps(emptyList(), emptyList())
                _categorizedApps.value = current.copy(userApps = userApps)
            } catch (e: Exception) {
                // Handle error silently - empty list will show empty state
            } finally {
                _isLoadingUserApps.value = false
            }
        }
    }

    fun loadSystemApps() {
        val current = _categorizedApps.value ?: CategorizedApps(emptyList(), emptyList())
        // Only load once
        if (current.systemApps.isNotEmpty()) return

        viewModelScope.launch {
            _isLoadingSystemApps.value = true
            try {
                val systemApps = repository.getApps(AppType.SYSTEM)
                val updated = _categorizedApps.value ?: CategorizedApps(emptyList(), emptyList())
                _categorizedApps.value = updated.copy(systemApps = systemApps)
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                _isLoadingSystemApps.value = false
            }
        }
    }

    fun toggleAppSelection(app: AppInfo) {
        val current = _categorizedApps.value ?: return
        val updatedUserApps = current.userApps.map {
            if (it.packageName == app.packageName) it.copy(isSelected = !it.isSelected) else it
        }
        val updatedSystemApps = current.systemApps.map {
            if (it.packageName == app.packageName) it.copy(isSelected = !it.isSelected) else it
        }
        _categorizedApps.value = CategorizedApps(updatedUserApps, updatedSystemApps)
    }
}
