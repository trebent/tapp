package handler

import (
	"net/http"
)

//nolint:gocognit,funlen
func Handler() http.Handler {
	mux := http.NewServeMux()

	// Health endpoint
	mux.HandleFunc("/health", func(w http.ResponseWriter, _ *http.Request) {
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
	return mux
}
