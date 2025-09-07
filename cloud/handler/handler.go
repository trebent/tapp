//nolint:gochecknoglobals
package handler

import (
	"net/http"

	"github.com/trebent/tapp-backend/db"
	"github.com/trebent/tapp-backend/env"
	"github.com/trebent/tapp-backend/model"
	"github.com/trebent/zerologr"
)

var jsonSerErr = []byte(`{"error": "serializer error"}`)
var jsonFormatErr = []byte(`{"error": "format error"}`)
var jsonDBErr = []byte(`{"error": "DB error"}`)

func logWrapper(h http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		zerologr.Info(r.Method + " " + r.URL.Path)
		h.ServeHTTP(w, r)
	})
}

//nolint:gocognit,funlen
func Handler() http.Handler {
	mux := http.NewServeMux()

	// Health endpoint
	mux.HandleFunc("/health", func(w http.ResponseWriter, _ *http.Request) {
		zerologr.Info("health check OK")
		w.WriteHeader(http.StatusNoContent)
	})

	mux.HandleFunc("/admin/debug", func(w http.ResponseWriter, r *http.Request) {
		zerologr.Info("outputing debug info...")

		if r.Header.Get("X-tapp-admin-key") != env.AdminKey.Value() {
			w.WriteHeader(http.StatusForbidden)
			return
		}

		accounts, _ := db.ReadAll[*model.Account]()
		groups, _ := db.ReadAll[*model.Group]()

		for _, group := range groups {
			_ = model.WriteJSON(w, group)
			tapps, _ := db.SimpleRead(&model.Tapp{GroupID: group.ID})
			_ = model.WriteJSON(w, tapps)
		}

		invites, _ := db.ReadAll[*model.Invitation]()

		_ = model.WriteJSON(w, accounts)
		_ = model.WriteJSON(w, invites)
	})

	mux.HandleFunc("/admin/clear", func(w http.ResponseWriter, r *http.Request) {
		zerologr.Info("clearing DB tables...")

		if r.Header.Get("X-tapp-admin-key") != env.AdminKey.Value() {
			w.WriteHeader(http.StatusForbidden)
			return
		}

		groups, _ := db.ReadAll[*model.Group]()

		for _, group := range groups {
			_ = db.SimpleClear(&model.Tapp{GroupID: group.ID})
		}

		_ = db.Clear[*model.Group]()
		_ = db.Clear[*model.Invitation]()
		_ = db.Clear[*model.Account]()

		w.WriteHeader(http.StatusNoContent)
	})

	// Account endpoints
	mux.HandleFunc("/accounts", func(w http.ResponseWriter, r *http.Request) {
		// POST
		if r.Body != nil {
			defer r.Body.Close()
		}

		if r.Method != http.MethodPost {
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}

		handleAccountCreate(w, r)
	})

	mux.HandleFunc("/password", func(w http.ResponseWriter, r *http.Request) {
		// POST
		if r.Body != nil {
			defer r.Body.Close()
		}

		if !authenticated(w, r) {
			return
		}

		if r.Method != http.MethodPost {
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}

		handlePasswordUpdate(w, r)
	})

	mux.HandleFunc("/accounts/{email}", func(w http.ResponseWriter, r *http.Request) {
		// GET, PUT, DELETE
		if r.Body != nil {
			defer r.Body.Close()
		}

		if !authenticated(w, r) {
			return
		}

		switch r.Method {
		case http.MethodGet:
			handleAccountGet(w, r)
		case http.MethodPut:
			handleAccountUpdate(w, r)
		case http.MethodDelete:
			handleAccountDelete(w, r)
		default:
			w.WriteHeader(http.StatusMethodNotAllowed)
		}
	})

	// Auth endpoints
	mux.HandleFunc("/login", func(w http.ResponseWriter, r *http.Request) {
		// POST
		if r.Body != nil {
			defer r.Body.Close()
		}

		if r.Method != http.MethodPost {
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}
		handleLogin(w, r)
	})

	mux.HandleFunc("/logout", func(w http.ResponseWriter, r *http.Request) {
		// POST
		if r.Method != http.MethodPost {
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}

		handleLogout(w, r)
	})

	// Group endpoints
	mux.HandleFunc("/groups", func(w http.ResponseWriter, r *http.Request) {
		// POST, GET
		if r.Body != nil {
			defer r.Body.Close()
		}

		if !authenticated(w, r) {
			return
		}

		switch r.Method {
		case http.MethodPost:
			handleGroupCreate(w, r)
		case http.MethodGet:
			handleGroupList(w, r)
		default:
			w.WriteHeader(http.StatusMethodNotAllowed)
		}
	})

	mux.HandleFunc("/groups/{group}", func(w http.ResponseWriter, r *http.Request) {
		// GET, PUT, DELETE
		if r.Body != nil {
			defer r.Body.Close()
		}

		if !authenticated(w, r) {
			return
		}

		switch r.Method {
		case http.MethodGet:
			handleGroupGet(w, r)
		case http.MethodPut:
			handleGroupUpdate(w, r)
		case http.MethodDelete:
			handleGroupDelete(w, r)
		default:
			w.WriteHeader(http.StatusMethodNotAllowed)
		}
	})

	mux.HandleFunc("/groups/{group}/invite", func(w http.ResponseWriter, r *http.Request) {
		// POST
		if r.Body != nil {
			defer r.Body.Close()
		}

		if !authenticated(w, r) {
			return
		}

		if r.Method != http.MethodPost {
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}

		handleGroupInvite(w, r)
	})

	mux.HandleFunc("/groups/{group}/join", func(w http.ResponseWriter, r *http.Request) {
		// POST
		if r.Body != nil {
			defer r.Body.Close()
		}

		if !authenticated(w, r) {
			return
		}

		if r.Method != http.MethodPost {
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}

		handleGroupJoin(w, r)
	})

	mux.HandleFunc("/groups/{group}/decline", func(w http.ResponseWriter, r *http.Request) {
		// POST
		if r.Body != nil {
			defer r.Body.Close()
		}

		if !authenticated(w, r) {
			return
		}

		if r.Method != http.MethodPost {
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}

		handleGroupDecline(w, r)
	})

	mux.HandleFunc("/groups/{group}/leave", func(w http.ResponseWriter, r *http.Request) {
		// POST
		if r.Body != nil {
			defer r.Body.Close()
		}

		if !authenticated(w, r) {
			return
		}

		if r.Method != http.MethodPost {
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}

		handleGroupLeave(w, r)
	})

	mux.HandleFunc("/groups/{group}/kick", func(w http.ResponseWriter, r *http.Request) {
		// POST
		if r.Body != nil {
			defer r.Body.Close()
		}

		if !authenticated(w, r) {
			return
		}

		if r.Method != http.MethodPost {
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}

		handleGroupKick(w, r)
	})

	mux.HandleFunc("/groups/invitations", func(w http.ResponseWriter, r *http.Request) {
		if !authenticated(w, r) {
			return
		}

		if r.Method != http.MethodGet {
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}

		handleGroupInvitesList(w, r)
	})

	// TAPP endpoints
	mux.HandleFunc("/groups/{group}/tapp", func(w http.ResponseWriter, r *http.Request) {
		// POST
		if r.Body != nil {
			defer r.Body.Close()
		}

		if !authenticated(w, r) {
			return
		}

		switch r.Method {
		case http.MethodPost:
			handleTapp(w, r)
		case http.MethodGet:
			handleTappGet(w, r)
		default:
			w.WriteHeader(http.StatusMethodNotAllowed)
		}
	})

	return logWrapper(mux)
}
