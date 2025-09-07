//nolint:errcheck,gosec
package handler

import (
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"os"
	"path/filepath"
	"sync"

	"github.com/trebent/tapp-backend/db"
	"github.com/trebent/tapp-backend/env"
	"github.com/trebent/tapp-backend/model"
	"github.com/trebent/zerologr"
)

//nolint:gochecknoglobals
var (
	authLock = sync.Mutex{}
	authBlob = map[string]string{}
)

const hashSize = 16

func Initialize() {
	authLock.Lock()
	defer authLock.Unlock()
	readAuthBlob()
	zerologr.Info("booted with auth blob", "blob", authBlob)
}

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
	authLock.Lock()
	defer authLock.Unlock()
	if val, ok := authBlob[token]; ok {
		return val
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
		zerologr.Error(err, "failed to deserialize login request")
		w.WriteHeader(http.StatusBadRequest)
		w.Write(jsonSerErr)
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

	authLock.Lock()
	defer authLock.Unlock()
	authBlob[hash] = body.Email
	writeAuthBlob()

	w.Header().Set("Authorization", hash)
	w.WriteHeader(http.StatusNoContent)
}

func handleLogout(w http.ResponseWriter, r *http.Request) {
	token := r.Header.Get("Authorization")
	authLock.Lock()
	defer authLock.Unlock()
	delete(authBlob, token)
	writeAuthBlob()
	w.WriteHeader(http.StatusNoContent)
}

func writeAuthBlob() {
	fp := filepath.Join(env.FileSystem.Value(), "authblob.json")

	data, err := json.Marshal(authBlob)
	if err != nil {
		zerologr.Error(err, "failed to serialize auth blob data")
	} else {
		//nolint:govet,gosec
		err := os.WriteFile(fp, data, 0o755)
		if err != nil {
			zerologr.Error(err, "failed to write auth blob to file")
		}
	}
}

func readAuthBlob() {
	fp := filepath.Join(env.FileSystem.Value(), "authblob.json")

	data, err := os.ReadFile(fp)
	if err != nil {
		zerologr.Error(err, "failed to read auth blob file")
	} else {
		//nolint:govet,gosec
		if err := json.Unmarshal(data, &authBlob); err != nil {
			zerologr.Error(err, "failed to write auth blob to file")
		}
	}
}

func newHash() string {
	b := make([]byte, hashSize)
	_, err := rand.Read(b)
	if err != nil {
		panic(err)
	}
	return hex.EncodeToString(b)
}
