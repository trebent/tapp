package handler

import (
	"net/http"
	"regexp"
	"slices"
	"strconv"
	"strings"

	"github.com/trebent/tapp-backend/db"
	"github.com/trebent/tapp-backend/model"
)

var regexGroupName = regexp.MustCompile(`^[a-zA-Z0-9 _-]{3,30}$`)

func handleGroupCreate(w http.ResponseWriter, r *http.Request) {
	newGroup, err := model.Deserialize(r.Body, &model.Group{})
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	if !regexGroupName.MatchString(newGroup.Name) {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	db.AquireTableLock[*model.Group]()
	defer db.ReleaseTableLock[*model.Group]()

	newGroup.ID = db.NextID[*model.Group]()
	newGroup.Name = strings.TrimSpace(newGroup.Name)
	newGroup.Owner = getUserEmailFromToken(r)

	if err := db.Save(newGroup); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	if err := model.WriteJSON(w, newGroup); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func handleGroupList(w http.ResponseWriter, r *http.Request) {
	groups, err := db.ReadAll[*model.Group]()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	email := getUserEmailFromToken(r)
	filteredGroups := slices.DeleteFunc(groups, func(g *model.Group) bool {
		return g.Owner != email &&
			!slices.ContainsFunc(g.Members, func(a *model.Account) bool { return a.Email == email })
	})

	if err := model.WriteJSON(w, filteredGroups); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func handleGroupGet(w http.ResponseWriter, r *http.Request) {
	groupID := r.URL.Path[len("/groups/"):]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	group, err := db.Read(&model.Group{ID: i})
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		return
	}

	email := getUserEmailFromToken(r)
	isMember := email == group.Owner || slices.ContainsFunc(group.Members, func(a *model.Account) bool { return a.Email == email })

	if !isMember {
		w.WriteHeader(http.StatusForbidden)
		return
	}

	if err := model.WriteJSON(w, group); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func handleGroupUpdate(w http.ResponseWriter, r *http.Request) {
	groupID := r.URL.Path[len("/groups/"):]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	var updatedGroup *model.Group
	db.AquireTableLock[*model.Group]()
	defer db.ReleaseTableLock[*model.Group]()

	existingGroup, err := db.Read(&model.Group{ID: i})
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		return
	}

	email := getUserEmailFromToken(r)
	if email != existingGroup.Owner {
		w.WriteHeader(http.StatusForbidden)
		return
	}

	updatedGroup, err = model.Deserialize(r.Body, &model.Group{})
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	if !regexGroupName.MatchString(updatedGroup.Name) {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	// Don't allow changing ownership, complicates things.
	if updatedGroup.Owner != existingGroup.Owner {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	updatedGroup.ID = existingGroup.ID
	updatedGroup.Name = strings.TrimSpace(updatedGroup.Name)

	if err := db.Save(updatedGroup); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	if err := model.WriteJSON(w, updatedGroup); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func handleGroupDelete(w http.ResponseWriter, r *http.Request) {
	groupID := r.URL.Path[len("/groups/"):]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
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
		w.WriteHeader(http.StatusForbidden)
		return
	}

	w.WriteHeader(http.StatusNoContent)
	if err := db.Delete(existingGroup); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func handleGroupInvite(w http.ResponseWriter, r *http.Request) {
	groupID := r.URL.Path[len("/groups/"):]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
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
		w.WriteHeader(http.StatusForbidden)
		return
	}

	invitedEmail := r.URL.Query().Get("email")
	if invitedEmail == "" {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	existingGroup.Invites = append(existingGroup.Invites, &model.Account{Email: invitedEmail})

	w.WriteHeader(http.StatusNoContent)
	if err := db.Save(existingGroup); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func handleGroupJoin(w http.ResponseWriter, r *http.Request) {
	groupID := r.URL.Path[len("/groups/"):]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
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
	isInvited := slices.ContainsFunc(existingGroup.Invites, func(a *model.Account) bool { return a.Email == email })

	if !isInvited {
		w.WriteHeader(http.StatusForbidden)
		return
	}

	existingGroup.Members = append(existingGroup.Members, &model.Account{Email: email})
	existingGroup.Invites = slices.DeleteFunc(existingGroup.Invites, func(a *model.Account) bool { return a.Email == email })

	w.WriteHeader(http.StatusNoContent)
	if err := db.Save(existingGroup); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func handleGroupLeave(w http.ResponseWriter, r *http.Request) {
	groupID := r.URL.Path[len("/groups/"):]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
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
	isMember := slices.ContainsFunc(existingGroup.Members, func(a *model.Account) bool { return a.Email == email })

	if !isMember {
		w.WriteHeader(http.StatusNotFound)
		return
	}

	existingGroup.Members = slices.DeleteFunc(existingGroup.Members, func(a *model.Account) bool { return a.Email == email })

	w.WriteHeader(http.StatusNoContent)
	if err := db.Save(existingGroup); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func handleGroupKick(w http.ResponseWriter, r *http.Request) {
	groupID := r.URL.Path[len("/groups/"):]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
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
		w.WriteHeader(http.StatusForbidden)
		return
	}

	kickedEmail := r.URL.Query().Get("email")
	if kickedEmail == "" {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	foundMember := slices.ContainsFunc(existingGroup.Members, func(a *model.Account) bool { return a.Email == kickedEmail })
	if !foundMember {
		w.WriteHeader(http.StatusNotFound)
		return
	}

	existingGroup.Members = slices.DeleteFunc(existingGroup.Members, func(a *model.Account) bool { return a.Email == kickedEmail })

	w.WriteHeader(http.StatusNoContent)
	if err := db.Save(existingGroup); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}
