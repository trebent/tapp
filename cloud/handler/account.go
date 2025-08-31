//nolint:errcheck,gosec
package handler

import (
	"net/http"
	"regexp"

	"github.com/trebent/tapp-backend/db"
	"github.com/trebent/tapp-backend/model"
	"github.com/trebent/zerologr"
)

var (
	regexpEmail    = regexp.MustCompile(`^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$`)
	regexpPassword = regexp.MustCompile(`^.{6,}$`)
)

func handleAccountCreate(w http.ResponseWriter, r *http.Request) {
	newAccount, err := model.Deserialize(r.Body, &model.Account{})
	if err != nil {
		zerologr.Error(err, "failed to deserialize account")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonSerErr)
		return
	}

	if !regexpEmail.MatchString(newAccount.Email) ||
		!regexpPassword.MatchString(newAccount.Password) {
		zerologr.Error(err, "account email or password format is bad")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	existingAccounts, err := db.ReadAll[*model.Account]()
	if err != nil {
		zerologr.Error(err, "failed to read all accounts from DB")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}

	for _, existingAccount := range existingAccounts {
		if existingAccount.Key() == newAccount.Key() {
			zerologr.Error(err, "account with that email already exists")
			w.WriteHeader(http.StatusConflict)
			return
		}

		if newAccount.Tag != "" && existingAccount.Tag == newAccount.Tag {
			zerologr.Error(err, "account with that tag already exists")
			w.WriteHeader(http.StatusConflict)
			return
		}
	}

	//nolint:gosec,govet
	if err := db.Save(newAccount); err != nil {
		zerologr.Error(err, "save account to DB failed")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}

	w.WriteHeader(http.StatusCreated)
	newAccount.Password = ""
	//nolint:gosec,govet
	if err := model.WriteJSON(w, newAccount); err != nil {
		zerologr.Error(err, "failed to serialize account")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonSerErr)
		return
	}
}

func handleAccountGet(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Path[len("/accounts/"):]

	account, err := db.Read(&model.Account{Email: email})
	if err != nil {
		zerologr.Error(err, "account not found")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	if account.Email != getUserEmailFromToken(r) {
		zerologr.Error(err, "that's not that user's account")
		w.WriteHeader(http.StatusForbidden)
		return
	}

	account.Password = ""
	//nolint:gosec,govet
	if err := model.WriteJSON(w, account); err != nil {
		zerologr.Error(err, "failed to serialize account")
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
}

func handleAccountUpdate(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Path[len("/accounts/"):]

	existingAccount, err := db.Read(&model.Account{Email: email})
	if err != nil {
		zerologr.Error(err, "account not found")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	updatedAccount, err := model.Deserialize(r.Body, &model.Account{})
	if err != nil {
		zerologr.Error(err, "failed to deserialize account")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonSerErr)
		return
	}
	updatedAccount.Password = existingAccount.Password

	if existingAccount.Email != updatedAccount.Email {
		zerologr.Error(err, "user tried to update email")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	//nolint:ineffassign
	accounts, err := db.ReadAll[*model.Account]()
	if err != nil {
		zerologr.Error(err, "failed to read all accounts")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}

	for _, a := range accounts {
		if updatedAccount.Tag != "" && a.Tag == updatedAccount.Tag {
			zerologr.Error(err, "that tag already exists")
			w.WriteHeader(http.StatusConflict)
			return
		}
	}

	//nolint:gosec,govet
	if err := db.Save(updatedAccount); err != nil {
		zerologr.Error(err, "failed to save updated account to DB")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}

	//nolint:gosec,govet
	if err := model.WriteJSON(w, updatedAccount); err != nil {
		zerologr.Error(err, "failed to serialize account")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonSerErr)
		return
	}
}

func handlePasswordUpdate(w http.ResponseWriter, r *http.Request) {
	email := getUserEmailFromToken(r)

	existingAccount, err := db.Read(&model.Account{Email: email})
	if err != nil {
		zerologr.Error(err, "account not found")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	acc := &model.Account{}
	if _, err := model.Deserialize(r.Body, &acc); err != nil {
		zerologr.Error(err, "failed to unmarshal password body")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonSerErr)
	}

	if !regexpPassword.MatchString(acc.Password) {
		zerologr.Error(err, "password format incorrect")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonFormatErr)
		return
	}

	existingAccount.Password = acc.Password

	//nolint:gosec,govet
	if err := db.Save(existingAccount); err != nil {
		zerologr.Error(err, "failed to save updated account to DB")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func handleAccountDelete(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Path[len("/accounts/"):]

	existingAccount, err := db.Read(&model.Account{Email: email})
	if err != nil {
		zerologr.Error(err, "failed to find account in DB")
		w.WriteHeader(http.StatusNotFound)
		return
	}

	w.WriteHeader(http.StatusNoContent)
	//nolint:gosec,govet
	if err := db.Delete(existingAccount); err != nil {
		zerologr.Error(err, "failed to delete account from DB")
		w.WriteHeader(http.StatusInternalServerError)
		w.Write(jsonDBErr)
		return
	}
	handleLogout(w, r)
}
