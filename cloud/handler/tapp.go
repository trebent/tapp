package handler

import (
	"net/http"
	"slices"
	"strconv"

	"github.com/trebent/tapp-backend/db"
	"github.com/trebent/tapp-backend/model"
)

func handleTapp(w http.ResponseWriter, r *http.Request) {
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

	isMember := email == group.Owner ||
		slices.ContainsFunc(group.Members, func(a *model.Account) bool { return a.Email == email })
	if !isMember {
		w.WriteHeader(http.StatusForbidden)
		return
	}

	db.AquireTableLock[*model.Tapp]()
	defer db.ReleaseTableLock[*model.Tapp]()

	newTapp := &model.Tapp{
		ID:      db.NextID[*model.Tapp](),
		GroupID: group.ID,
		User:    &model.Account{Email: email},
	}

	w.WriteHeader(http.StatusNoContent)
	//nolint:gosec,govet
	if err := db.Save(newTapp); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	// TODO: dispatch notification to group members
	// TODO: dispatch notification to group members
	// TODO: dispatch notification to group members
	// TODO: dispatch notification to group members
}

func handleTappGet(w http.ResponseWriter, r *http.Request) {
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

	isMember := email == group.Owner ||
		slices.ContainsFunc(group.Members, func(a *model.Account) bool { return a.Email == email })
	if !isMember {
		w.WriteHeader(http.StatusForbidden)
		return
	}

	tapps, err := db.ReadAll[*model.Tapp]()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	//nolint:gosec,govet
	if err := model.WriteJSON(w, tapps); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}
