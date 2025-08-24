package com.github.trebent.tapp

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.collections.listOf


data class TappGroup(val name: String)


class GroupViewModel() : ViewModel() {
    private val _groups = MutableStateFlow<List<TappGroup>>(emptyList())
    val groups = _groups.asStateFlow()

    // Used for relaying to the MainActivity if the splash screen can be removed or not.
    private val _i = MutableStateFlow(false)
    val initialised = _i.asStateFlow()

    init {
        Log.i("GroupViewModel", "initialising the auth view model")
        // TODO: remove hardcoded sleep
        Thread.sleep(200)
        _groups.value = listOf(
            TappGroup("group1"),
            TappGroup("group2"),
            TappGroup("group3"),
            TappGroup("group4"),
            TappGroup("group5"),
            TappGroup("group6"),
            TappGroup("group7"),
            TappGroup("group8"),
            TappGroup("group9"),
            TappGroup("group10"),
        )
        // TODO: add cloud fetch
        _i.value = true
    }

    fun createGroup(newTappGroup: TappGroup) {
        Log.i("GroupVewiModel", "adding group ${newTappGroup.name}")
        _groups.value = _groups.value + newTappGroup
    }
}