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
		w.WriteHeader(400)
		return
	}

	if !regexpEmail.MatchString(newAccount.Email) ||
		!regexpPassword.MatchString(newAccount.Password) {
		w.WriteHeader(400)
		return
	}

	existingAccounts, err := db.ReadAll[*model.Account]()
	for _, existingAccount := range existingAccounts {
		if existingAccount.Key() == newAccount.Key() {
			w.WriteHeader(409)
			return
		}

		if newAccount.Tag != "" && existingAccount.Tag == newAccount.Tag {
			w.WriteHeader(409)
			return
		}
	}

	if err := db.Save(newAccount); err != nil {
		w.WriteHeader(500)
		return
	}

	w.WriteHeader(http.StatusCreated)
	newAccount.Password = ""
	if err := model.WriteJSON(w, newAccount); err != nil {
		w.WriteHeader(500)
		return
	}
}

func handleAccountGet(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Path[len("/accounts/"):]

	account, err := db.Read(&model.Account{Email: email})
	if err != nil {
		w.WriteHeader(404)
		return
	}

	if account.Email != getUserEmailFromToken(r) {
		w.WriteHeader(403)
		return
	}

	account.Password = ""
	if err := model.WriteJSON(w, account); err != nil {
		w.WriteHeader(500)
		return
	}
}

func handleAccountUpdate(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Path[len("/accounts/"):]

	existingAccount, err := db.Read(&model.Account{Email: email})
	if err != nil {
		w.WriteHeader(404)
		return
	}

	updatedAccount, err := model.Deserialize(r.Body, &model.Account{})
	if err != nil {
		w.WriteHeader(400)
		return
	}

	if existingAccount.Email != updatedAccount.Email {
		w.WriteHeader(400)
		return
	}

	if !regexpPassword.MatchString(updatedAccount.Password) {
		w.WriteHeader(400)
		return
	}

	accounts, err := db.ReadAll[*model.Account]()
	for _, a := range accounts {
		if updatedAccount.Tag != "" && a.Tag == updatedAccount.Tag {
			w.WriteHeader(409)
			return
		}
	}

	if err := db.Save(updatedAccount); err != nil {
		w.WriteHeader(500)
		return
	}

	if err := model.WriteJSON(w, updatedAccount); err != nil {
		w.WriteHeader(500)
		return
	}
}

func handleAccountDelete(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Path[len("/accounts/"):]

	existingAccount, err := db.Read(&model.Account{Email: email})
	if err != nil {
		w.WriteHeader(404)
		return
	}

	w.WriteHeader(http.StatusNoContent)
	if err := db.Delete(existingAccount); err != nil {
		w.WriteHeader(500)
		return
	}
	handleLogout(w, r)
}
