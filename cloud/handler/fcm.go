package handler

import (
	"net/http"

	"github.com/trebent/tapp-backend/firebase"
)

func handleFCMUpdate(w http.ResponseWriter, r *http.Request) {
	email := getUserEmailFromToken(r)
	fcm := r.Header.Get("X-fcm-token")

	firebase.Add(email, fcm)

	w.WriteHeader(204)
}
