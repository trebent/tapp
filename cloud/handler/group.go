//nolint:errcheck,gosec
package handler

import (
	"fmt"
	"net/http"
	"regexp"
	"slices"
	"strconv"
	"strings"
	"time"

	"github.com/trebent/tapp-backend/db"
	"github.com/trebent/tapp-backend/firebase"
	"github.com/trebent/tapp-backend/model"
	"github.com/trebent/zerologr"
)

var regexGroupName = regexp.MustCompile(`^[a-zA-Z0-9 _-]{3,30}$`)

func handleGroupCreate(w http.ResponseWriter, r *http.Request) {
	newGroup, err := model.Deserialize(r.Body, &model.Group{})
	if err != nil {
		zerologr.Error(err, "failed to deserialize group")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonSerErr)
		return
	}

	if !regexGroupName.MatchString(newGroup.Name) {
		zerologr.Error(err, "group name is bad")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	db.AquireTableLock[*model.Group]()
	defer db.ReleaseTableLock[*model.Group]()

	newGroup.ID = db.NextID[*model.Group]()
	newGroup.Name = strings.TrimSpace(newGroup.Name)
	newGroup.Owner = getUserEmailFromToken(r)

	//nolint:gosec,govet
	if err := db.Save(newGroup); err != nil {
		zerologr.Error(err, "save new group to DB failed")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}

	w.WriteHeader(http.StatusCreated)
	//nolint:gosec,govet
	if err := model.WriteJSON(w, newGroup); err != nil {
		zerologr.Error(err, "failed to write new group to response body")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonSerErr)
		return
	}
}

func handleGroupList(w http.ResponseWriter, r *http.Request) {
	groups, err := db.ReadAll[*model.Group]()
	if err != nil {
		zerologr.Error(err, "failed to read all groups from DB")
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	email := getUserEmailFromToken(r)
	filteredGroups := slices.DeleteFunc(groups, func(g *model.Group) bool {
		return g.Owner != email &&
			!slices.ContainsFunc(g.Members, func(a *model.Account) bool { return a.Email == email })
	})

	//nolint:gosec,govet
	if err := model.WriteJSON(w, filteredGroups); err != nil {
		zerologr.Error(err, "failed to write all groups to response body")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonSerErr)
		return
	}
}

func handleGroupGet(w http.ResponseWriter, r *http.Request) {
	groupID := r.URL.Path[len("/groups/"):]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		zerologr.Error(err, "failed to convert path parameter to integer")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	group, err := db.Read(&model.Group{ID: i})
	if err != nil {
		zerologr.Error(err, "group not found")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	email := getUserEmailFromToken(r)
	isMember := email == group.Owner ||
		slices.ContainsFunc(group.Members, func(a *model.Account) bool { return a.Email == email })

	if !isMember {
		zerologr.Error(err, "user is not a member of the group")
		w.WriteHeader(http.StatusForbidden)
		return
	}

	//nolint:gosec,govet
	if err := model.WriteJSON(w, group); err != nil {
		zerologr.Error(err, "failed to write group to response body")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonSerErr)
		return
	}
}

