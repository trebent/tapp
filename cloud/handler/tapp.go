//nolint:errcheck,gosec
package handler

import (
	"fmt"
	"net/http"
	"slices"
	"strconv"
	"strings"
	"time"

	"github.com/trebent/tapp-backend/db"
	"github.com/trebent/tapp-backend/firebase"
	"github.com/trebent/tapp-backend/model"
	"github.com/trebent/zerologr"
)

func handleTapp(w http.ResponseWriter, r *http.Request) {
	groupID := strings.Split(r.URL.Path, "/")[2]

	i, err := strconv.Atoi(groupID)
	if err != nil {
		zerologr.Error(err, "failed to convert path parameter to integer")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	group, err := db.Read(&model.Group{ID: i})
	if err != nil {
		zerologr.Error(err, "group to tapp not found")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	email := getUserEmailFromToken(r)

	account, err := db.Read(&model.Account{Email: email})
	if err != nil {
		zerologr.Error(err, "account not found")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	isMember := email == group.Owner ||
		slices.ContainsFunc(group.Members, func(a *model.Account) bool { return a.Email == email })
	if !isMember {
		zerologr.Error(err, "user is not a member of the group")
		w.WriteHeader(http.StatusForbidden)
		return
	}

	newTapp := &model.Tapp{
		Time:    time.Now().Local().UnixMilli(),
		GroupID: group.ID,
		User:    &model.Account{Email: email, Tag: account.Tag},
	}
	go firebase.SendMulticast(&firebase.TappNotification{
		Title:   fmt.Sprintf("Group %s was tapped!", group.Name),
		Body:    fmt.Sprintf("%s tapped group %s, tapp them back!", newTapp.User.UserIdentifier(), group.Name),
		Time:    newTapp.Time,
		Group:   group,
		Account: newTapp.User,
		Type:    "tapp",
	})

	db.SimpleAcquire(newTapp)
	defer db.SimpleRelease(newTapp)

	w.WriteHeader(http.StatusNoContent)
	//nolint:gosec,govet
	if err := db.SimpleAppend(newTapp); err != nil {
		zerologr.Error(err, "failed to save tapp to DB")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}
}

func handleTappGet(w http.ResponseWriter, r *http.Request) {
	groupID := strings.Split(r.URL.Path, "/")[2]

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

	tapps, err := db.SimpleRead(&model.Tapp{GroupID: group.ID})
	if err != nil {
		zerologr.Error(err, "failed to read all tapps from DB")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}

	//nolint:mnd
	if len(tapps) > 100 {
		//nolint:mnd
		tapps = tapps[:100]
	}
	tapps = slices.DeleteFunc(tapps, func(t *model.Tapp) bool { return t.GroupID != group.ID })

	//nolint:gosec,govet
	if err := model.WriteJSON(w, tapps); err != nil {
		zerologr.Error(err, "failed to serialize tapps")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonSerErr)
		return
	}
}
