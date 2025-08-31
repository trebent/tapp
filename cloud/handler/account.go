package handler

import (
	"net/http"
	"regexp"

	"github.com/trebent/tapp-backend/db"
	"github.com/trebent/tapp-backend/model"
)

var (
	regexpEmail    = regexp.MustCompile(`^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$`)
	regexpPassword = regexp.MustCompile(`^.{6,}$`)
)

func handleAccountCreate(w http.ResponseWriter, r *http.Request) {
	newAccount, err := model.Deserialize(r.Body, &model.Account{})
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	if !regexpEmail.MatchString(newAccount.Email) ||
		!regexpPassword.MatchString(newAccount.Password) {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	existingAccounts, err := db.ReadAll[*model.Account]()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	for _, existingAccount := range existingAccounts {
		if existingAccount.Key() == newAccount.Key() {
			w.WriteHeader(http.StatusConflict)
			return
		}

		if newAccount.Tag != "" && existingAccount.Tag == newAccount.Tag {
			w.WriteHeader(http.StatusConflict)
			return
		}
	}

	//nolint:gosec,govet
	if err := db.Save(newAccount); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	newAccount.Password = ""
	//nolint:gosec,govet
	if err := model.WriteJSON(w, newAccount); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func handleAccountGet(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Path[len("/accounts/"):]

	account, err := db.Read(&model.Account{Email: email})
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		return
	}

	if account.Email != getUserEmailFromToken(r) {
		w.WriteHeader(http.StatusForbidden)
		return
	}

	account.Password = ""
	//nolint:gosec,govet
	if err := model.WriteJSON(w, account); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func handleAccountUpdate(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Path[len("/accounts/"):]

	existingAccount, err := db.Read(&model.Account{Email: email})
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		return
	}

	updatedAccount, err := model.Deserialize(r.Body, &model.Account{})
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	if existingAccount.Email != updatedAccount.Email {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	if !regexpPassword.MatchString(updatedAccount.Password) {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	//nolint:ineffassign
	accounts, err := db.ReadAll[*model.Account]()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	for _, a := range accounts {
		if updatedAccount.Tag != "" && a.Tag == updatedAccount.Tag {
			w.WriteHeader(http.StatusConflict)
			return
		}
	}

	//nolint:gosec,govet
	if err := db.Save(updatedAccount); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	//nolint:gosec,govet
	if err := model.WriteJSON(w, updatedAccount); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func handleAccountDelete(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Path[len("/accounts/"):]

	existingAccount, err := db.Read(&model.Account{Email: email})
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		return
	}

	w.WriteHeader(http.StatusNoContent)
	//nolint:gosec,govet
	if err := db.Delete(existingAccount); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	handleLogout(w, r)
}
