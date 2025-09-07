package com.github.trebent.tapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.trebent.tapp.api.Account
import com.github.trebent.tapp.api.Tapp
import com.github.trebent.tapp.api.TappGroup
import com.github.trebent.tapp.api.TappGroupInvitation
import com.github.trebent.tapp.api.groupService
import com.github.trebent.tapp.api.sortedTapps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


val newTappGroup = TappGroup(0, "", "", "", "", emptyList(), emptyList())
val testGroup =
    TappGroup(
        12,
        "group name",
        "❤️",
        "",
        "group description",
        listOf(testAccount, testAccount, testAccount),
        emptyList()
    )
val testGroupOwnerNoMembers =
    TappGroup(
        12,
        "group name",
        "❤️",
        testAccount.email,
        "group description",
        emptyList(),
        emptyList()
    )
val testGroupOwner =
    TappGroup(
        12,
        "group name",
        "❤️",
        testAccount.email,
        "group description",
        listOf(testAccount, testAccount, testAccount),
        emptyList()
    )
val testTapps = listOf(
    Tapp(1, 123, testAccountWithNullTag),
    Tapp(1, 123, testAccountWithNullTag),
    Tapp(1, 123, testAccountWithNullTag),
    Tapp(1, 123, testAccountWithNullTag),
    Tapp(1, 123, testAccountWithEmptyTag),
    Tapp(1, 123, testAccount),
    Tapp(1, 123, testAccountWithEmptyTag),
    Tapp(1, 123, testAccount),
    Tapp(1, 123, testAccount),
    Tapp(1, 123, testAccount),
    Tapp(1, 123, testAccount),
    Tapp(1, 123, testAccountTag2),
    Tapp(1, 123, testAccount),
    Tapp(1, 123, testAccountWithEmptyTag),
    Tapp(1, 123, testAccountTag2),
    Tapp(1, 123, testAccountTag2),
    Tapp(1, 123, testAccountWithEmptyTag),
    Tapp(1, 123, testAccountTag2),
)
val testGroups = listOf(
    TappGroup(1, "group1", "", "", "some words", listOf(testAccount, testAccount), emptyList()),
    TappGroup(2, "group2", "", "", "some words", emptyList(), emptyList()),
    TappGroup(3, "group3", "", "", "some words", emptyList(), emptyList()),
    TappGroup(4, "group4", "", "", "some words", emptyList(), emptyList()),
    TappGroup(5, "group5", "", "", "some words", emptyList(), emptyList()),
    TappGroup(6, "group6", "", "", "some words", emptyList(), emptyList()),
    TappGroup(7, "group7", "", "", "some words", emptyList(), emptyList()),
    TappGroup(8, "group8", "", "", "some words", emptyList(), emptyList()),
    TappGroup(9, "group9", "", "", "some words", emptyList(), emptyList()),
    TappGroup(10, "group10", "", "", "some words", emptyList(), emptyList()),
)

val testInvite = TappGroupInvitation(1, "name", "emial@test.se")
val testInvites = listOf(testInvite)

class TappGroupViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _selectedGroup = MutableStateFlow(newTappGroup)
    val selectedGroup = _selectedGroup.asStateFlow()

    private val _groups = MutableStateFlow<List<TappGroup>>(emptyList())
    val groups = _groups.asStateFlow()

    private val _invitations = MutableStateFlow<List<TappGroupInvitation>>(emptyList())
    val invitations = _invitations.asStateFlow()

    private val _tapps = MutableStateFlow<List<Tapp>>(emptyList())
    val tapps = _tapps.asStateFlow()

    // Used for relaying to the MainActivity if the splash screen can be removed or not.
    private val _i = MutableStateFlow(false)
    val initialised = _i.asStateFlow()

    private var _tokenGetter: () -> String = { "" }

    init {
        Log.i("GroupViewModel", "initialising the group view model")
        _i.value = true
    }

    fun setTokenGetter(getter: () -> String) {
        _tokenGetter = getter
    }

    fun tapp(a: Account) {
        Log.i("GroupViewModel", "tapped group! ${selectedGroup.value.id}")
        val t = Tapp(selectedGroup.value.id, System.currentTimeMillis(), a)
        _tapps.value = listOf(t) + _tapps.value
        viewModelScope.launch {
            val response = groupService.createTapp(_tokenGetter(), selectedGroup.value.id)
            if (!response.isSuccessful) {
                Log.e("GroupViewModel", "failed to tapp group ${selectedGroup.value.id}")
            }
        }
    }

    fun listTapps(): StateFlow<List<Tapp>> {
        Log.i("GroupViewModel", "listing tapps for group ${selectedGroup.value.id}")
        viewModelScope.launch {
            val response = groupService.listTapps(_tokenGetter(), selectedGroup.value.id)
            if (response.isSuccessful) {
                _tapps.value = sortedTapps(response.body()!!)
                Log.i("GroupViewModel", "listed ${_tapps.value.size} tapps!")
            } else {
                Log.e("GroupViewModel", "failed to list tapps for group ${selectedGroup.value.id}")
            }
        }

        return tapps
    }

    fun list(): StateFlow<List<TappGroup>> {
        Log.i("GroupViewModel", "listing groups")
        viewModelScope.launch {
            val response = groupService.listGroups(_tokenGetter())
            if (response.isSuccessful) {
                updateGroups(response.body()!!)
            } else {
                Log.e("GroupViewModel", "failed to fetch groups!")
            }
        }

        return groups
    }

    fun refreshSelectedGroup(): StateFlow<TappGroup> {
        Log.i("GroupViewModel", "refreshing the selected group")
        viewModelScope.launch {
            val response = groupService.getGroup(_tokenGetter(), _selectedGroup.value.id)
            if (response.isSuccessful) {
                _selectedGroup.value = response.body()!!
            } else {
                Log.e("GroupViewModel", "failed to refresh selected group")
            }
        }

        return _selectedGroup
    }

    fun selectGroup(group: TappGroup) {
        _selectedGroup.value = group
    }

    fun delete(tappGroup: TappGroup, onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("GroupViewModel", "deleting group ${tappGroup.id}: ${tappGroup.name}")
        viewModelScope.launch {
            val response = groupService.deleteGroup(_tokenGetter(), tappGroup.id)
            if (response.isSuccessful) {
                Log.i(
                    "GroupViewModel",
                    "successfully deleted group ${tappGroup.id}: ${tappGroup.name}"
                )
                onSuccess()
            } else {
                Log.e(
                    "GroupViewModel",
                    "failed to delete group ${tappGroup.id}: ${tappGroup.name}"
                )
                onFailure()
            }
        }
    }

    fun save(tappGroup: TappGroup, onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("GroupViewModel", "saving group ${tappGroup.id}: ${tappGroup.name}")
        if (tappGroup.id == 0) {
            create(tappGroup, onSuccess, onFailure)
        } else {
            update(tappGroup, onSuccess, onFailure)
        }
    }

    fun inviteToGroup(
        email: String,
        group: TappGroup,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        Log.i("GroupViewModel", "inviting user $email to group $group.id")
        viewModelScope.launch {
            val response = groupService.inviteToGroup(_tokenGetter(), group.id, email)
            if (response.isSuccessful) {
                list()
                onSuccess()
            } else {
                Log.e("GroupViewModel", "failed to invite $email to group $group.id!")
                onFailure()
            }
        }
    }

    fun listInvitations(): StateFlow<List<TappGroupInvitation>> {
        Log.i("GroupViewModel", "listing group invitations")
        viewModelScope.launch {
            val response = groupService.listInvitations(_tokenGetter())
            if (response.isSuccessful) {
                updateInvitations(response.body()!!)
            } else {
                Log.e("GroupViewModel", "failed to list invitations!")
            }
        }

        return invitations
    }

    fun acceptInvitation(
        invite: TappGroupInvitation,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        Log.i("GroupViewModel", "accepting group invitation ${invite.groupId}")
        viewModelScope.launch {
            val response = groupService.joinGroup(_tokenGetter(), invite.groupId)
            if (response.isSuccessful) {
                onSuccess()
                removeInvitation(invite.groupId)

                // Trigger group-listing
                list()
            } else {
                Log.e("GroupViewModel", "failed to accept invitation ${invite.groupId}!")
                onFailure()
            }
        }
    }

    fun declineInvitation(
        invite: TappGroupInvitation,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        Log.i("GroupViewModel", "declining group invitation ${invite.groupId}")
        viewModelScope.launch {
            val response = groupService.declineGroup(_tokenGetter(), invite.groupId)
            if (response.isSuccessful) {
                onSuccess()
                removeInvitation(invite.groupId)
            } else {
                Log.e("GroupViewModel", "failed to accept invitation ${invite.groupId}!")
                onFailure()
            }
        }
    }

    fun leaveGroup(
        group: TappGroup,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        Log.i("GroupViewModel", "leaving group ${group.id}")
        viewModelScope.launch {
            val response = groupService.leaveGroup(_tokenGetter(), group.id)
            if (response.isSuccessful) {
                onSuccess()
                removeGroup(group)
            } else {
                Log.e("GroupViewModel", "failed to leave group ${group.id}!")
                onFailure()
            }
        }
    }

    fun kickFromGroup(
        group: TappGroup,
        email: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        Log.i("GroupViewModel", "kicking $email from group ${group.id}")
        viewModelScope.launch {
            val response = groupService.kickFromGroup(_tokenGetter(), group.id, email)
            if (response.isSuccessful) {
                onSuccess()
                _selectedGroup.value = TappGroup(
                    group.id,
                    group.name,
                    group.emoji,
                    group.owner,
                    group.description,
                    group.members!!.filter { g -> g.email != email },
                    group.invites,
                )
            } else {
                Log.e("GroupViewModel", "failed to leave group ${group.id}!")
                onFailure()
            }
        }
    }

    private fun create(new: TappGroup, onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("GroupViewModel", "creating group ${new.name}")
        viewModelScope.launch {
            val response = groupService.createGroup(_tokenGetter(), new)
            if (response.isSuccessful) {
                Log.i(
                    "GroupViewModel",
                    "successfully created group ${new.id}: ${new.name}"
                )
                _groups.value = _groups.value + response.body()!!
                onSuccess()
            } else {
                Log.e(
                    "GroupViewModel",
                    "failed to create group ${new.id}: ${new.name}"
                )
                onFailure()
            }
        }
    }

    private fun update(updatedTappGroup: TappGroup, onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("GroupViewModel", "updating group ${updatedTappGroup.id}: ${updatedTappGroup.name}")
        viewModelScope.launch {
            val response =
                groupService.updateGroup(_tokenGetter(), updatedTappGroup.id, updatedTappGroup)
            if (response.isSuccessful) {
                Log.i(
                    "GroupViewModel",
                    "successfully updated group ${updatedTappGroup.id}: ${updatedTappGroup.name}"
                )
                _selectedGroup.value = updatedTappGroup
                onSuccess()
            } else {
                Log.e(
                    "GroupViewModel",
                    "failed to update group ${updatedTappGroup.id}: ${updatedTappGroup.name}"
                )
                onFailure()
            }
        }
    }

    private fun updateGroups(remoteGroups: List<TappGroup>) {
        Log.i("GroupViewModel", "setting ${remoteGroups.size} groups")
        _groups.value = remoteGroups
    }

    private fun removeGroup(group: TappGroup) {
        Log.i("GroupViewModel", "removing local group ${group.id}")
        _groups.value = _groups.value.filter { g -> g.id != group.id }
    }

    private fun updateInvitations(remoteInvitations: List<TappGroupInvitation>) {
        Log.i("GroupViewModel", "setting ${remoteInvitations.size} invitations")
        _invitations.value = remoteInvitations
    }

    private fun removeInvitation(groupID: Int) {
        Log.i("GroupViewModel", "removing local invitation $groupID")
        _invitations.value = _invitations.value.filter { i -> i.groupId != groupID }
    }
}