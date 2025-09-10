/**
 * This file contain implementation for the Tapp groups. Creation, updates, deletions, user memberships
 * etc. are all handled here. And, of course, the tapping itself.
 */
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
import com.github.trebent.tapp.notification.TappNotificationEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


/**
 * New tapp group, this object is used when creating new tapp groups as a baseline for value defaults.
 */
val newTappGroup = TappGroup(0, "", "", "", "", emptyList(), emptyList())

/*
Test declarations, for preview composables.
*/
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

/**
 * Tapp group view model. Most backend communication will accept a success and failure callback,
 * enabling just-in-time-reactions in the UI. Listing content is handled slightly differently as
 * they rather return state flows for UI components to subscribe to.
 *
 * @property application
 * @constructor Create empty Tapp group view model
 */
class TappGroupViewModel(private val application: Application) : AndroidViewModel(application) {

    // The currently selected group, set on navigation to always keep a reference to what is being
    // worked on.
    private val _selectedGroup = MutableStateFlow(newTappGroup)
    val selectedGroup = _selectedGroup.asStateFlow()

    // The user's groups.
    private val _groups = MutableStateFlow<List<TappGroup>>(emptyList())
    val groups = _groups.asStateFlow()

    // Any pending group invitations.
    private val _invitations = MutableStateFlow<List<TappGroupInvitation>>(emptyList())
    val invitations = _invitations.asStateFlow()

    // The current group's tapps.
    private val _tapps = MutableStateFlow<List<Tapp>>(emptyList())
    val tapps = _tapps.asStateFlow()

    // Used for relaying to the MainActivity if the splash screen can be removed or not.
    private val _i = MutableStateFlow(false)
    val initialised = _i.asStateFlow()

    // A callback that can be used to fetch the current token. Uniform way of fetching
    // the outgoing HTTP Authorization header required for ALL authorized calls.
    private var _tokenGetter: () -> String = { "" }

    /**
     * The Tapp group view model will subscribe to tapp notifications to be able to refresh the tapp
     * list without fetching it fully from the backend. More efficient this way. It will only prepend
     * to the current tapps if the incoming tapp matches the selected tapp group (meaning it's applicable
     * to the current navigation).
     */
    init {
        Log.i("GroupViewModel", "initialising the group view model")

        _i.value = true
        viewModelScope.launch {
            TappNotificationEvents.events.collect { tapp ->
                Log.i("GroupViewModel", "running tapp notification event collector")

                if (tapp.groupId == _selectedGroup.value.id) {
                    _tapps.value = listOf(tapp) + _tapps.value
                }
            }
        }
    }

    /**
     * Set token getter, called from the main activity.
     *
     * @param getter
     */
    fun setTokenGetter(getter: () -> String) {
        _tokenGetter = getter
    }

    /**
     * Tapp a group, send a nofification to all members! This is where the fun begins :-).
     *
     * @param a account that does the tapping, to attach the correct identity to the tapp
     */
    fun tapp(a: Account) {
        Log.i("GroupViewModel", "tapped group! ${selectedGroup.value.id}")
        val t = Tapp(selectedGroup.value.id, System.currentTimeMillis(), a)
        _tapps.value = listOf(t) + _tapps.value
        viewModelScope.launch {
            try {
                val response = groupService.createTapp(_tokenGetter(), selectedGroup.value.id)
                if (!response.isSuccessful) {
                    Log.e("GroupViewModel", "failed to tapp group ${selectedGroup.value.id}")
                }
            } catch (e: Exception) {
                Log.e(
                    "GroupViewModel",
                    "caught exception trying to tapp: ${e.toString()}. This is most likely a rate issue since the DB is slow."
                )
            }
        }
    }

