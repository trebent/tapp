package com.github.trebent.tapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.trebent.tapp.api.TappGroup
import com.github.trebent.tapp.api.groupService
import com.github.trebent.tapp.dataStore
import com.github.trebent.tapp.tokenkey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


val newTappGroup = TappGroup(0, "", "", "", emptyList(), emptyList())
val testGroup =
    TappGroup(12, "group name", "❤️", "group description", emptyList(), emptyList())

val testGroups = listOf(
    TappGroup(1, "group1", "", "some words", emptyList(), emptyList()),
    TappGroup(2, "group2", "", "some words", emptyList(), emptyList()),
    TappGroup(3, "group3", "", "some words", emptyList(), emptyList()),
    TappGroup(4, "group4", "", "some words", emptyList(), emptyList()),
    TappGroup(5, "group5", "", "some words", emptyList(), emptyList()),
    TappGroup(6, "group6", "", "some words", emptyList(), emptyList()),
    TappGroup(7, "group7", "", "some words", emptyList(), emptyList()),
    TappGroup(8, "group8", "", "some words", emptyList(), emptyList()),
    TappGroup(9, "group9", "", "some words", emptyList(), emptyList()),
    TappGroup(10, "group10", "", "some words", emptyList(), emptyList()),
)

class TappGroupViewModel(private val application: Application) : AndroidViewModel(application) {
    private val _groups = MutableStateFlow<List<TappGroup>>(emptyList())
    val groups = _groups.asStateFlow()

    // Used for relaying to the MainActivity if the splash screen can be removed or not.
    private val _i = MutableStateFlow(false)
    private val _token = MutableStateFlow<String?>(null)
    private val _selectedGroup = MutableStateFlow(newTappGroup)

    val initialised = _i.asStateFlow()
    val selectedGroup = _selectedGroup.asStateFlow()

    init {
        Log.i("GroupViewModel", "initialising the group view model")
        viewModelScope.launch {
            Log.i("GroupViewModel", "checking datastore for stored tokens")

            val preferences = application.dataStore.data.first()
            _token.value = preferences[tokenkey]

            _i.value = true

            application.dataStore.data.collect { preferences ->
                Log.i("GroupViewModel", "preferences $preferences")
                _token.value = preferences[tokenkey]
                Log.i("GroupViewModel", "preferences updated token ${_token.value}")
            }
        }
    }

    fun list(): StateFlow<List<TappGroup>> {
        Log.i("GroupViewModel", "listing groups")
        viewModelScope.launch {
            val response = groupService.listGroups(_token.value!!)
            if (response.isSuccessful) {
                updateGroups(response.body()!!)
            } else {
                Log.e("GroupViewModel", "failed to fetch groups!")
            }
        }

        return groups
    }

    fun selectGroup(group: TappGroup) {
        _selectedGroup.value = group
    }

    fun get(id: Int): TappGroup {
        var tg = _groups.value.find { tg -> tg.id == id }
        if (tg == null) {
            tg = newTappGroup
        }
        return tg
    }

    fun delete(tappGroup: TappGroup) {
        Log.i("GroupViewModel", "deleting group ${tappGroup.id}: ${tappGroup.name}")
        _groups.value = _groups.value - tappGroup
    }

    fun save(tappGroup: TappGroup) {
        Log.i("GroupViewModel", "saving group ${tappGroup.id}: ${tappGroup.name}")
        if (tappGroup.id == 0) {
            create(tappGroup)
        } else {
            update(tappGroup)
        }
    }

    fun create(n: TappGroup) {
        Log.i("GroupViewModel", "adding group ${n.name}")
        val temp = TappGroup(
            _groups.value.size + 2,
            n.name,
            n.emoji,
            n.description,
            emptyList(),
            emptyList()
        )
        Log.i("GroupViewModel", "TEMP, gave it id ${temp.id}")
        _groups.value = _groups.value + temp
    }

    fun update(updatedTappGroup: TappGroup) {
        Log.i("GroupViewModel", "updating group ${updatedTappGroup.id}: ${updatedTappGroup.name}")
        _groups.value = _groups.value.map { group ->
            if (group.id == updatedTappGroup.id) updatedTappGroup else group
        }
    }

    private fun updateGroups(remoteGroups: List<TappGroup>) {
        Log.i("GroupViewModel", "setting ${remoteGroups.size} groups")
        _groups.value = remoteGroups
    }
}