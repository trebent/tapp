package com.github.trebent.tapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.trebent.tapp.api.Account
import com.github.trebent.tapp.dataStore
import com.github.trebent.tapp.tokenkey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


data class TappGroup(
    val id: Int,
    val name: String,
    val emoji: String,
    val description: String,
    val members: List<Account>,
    val invites: List<Account>,
    var edit: Boolean
)

val newTappGroup = TappGroup(0, "", "", "", emptyList(), emptyList(), true)
val testGroup =
    TappGroup(12, "group name", "❤️", "group description", emptyList(), emptyList(), false)

val testGroups = listOf(
    TappGroup(1, "group1", "", "some words", emptyList(), emptyList(), false),
    TappGroup(2, "group2", "", "some words", emptyList(), emptyList(), false),
    TappGroup(3, "group3", "", "some words", emptyList(), emptyList(), false),
    TappGroup(4, "group4", "", "some words", emptyList(), emptyList(), false),
    TappGroup(5, "group5", "", "some words", emptyList(), emptyList(), false),
    TappGroup(6, "group6", "", "some words", emptyList(), emptyList(), false),
    TappGroup(7, "group7", "", "some words", emptyList(), emptyList(), false),
    TappGroup(8, "group8", "", "some words", emptyList(), emptyList(), false),
    TappGroup(9, "group9", "", "some words", emptyList(), emptyList(), false),
    TappGroup(10, "group10", "", "some words", emptyList(), emptyList(), false),
)

class TappGroupViewModel(private val application: Application) : AndroidViewModel(application) {
    private val _groups = MutableStateFlow<List<TappGroup>>(emptyList())
    val groups = _groups.asStateFlow()

    // Used for relaying to the MainActivity if the splash screen can be removed or not.
    private val _i = MutableStateFlow(false)
    private val _token = MutableStateFlow<String?>(null)

    val initialised = _i.asStateFlow()

    init {
        Log.i("GroupViewModel", "initialising the group view model")
        viewModelScope.launch {
            Log.i("GroupViewModel", "checking datastore for stored tokens")

            val preferences = application.dataStore.data.first()
            _token.value = preferences[tokenkey]

            if (_token.value != null) {
                Log.i("GroupViewModel", "found token, fetching groups")


            }

            Log.i("GroupViewModel", "initialised account view model")
            _i.value = true
        }
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
            emptyList(),
            false
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
}