    /**
     * List tapps from the cloud. Returns the stateflow immediately which will be populated with
     * tapps when the backend responds.
     *
     * @return the tapp state flow
     */
    fun listTapps(): StateFlow<List<Tapp>> {
        Log.i("GroupViewModel", "listing tapps for group ${selectedGroup.value.id}")
        viewModelScope.launch {
            try {
                val response = groupService.listTapps(_tokenGetter(), selectedGroup.value.id)
                if (response.isSuccessful) {
                    _tapps.value = sortedTapps(response.body()!!)
                    Log.i("GroupViewModel", "listed ${_tapps.value.size} tapps!")
                } else {
                    Log.e(
                        "GroupViewModel",
                        "failed to list tapps for group ${selectedGroup.value.id}"
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "GroupViewModel",
                    "caught exception trying to list tapps: ${e.toString()}. This is most likely a rate issue since the DB is slow."
                )
            }
        }

        return tapps
    }

    /**
     * List tapp groups. Returns the stateflow immediately which will be populated with
     * tapps when the backend responds.
     *
     * @return the group state flow
     */
    fun list(): StateFlow<List<TappGroup>> {
        Log.i("GroupViewModel", "listing groups")
        viewModelScope.launch {
            try {
                val response = groupService.listGroups(_tokenGetter())
                if (response.isSuccessful) {
                    updateGroups(response.body()!!)
                } else {
                    Log.e("GroupViewModel", "failed to fetch groups!")
                }
            } catch (e: Exception) {
                Log.e(
                    "GroupViewModel",
                    "caught exception trying to list groups: ${e.toString()}. This is most likely a rate issue since the DB is slow."
                )
            }
        }

        return groups
    }

    /**
     * Refresh the currently selected group. Returns the stateflow immediately which will be populated with
     * tapps when the backend responds.
     *
     * @return the selected group state flow
     */
    fun refreshSelectedGroup(): StateFlow<TappGroup> {
        Log.i("GroupViewModel", "refreshing the selected group")
        viewModelScope.launch {
            try {
                val response = groupService.getGroup(_tokenGetter(), _selectedGroup.value.id)
                if (response.isSuccessful) {
                    _selectedGroup.value = response.body()!!
                } else {
                    Log.e("GroupViewModel", "failed to refresh selected group")
                }
            } catch (e: Exception) {
                Log.e(
                    "GroupViewModel",
                    "caught exception trying to refresh the selected group: ${e.toString()}. This is most likely a rate issue since the DB is slow."
                )
            }
        }

        return _selectedGroup
    }

    /**
     * Select group. Selecting a group centers it for handling across the application. This is
     * typically done prior to navigation to group detail screens.
     *
     * @param group to select
     */
    fun selectGroup(group: TappGroup) {
        _selectedGroup.value = group
    }

    /**
     * Delete the input group.
     *
     * @param tappGroup to delete
     * @param onSuccess callback
     * @param onFailure callback
     */
    fun delete(tappGroup: TappGroup, onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("GroupViewModel", "deleting group ${tappGroup.id}: ${tappGroup.name}")
        viewModelScope.launch {
            try {
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
            } catch (e: Exception) {
                Log.e(
                    "GroupViewModel",
                    "caught exception trying to delete a group: ${e.toString()}. This is most likely a rate issue since the DB is slow."
                )
                onFailure()
            }
        }
    }

    /**
     * Save the input group. The group is updated if its ID != 0, otherwise created.
     *
     * @param tappGroup to save
     * @param onSuccess callback
     * @param onFailure callback
     */
    fun save(tappGroup: TappGroup, onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("GroupViewModel", "saving group ${tappGroup.id}: ${tappGroup.name}")
        if (tappGroup.id == 0) {
            create(tappGroup, onSuccess, onFailure)
        } else {
            update(tappGroup, onSuccess, onFailure)
        }
    }

    /**
     * Invite the input email to the group.
     *
     * @param email
     * @param group
     * @param onSuccess callback
     * @param onFailure callback
     */
    fun inviteToGroup(
        email: String,
        group: TappGroup,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        Log.i("GroupViewModel", "inviting user $email to group $group.id")
        viewModelScope.launch {
            try {
                val response = groupService.inviteToGroup(_tokenGetter(), group.id, email)
                if (response.isSuccessful) {
                    list()
                    onSuccess()
                } else {
                    Log.e("GroupViewModel", "failed to invite $email to group $group.id!")
                    onFailure()
                }
            } catch (e: Exception) {
                Log.e(
                    "GroupViewModel",
                    "caught exception trying to invite to group: ${e.toString()}. This is most likely a rate issue since the DB is slow."
                )
                onFailure()
            }
        }
    }

    /**
     * List invitations. Returns the stateflow immediately which will be populated with
     * tapps when the backend responds.
     *
     * @return
     */
    fun listInvitations(): StateFlow<List<TappGroupInvitation>> {
        Log.i("GroupViewModel", "listing group invitations")
        viewModelScope.launch {
            try {
                val response = groupService.listInvitations(_tokenGetter())
                if (response.isSuccessful) {
                    updateInvitations(response.body()!!)
                } else {
                    Log.e("GroupViewModel", "failed to list invitations!")
                }
            } catch (e: Exception) {
                Log.e(
                    "GroupViewModel",
                    "caught exception trying to list invitations: ${e.toString()}. This is most likely a rate issue since the DB is slow."
                )
            }
        }

        return invitations
    }

    /**
     * Accept invitation to a group.
     *
     * @param invite
     * @param onSuccess callback
     * @param onFailure callback
     */
    fun acceptInvitation(
        invite: TappGroupInvitation,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        Log.i("GroupViewModel", "accepting group invitation ${invite.groupId}")
        viewModelScope.launch {
            try {
                val response = groupService.joinGroup(_tokenGetter(), invite.groupId)
                if (response.isSuccessful) {
                    onSuccess()
                    removeInvitation(invite.groupId)

                    // Trigger group-listing, implies a UI component is monitoring the group state flow.
                    list()
                } else {
                    Log.e("GroupViewModel", "failed to accept invitation ${invite.groupId}!")
                    onFailure()
                }
            } catch (e: Exception) {
                Log.e(
                    "GroupViewModel",
                    "caught exception trying to accept invitation: ${e.toString()}. This is most likely a rate issue since the DB is slow."
                )
                onFailure()
            }
        }
    }

    /**
     * Decline invitation
     *
     * @param invite
     * @param onSuccess callback
     * @param onFailure callback
     */
    fun declineInvitation(
        invite: TappGroupInvitation,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        Log.i("GroupViewModel", "declining group invitation ${invite.groupId}")
        viewModelScope.launch {
            try {
                val response = groupService.declineGroup(_tokenGetter(), invite.groupId)
                if (response.isSuccessful) {
                    onSuccess()
                    removeInvitation(invite.groupId)
                } else {
                    Log.e("GroupViewModel", "failed to accept invitation ${invite.groupId}!")
                    onFailure()
                }
            } catch (e: Exception) {
                Log.e(
                    "GroupViewModel",
                    "caught exception trying to decline an invitation: ${e.toString()}. This is most likely a rate issue since the DB is slow."
                )
                onFailure()
            }
        }
    }

    /**
     * Leave group
     *
     * @param group
     * @param onSuccess callback
     * @param onFailure callback
     */
    fun leaveGroup(
        group: TappGroup,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        Log.i("GroupViewModel", "leaving group ${group.id}")
        viewModelScope.launch {
            try {
                val response = groupService.leaveGroup(_tokenGetter(), group.id)
                if (response.isSuccessful) {
                    onSuccess()
                    removeGroup(group)
                } else {
                    Log.e("GroupViewModel", "failed to leave group ${group.id}!")
                    onFailure()
                }
            } catch (e: Exception) {
                Log.e(
                    "GroupViewModel",
                    "caught exception trying to leave group: ${e.toString()}. This is most likely a rate issue since the DB is slow."
                )
                onFailure()
            }
        }
    }

    /**
     * Kick from group
     *
     * @param group
     * @param email
     * @param onSuccess callback
     * @param onFailure callback
     */
    fun kickFromGroup(
        group: TappGroup,
        email: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        Log.i("GroupViewModel", "kicking $email from group ${group.id}")
        viewModelScope.launch {
            try {
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
            } catch (e: Exception) {
                Log.e(
                    "GroupViewModel",
                    "caught exception trying to kick from group: ${e.toString()}. This is most likely a rate issue since the DB is slow."
                )
                onFailure()
            }
        }
    }

    /**
     * Create a group
     *
     * @param new
     * @param onSuccess callback
     * @param onFailure callback
     */
    private fun create(new: TappGroup, onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("GroupViewModel", "creating group ${new.name}")
        viewModelScope.launch {
            try {
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
            } catch (e: Exception) {
                Log.e(
                    "GroupViewModel",
                    "caught exception trying to create group: ${e.toString()}. This is most likely a rate issue since the DB is slow."
                )
                onFailure()
            }
        }
    }

    /**
     * Update a group.
     *
     * @param updatedTappGroup
     * @param onSuccess callback
     * @param onFailure callback
     */
    private fun update(updatedTappGroup: TappGroup, onSuccess: () -> Unit, onFailure: () -> Unit) {
        Log.i("GroupViewModel", "updating group ${updatedTappGroup.id}: ${updatedTappGroup.name}")
        viewModelScope.launch {
            try {
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
            } catch (e: Exception) {
                Log.e(
                    "GroupViewModel",
                    "caught exception trying to update group: ${e.toString()}. This is most likely a rate issue since the DB is slow."
                )
                onFailure()
            }
        }
    }

    /**
     * Update the groups state flow with a new value.
     *
     * @param remoteGroups to replace current list
     */
    private fun updateGroups(remoteGroups: List<TappGroup>) {
        Log.i("GroupViewModel", "setting ${remoteGroups.size} groups")
        _groups.value = remoteGroups
    }

    /**
     * Remove group from the internal state flow
     *
     * @param group to remove
     */
    private fun removeGroup(group: TappGroup) {
        Log.i("GroupViewModel", "removing local group ${group.id}")
        _groups.value = _groups.value.filter { g -> g.id != group.id }
    }

    /**
     * Update the invitations state flow with a fetched list
     *
     * @param remoteInvitations
     */
    private fun updateInvitations(remoteInvitations: List<TappGroupInvitation>) {
        Log.i("GroupViewModel", "setting ${remoteInvitations.size} invitations")
        _invitations.value = remoteInvitations
    }

    /**
     * Remove invitation from the local state flow
     *
     * @param groupID of the invitation to remove
     */
    private fun removeInvitation(groupID: Int) {
        Log.i("GroupViewModel", "removing local invitation $groupID")
        _invitations.value = _invitations.value.filter { i -> i.groupId != groupID }
    }
}