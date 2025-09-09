/**
 * This file contains UI composables for the home screen, where groups are listed, and the user
 * can create new groups. The navigation also allows the user to visit their account management
 * screen.
 */
package com.github.trebent.tapp.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.trebent.tapp.R
import com.github.trebent.tapp.api.TappGroup
import com.github.trebent.tapp.api.TappGroupInvitation
import com.github.trebent.tapp.viewmodel.TappGroupViewModel
import com.github.trebent.tapp.viewmodel.newTappGroup
import com.github.trebent.tapp.viewmodel.testGroups
import com.github.trebent.tapp.viewmodel.testInvites
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


const val viewGroups = "groups"
const val viewInvitations = "invitations"

/**
 * Home screen route
 *
 * @param tappGroupViewModel
 * @param goToViewGroup
 * @param goToEditGroup
 * @param goToAccount
 */
@ExperimentalMaterial3Api
@Composable
fun HomeScreenRoute(
    tappGroupViewModel: TappGroupViewModel,
    goToViewGroup: (TappGroup) -> Unit,
    goToEditGroup: (TappGroup) -> Unit,
    goToAccount: () -> Unit,
) {
    Log.i("HomeScreenRoute", "navigated")

    HomeScreen(
        viewGroups,
        tappGroupViewModel.list(),
        { tappGroupViewModel.list() },
        tappGroupViewModel.listInvitations(),
        { tappGroupViewModel.listInvitations() },
        { i, s, f -> tappGroupViewModel.acceptInvitation(i, s, f) },
        { i, s, f -> tappGroupViewModel.declineInvitation(i, s, f) },
        goToViewGroup,
        goToEditGroup,
        goToAccount,
    )
}

/**
 * Home screen
 *
 * @param currentViewDefault
 * @param grps
 * @param relistGroups
 * @param invis
 * @param relistInvitations
 * @param acceptInvitation
 * @param declineInvitation
 * @param goToViewGroup
 * @param goToEditGroup
 * @param goToAccount
 */
