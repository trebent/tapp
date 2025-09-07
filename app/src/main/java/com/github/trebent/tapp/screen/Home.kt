package com.github.trebent.tapp.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
        tappGroupViewModel.list(),
        tappGroupViewModel.listInvitations(),
        { i, s, f -> tappGroupViewModel.acceptInvitation(i, s, f) },
        { i, s, f -> tappGroupViewModel.declineInvitation(i, s, f) },
        goToViewGroup,
        goToEditGroup,
        goToAccount,
    )
}

@ExperimentalMaterial3Api
@Composable
fun HomeScreen(
    grps: StateFlow<List<TappGroup>>,
    invis: StateFlow<List<TappGroupInvitation>>,
    acceptInvitation: (invite: TappGroupInvitation, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    declineInvitation: (invite: TappGroupInvitation, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    goToViewGroup: (TappGroup) -> Unit,
    goToEditGroup: (TappGroup) -> Unit,
    goToAccount: () -> Unit,
) {
    Log.i("HomeScreen", "rendering")
    val groups by grps.collectAsState()
    val invitations by invis.collectAsState()
    var currentView by rememberSaveable { mutableStateOf(viewGroups) }

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
            FloatingActionButton(onClick = {
                Log.i("HomeScreen", "clicked main create group FAB")
                goToEditGroup(newTappGroup)
            }) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "Go to account"
                )
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
                    items(groups, span = { GridItemSpan(4) }) { tappGroup ->
                        TappGroupRow(tappGroup, goToViewGroup)
                    }
                } else {
                    items(invitations, span = { GridItemSpan(4) }) { invite ->
                        TappGroupInviteRow(invite, acceptInvitation, declineInvitation)
                    }
                }
            }
        }
    }
}

@Composable
fun TappGroupRow(tappGroup: TappGroup, openGroup: (tappGroup: TappGroup) -> Unit) {
    OutlinedButton(
        shape = RoundedCornerShape(8.dp),
        onClick = {
            Log.i("HomeScreen", "opening group ${tappGroup.id}: ${tappGroup.name}")
            openGroup(tappGroup)
        },
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(tappGroup.name)
    }
}

@Composable
fun TappGroupInviteRow(
    invite: TappGroupInvitation,
    accept: (invite: TappGroupInvitation, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    decline: (invite: TappGroupInvitation, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Text(invite.groupName)
            Row {
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
                    )
                }
            }

        }
    }
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        MutableStateFlow(testGroups),
        MutableStateFlow(testInvites),
        { _, _, _ -> },
        { _, _, _ -> },
        {},
        {},
        {},
    )
}
