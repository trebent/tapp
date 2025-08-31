package com.github.trebent.tapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.github.trebent.tapp.api.TappGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


val newTappGroup = TappGroup(0, "", "", "", true)
val testGroup = TappGroup(12, "group name", "❤️", "group description", false)

val testGroups = listOf(
    TappGroup(1, "group1", "", "some words", false),
    TappGroup(2, "group2", "", "some words", false),
    TappGroup(3, "group3", "", "some words", false),
    TappGroup(4, "group4", "", "some words", false),
    TappGroup(5, "group5", "", "some words", false),
    TappGroup(6, "group6", "", "some words", false),
    TappGroup(7, "group7", "", "some words", false),
    TappGroup(8, "group8", "", "some words", false),
    TappGroup(9, "group9", "", "some words", false),
    TappGroup(10, "group10", "", "some words", false),
)

class TappGroupViewModel() : ViewModel() {
    private val _groups = MutableStateFlow<List<TappGroup>>(emptyList())
    val groups = _groups.asStateFlow()

    // Used for relaying to the MainActivity if the splash screen can be removed or not.
    private val _i = MutableStateFlow(false)
    val initialised = _i.asStateFlow()

    init {
        Log.i("GroupViewModel", "initialising the auth view model")
        // TODO: remove hardcoded sleep
        Thread.sleep(200)
        _groups.value = testGroups
        // TODO: add cloud fetch
        _i.value = true
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