func handleGroupUpdate(w http.ResponseWriter, r *http.Request) {
	groupID := r.URL.Path[len("/groups/"):]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		zerologr.Error(err, "failed to convert path parameter to integer")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	var updatedGroup *model.Group
	db.AquireTableLock[*model.Group]()
	defer db.ReleaseTableLock[*model.Group]()

	existingGroup, err := db.Read(&model.Group{ID: i})
	if err != nil {
		zerologr.Error(err, "existing group not found")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	email := getUserEmailFromToken(r)
	if email != existingGroup.Owner {
		zerologr.Error(err, "user is not the owner of the group")
		w.WriteHeader(http.StatusForbidden)
		return
	}

	updatedGroup, err = model.Deserialize(r.Body, &model.Group{})
	if err != nil {
		zerologr.Error(err, "failed to deserialize the group")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonSerErr)
		return
	}

	if !regexGroupName.MatchString(updatedGroup.Name) {
		zerologr.Error(err, "group name is invalid")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	// Don't allow changing ownership, complicates things.
	if updatedGroup.Owner != existingGroup.Owner {
		zerologr.Error(err, "attempted to change the group owner")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	updatedGroup.ID = existingGroup.ID
	updatedGroup.Name = strings.TrimSpace(updatedGroup.Name)
	updatedGroup.Members = existingGroup.Members
	updatedGroup.Invites = existingGroup.Invites

	//nolint:gosec,govet
	if err := db.Save(updatedGroup); err != nil {
		zerologr.Error(err, "failed to save updated group to DB")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}

	//nolint:gosec,govet
	if err := model.WriteJSON(w, updatedGroup); err != nil {
		zerologr.Error(err, "failed to write updated group to response body")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonSerErr)
		return
	}
}

func handleGroupDelete(w http.ResponseWriter, r *http.Request) {
	groupID := r.URL.Path[len("/groups/"):]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		zerologr.Error(err, "failed to convert path parameter to integer")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	db.AquireTableLock[*model.Group]()
	defer db.ReleaseTableLock[*model.Group]()

	existingGroup, err := db.Read(&model.Group{ID: i})
	if err != nil {
		zerologr.Error(err, "group does not exist")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	email := getUserEmailFromToken(r)
	if email != existingGroup.Owner {
		zerologr.Error(err, "user is not the owner of the group")
		w.WriteHeader(http.StatusForbidden)
		return
	}

	db.AquireTableLock[*model.Invitation]()
	defer db.ReleaseTableLock[*model.Invitation]()

	invites, err := db.ReadAll[*model.Invitation]()
	if err != nil {
		zerologr.Error(err, "failed to read invitations")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}

	for _, invite := range invites {
		if invite.GroupID == existingGroup.ID {
			_ = db.Delete(invite)
		}
	}

	//nolint:gosec,govet
	if err := db.SimpleClear(&model.Tapp{GroupID: existingGroup.ID}); err != nil {
		zerologr.Error(err, "failed to delete tapps related to group from DB")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}

	w.WriteHeader(http.StatusNoContent)
	//nolint:gosec,govet
	if err := db.Delete(existingGroup); err != nil {
		zerologr.Error(err, "failed to delete group from DB")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}
}

func handleGroupInvite(w http.ResponseWriter, r *http.Request) {
	groupID := strings.Split(r.URL.Path, "/")[2]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		zerologr.Error(err, "failed to convert path parameter into integer")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	db.AquireTableLock[*model.Group]()
	defer db.ReleaseTableLock[*model.Group]()

	existingGroup, err := db.Read(&model.Group{ID: i})
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		return
	}

	email := getUserEmailFromToken(r)
	if email != existingGroup.Owner {
		zerologr.Error(err, "user is not the owner of the group")
		w.WriteHeader(http.StatusForbidden)
		return
	}

	invitedEmail := r.URL.Query().Get("email")
	if invitedEmail == "" {
		zerologr.Error(err, "no invitation email found in query parameters")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	invitedAccount, err := db.Read(&model.Account{Email: invitedEmail})
	if err != nil {
		zerologr.Error(err, "no email found matching the invited email")
		w.WriteHeader(http.StatusNotFound)
		w.Write(jsonFormatErr)
		return
	}

	if !slices.ContainsFunc(
		existingGroup.Invites,
		func(a *model.Account) bool { return a.Email == invitedEmail },
	) &&
		!slices.ContainsFunc(
			existingGroup.Members,
			func(a *model.Account) bool { return a.Email == invitedEmail },
		) {
		existingGroup.Invites = append(existingGroup.Invites, &model.Account{Email: invitedEmail})

		go firebase.SendIndividual(&firebase.TappNotification{
			Title: fmt.Sprintf(
				"You have been invited to the group %s!", existingGroup.Name,
			),
			Body: fmt.Sprintf(
				"%s has invited you to join the group %s!", email, existingGroup.Name,
			),
			Time:    time.Now().UnixMilli(),
			Group:   existingGroup,
			Account: invitedAccount,
		})

		db.AquireTableLock[*model.Invitation]()
		defer db.ReleaseTableLock[*model.Invitation]()

		//nolint:govet,gosec
		if err := db.Save(&model.Invitation{
			GroupID:   existingGroup.ID,
			GroupName: existingGroup.Name,
			Email:     invitedEmail,
		}); err != nil {
			zerologr.Error(err, "failed to save invitation")
			w.WriteHeader(http.StatusInternalServerError)
			w.Write(jsonDBErr)
			return
		}

		//nolint:gosec,govet
		if err := db.Save(existingGroup); err != nil {
			zerologr.Error(err, "save to DB failed")
			w.WriteHeader(http.StatusInternalServerError)
			w.Write(jsonDBErr)
			return
		}
	}

	w.WriteHeader(http.StatusNoContent)
}

func handleGroupJoin(w http.ResponseWriter, r *http.Request) {
	groupID := strings.Split(r.URL.Path, "/")[2]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		zerologr.Error(err, "failed to convert path parameter to integer")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	db.AquireTableLock[*model.Group]()
	defer db.ReleaseTableLock[*model.Group]()

	existingGroup, err := db.Read(&model.Group{ID: i})
	if err != nil {
		zerologr.Error(err, "group not found")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	email := getUserEmailFromToken(r)
	isInvited := slices.ContainsFunc(
		existingGroup.Invites, func(a *model.Account) bool { return a.Email == email },
	)

	if !isInvited {
		zerologr.Error(err, "user was not invited to the group")
		w.WriteHeader(http.StatusForbidden)
		return
	}

	invitedAccount, err := db.Read(&model.Account{Email: email})
	if err != nil {
		zerologr.Error(err, "no email found matching the email")
		w.WriteHeader(http.StatusNotFound)
		w.Write(jsonFormatErr)
		return
	}

	existingGroup.Members = append(existingGroup.Members, &model.Account{Email: email})
	existingGroup.Invites = slices.DeleteFunc(
		existingGroup.Invites, func(a *model.Account) bool { return a.Email == email },
	)

	go firebase.SendMulticast(&firebase.TappNotification{
		Title: fmt.Sprintf(
			"%s has joined the group %s!", invitedAccount.UserIdentifier(), existingGroup.Name,
		),
		Body: fmt.Sprintf(
			"%s has accepted the invitation to join the group %s!",
			invitedAccount.UserIdentifier(),
			existingGroup.Name,
		),
		Time:    time.Now().UnixMilli(),
		Group:   existingGroup,
		Account: invitedAccount,
	})

	db.AquireTableLock[*model.Invitation]()
	defer db.ReleaseTableLock[*model.Invitation]()

	//nolint:govet,gosec
	if err := db.Delete(&model.Invitation{GroupID: existingGroup.ID, Email: email}); err != nil {
		zerologr.Error(err, "failed to delete invitation")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}

	w.WriteHeader(http.StatusNoContent)
	//nolint:gosec,govet
	if err := db.Save(existingGroup); err != nil {
		zerologr.Error(err, "failed to save group to DB")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}
}

func handleGroupDecline(w http.ResponseWriter, r *http.Request) {
	groupID := strings.Split(r.URL.Path, "/")[2]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		zerologr.Error(err, "failed to convert path parameter to integer")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	db.AquireTableLock[*model.Group]()
	defer db.ReleaseTableLock[*model.Group]()

	existingGroup, err := db.Read(&model.Group{ID: i})
	if err != nil {
		zerologr.Error(err, "group not found")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	email := getUserEmailFromToken(r)
	isInvited := slices.ContainsFunc(
		existingGroup.Invites, func(a *model.Account) bool { return a.Email == email },
	)

	if !isInvited {
		zerologr.Error(err, "user was not invited to the group")
		w.WriteHeader(http.StatusForbidden)
		return
	}

	existingGroup.Invites = slices.DeleteFunc(
		existingGroup.Invites, func(a *model.Account) bool { return a.Email == email },
	)

	db.AquireTableLock[*model.Invitation]()
	defer db.ReleaseTableLock[*model.Invitation]()

	//nolint:govet,gosec
	if err := db.Delete(&model.Invitation{GroupID: existingGroup.ID, Email: email}); err != nil {
		zerologr.Error(err, "failed to delete invitation")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}

	w.WriteHeader(http.StatusNoContent)
	//nolint:gosec,govet
	if err := db.Save(existingGroup); err != nil {
		zerologr.Error(err, "failed to save group to DB")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}
}

func handleGroupLeave(w http.ResponseWriter, r *http.Request) {
	groupID := strings.Split(r.URL.Path, "/")[2]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		zerologr.Error(err, "failed to convert path parameter to integer")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	db.AquireTableLock[*model.Group]()
	defer db.ReleaseTableLock[*model.Group]()

	existingGroup, err := db.Read(&model.Group{ID: i})
	if err != nil {
		zerologr.Error(err, "group not found")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	email := getUserEmailFromToken(r)
	isMember := slices.ContainsFunc(
		existingGroup.Members, func(a *model.Account) bool { return a.Email == email },
	)

	if !isMember {
		zerologr.Error(err, "user is not a member of the group")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	leavingAccount, err := db.Read(&model.Account{Email: email})
	if err != nil {
		zerologr.Error(err, "no email found matching the email")
		w.WriteHeader(http.StatusNotFound)
		w.Write(jsonFormatErr)
		return
	}

	go firebase.SendMulticast(&firebase.TappNotification{
		Title: fmt.Sprintf(
			"%s has left the group %s!",
			leavingAccount.UserIdentifier(),
			existingGroup.Name,
		),
		Body: fmt.Sprintf(
			"%s has decided to leave the group %s!",
			leavingAccount.UserIdentifier(),
			existingGroup.Name,
		),
		Time:    time.Now().UnixMilli(),
		Group:   existingGroup,
		Account: leavingAccount,
	})

	existingGroup.Members = slices.DeleteFunc(
		existingGroup.Members, func(a *model.Account) bool { return a.Email == email },
	)

	w.WriteHeader(http.StatusNoContent)
	//nolint:gosec,govet
	if err := db.Save(existingGroup); err != nil {
		zerologr.Error(err, "saving the group to DB failed")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}
}

func handleGroupKick(w http.ResponseWriter, r *http.Request) {
	groupID := strings.Split(r.URL.Path, "/")[2]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		zerologr.Error(err, "failed to convert path parameter to integer")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	db.AquireTableLock[*model.Group]()
	defer db.ReleaseTableLock[*model.Group]()
	db.AquireTableLock[*model.Invitation]()
	defer db.ReleaseTableLock[*model.Invitation]()

	existingGroup, err := db.Read(&model.Group{ID: i})
	if err != nil {
		zerologr.Error(err, "group not found")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	email := getUserEmailFromToken(r)
	if email != existingGroup.Owner {
		zerologr.Error(err, "user is not the group owner")
		w.WriteHeader(http.StatusForbidden)
		return
	}

	kickedEmail := r.URL.Query().Get("email")
	if kickedEmail == "" {
		zerologr.Error(err, "no email to kick found in query parameters")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	foundMember := slices.ContainsFunc(
		existingGroup.Members,
		func(a *model.Account) bool { return a.Email == kickedEmail },
	)
	if !foundMember {
		zerologr.Error(err, "group has no member with that email")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	kickedAccount, err := db.Read(&model.Account{Email: email})
	if err != nil {
		zerologr.Error(err, "no email found matching the email")
		w.WriteHeader(http.StatusNotFound)
		w.Write(jsonFormatErr)
		return
	}

	go firebase.SendMulticast(&firebase.TappNotification{
		Title:   fmt.Sprintf("%s has been kicked from the group %s!", kickedAccount.UserIdentifier(), existingGroup.Name),
		Body:    fmt.Sprintf("%s has been kicked from the group %s!", kickedAccount.UserIdentifier(), existingGroup.Name),
		Time:    time.Now().UnixMilli(),
		Group:   existingGroup,
		Account: &model.Account{Email: email},
	})

	existingGroup.Members = slices.DeleteFunc(
		existingGroup.Members, func(a *model.Account) bool { return a.Email == kickedEmail },
	)

	w.WriteHeader(http.StatusNoContent)
	//nolint:gosec,govet
	if err := db.Save(existingGroup); err != nil {
		zerologr.Error(err, "save group to DB failed")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}
}

func handleGroupInvitesList(w http.ResponseWriter, r *http.Request) {
	db.AquireTableLock[*model.Invitation]()
	defer db.ReleaseTableLock[*model.Invitation]()

	email := getUserEmailFromToken(r)

	invites, err := db.ReadAll[*model.Invitation]()
	if err != nil {
		zerologr.Error(err, "failed to read from invitations table")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}

	filteredInvites := slices.DeleteFunc(invites, func(i *model.Invitation) bool {
		return i.Email != email
	})

	//nolint:gosec,govet
	if err := model.WriteJSON(w, filteredInvites); err != nil {
		zerologr.Error(err, "failed to serialize invitations")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonSerErr)
		return
	}
}
