package handler

import (
	"crypto/rand"
	"encoding/hex"
	"net/http"
	"sync"

	"github.com/trebent/tapp-backend/db"
	"github.com/trebent/tapp-backend/model"
)

//nolint:gochecknoglobals
var authMap = sync.Map{}

const hashSize = 16

func authenticated(w http.ResponseWriter, r *http.Request) bool {
	if getUserEmailFromToken(r) == "" {
		w.WriteHeader(http.StatusUnauthorized)
		return false
	}

	return true
}

func getUserEmailFromToken(r *http.Request) string {
	token := r.Header.Get("Authorization")
	return getTokenValue(token)
}

func getTokenValue(token string) string {
	if val, ok := authMap.Load(token); ok {
		//nolint:errcheck
		return val.(string)
	}

	return ""
}

func handleLogin(w http.ResponseWriter, r *http.Request) {
	// POST
	var body struct {
		Email    string `json:"email"`
		Password string `json:"password"`
	}
	if _, err := model.Deserialize(r.Body, &body); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	account, err := db.Read(&model.Account{Email: body.Email})
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}

	if account.Password != body.Password {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}

	hash := newHash()
	authMap.Store(hash, body.Email)

	w.Header().Set("Authorization", hash)
	w.WriteHeader(http.StatusNoContent)
}

func handleLogout(w http.ResponseWriter, r *http.Request) {
	token := r.Header.Get("Authorization")
	authMap.Delete(token)
	w.WriteHeader(http.StatusNoContent)
}

func newHash() string {
	b := make([]byte, hashSize)
	_, err := rand.Read(b)
	if err != nil {
		panic(err)
	}
	return hex.EncodeToString(b)
}