@ExperimentalMaterial3Api
@Composable
fun HomeScreen(
    currentViewDefault: String,
    grps: StateFlow<List<TappGroup>>,
    relistGroups: () -> Unit,
    invis: StateFlow<List<TappGroupInvitation>>,
    relistInvitations: () -> Unit,
    acceptInvitation: (invite: TappGroupInvitation, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    declineInvitation: (invite: TappGroupInvitation, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    goToViewGroup: (TappGroup) -> Unit,
    goToEditGroup: (TappGroup) -> Unit,
    goToAccount: () -> Unit,
) {
    Log.i("HomeScreen", "rendering")

    val groups by grps.collectAsState()
    val invitations by invis.collectAsState()
    var currentView by rememberSaveable { mutableStateOf(currentViewDefault) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = {
                        Log.i("HomeScreen", "clicked account icon")
                        goToAccount()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Go to account"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentView == viewGroups,
                    onClick = {
                        currentView = viewGroups
                        relistGroups()
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.groups),
                            contentDescription = "Groups"
                        )
                    },
                    label = { Text("My groups") }
                )
                NavigationBarItem(
                    selected = currentView == viewInvitations,
                    onClick = {
                        currentView = viewInvitations
                        relistInvitations()
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.group_add),
                            contentDescription = "Invitations"
                        )
                    },
                    label = { Text("Invitations") }
                )
            }
        },
        floatingActionButton = {
            if (currentView == viewGroups) {
                FloatingActionButton(onClick = {
                    Log.i("HomeScreen", "clicked main create group FAB")
                    goToEditGroup(newTappGroup)
                }) {
                    Icon(
                        imageVector = Icons.Filled.AddCircle,
                        contentDescription = "Go to account"
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                if (currentView == viewGroups) {
                    if (groups.isEmpty()) {
                        item(span = { GridItemSpan(4) }) {
                            Text(
                                text = "You are not part of any groups yet",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp)
                            )
                        }
                    } else {
                        items(groups, span = { GridItemSpan(4) }) { tappGroup ->
                            TappGroupRow(tappGroup, goToViewGroup)
                        }
                    }
                } else {
                    if (!invitations.isEmpty()) {
                        items(invitations, span = { GridItemSpan(4) }) { invite ->
                            TappGroupInviteRow(invite, acceptInvitation, declineInvitation)
                        }
                    } else {
                        item(span = { GridItemSpan(4) }) {
                            Text(
                                text = "There are no pending invitations",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tapp group row, a composable showing each tapp group the user is a member or owner of.
 *
 * @param tappGroup
 * @param openGroup
 */
@Composable
fun TappGroupRow(tappGroup: TappGroup, openGroup: (tappGroup: TappGroup) -> Unit) {
    OutlinedButton(
        shape = RoundedCornerShape(8.dp),
        onClick = {
            Log.i("HomeScreen", "opening group ${tappGroup.id}: ${tappGroup.name}")
            openGroup(tappGroup)
        },
        modifier = Modifier
            .padding(bottom = 8.dp)
            .heightIn(min = 56.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                tappGroup.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Text(
                "${tappGroup.memberCount()}" + " members",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Tapp group invite row, a composable showing each tapp group the user is invited to join.
 *
 * @param invite
 * @param accept
 * @param decline
 */
@Composable
fun TappGroupInviteRow(
    invite: TappGroupInvitation,
    accept: (invite: TappGroupInvitation, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    decline: (invite: TappGroupInvitation, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(bottom = 8.dp)
            .heightIn(min = 56.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = 24.dp, end = 16.dp)
        ) {
            Text(
                invite.groupName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight()
            ) {
                IconButton(
                    onClick = {
                        accept(invite, {
                            Log.i("HomeScreen", "accepted invitation successfully!")
                        }, {
                            Log.e("HomeScreen", "invitation failed to be accepted")
                            Toast.makeText(
                                context,
                                "Invitation could not be accepted, please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        })
                    },
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = "accept invitation",
                    )
                }
                IconButton(
                    onClick = {
                        decline(invite, {
                            Log.i("HomeScreen", "declined invitation successfully!")
                        }, {
                            Log.e("HomeScreen", "invitation failed to be declined")
                            Toast.makeText(
                                context,
                                "Invitation could not be declined, please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        })
                    },
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "decline invitation",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

        }

    }
}

/**
 * Home screen my groups preview
 *
 */
@ExperimentalMaterial3Api
@Preview
@Composable
fun HomeScreenMyGroupsPreview() {
    HomeScreen(
        viewGroups,
        MutableStateFlow(testGroups),
        {},
        MutableStateFlow(testInvites),
        {},
        { _, _, _ -> },
        { _, _, _ -> },
        {},
        {},
        {},
    )
}

/**
 * Home screen no groups preview
 *
 */
@ExperimentalMaterial3Api
@Preview
@Composable
fun HomeScreenNoGroupsPreview() {
    HomeScreen(
        viewGroups,
        MutableStateFlow(emptyList()),
        {},
        MutableStateFlow(testInvites),
        {},
        { _, _, _ -> },
        { _, _, _ -> },
        {},
        {},
        {},
    )
}

/**
 * Home screen invitations preview
 *
 */
@ExperimentalMaterial3Api
@Preview
@Composable
fun HomeScreenInvitationsPreview() {
    HomeScreen(
        viewInvitations,
        MutableStateFlow(testGroups),
        {},
        MutableStateFlow(testInvites),
        {},
        { _, _, _ -> },
        { _, _, _ -> },
        {},
        {},
        {},
    )
}

/**
 * Home screen no invitations preview
 *
 */
@ExperimentalMaterial3Api
@Preview
@Composable
fun HomeScreenNoInvitationsPreview() {
    HomeScreen(
        viewInvitations,
        MutableStateFlow(testGroups),
        {},
        MutableStateFlow(emptyList()),
        {},
        { _, _, _ -> },
        { _, _, _ -> },
        {},
        {},
        {},
    )
}
