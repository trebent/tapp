package handler

import (
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/trebent/tapp-backend/db"
	"github.com/trebent/tapp-backend/env"
	"github.com/trebent/tapp-backend/model"
)

func TestLoginGetAccount(t *testing.T) {
	defer db.Clear[*model.Account]()
	env.Parse()

	db.Save(&model.Account{
		Tag:      "tag",
		Email:    "email@domain.se",
		Password: "password",
	})

	req := httptest.NewRequest("POST", "/login", strings.NewReader(`{"email":"email@domain.se","password":"password"}`))
	recorder := httptest.NewRecorder()

	handleLogin(recorder, req)

	if recorder.Result().StatusCode != 204 {
		t.Fatalf("got status %d, want %d", recorder.Code, 204)
		return
	}

	req = httptest.NewRequest("GET", "/accounts/email@domain.se", nil)
	req.Header.Set("Authorization", recorder.Header().Get("Authorization"))
	recorder = httptest.NewRecorder()
	handleAccountGet(recorder, req)

	if recorder.Result().StatusCode != 200 {
		t.Fatalf("got status %d, want %d", recorder.Code, 200)
		return
	}
}

func TestLoginLogout(t *testing.T) {
	defer db.Clear[*model.Account]()
	env.Parse()

	db.Save(&model.Account{
		Tag:      "tag",
		Email:    "email@domain.se",
		Password: "password",
	})

	req := httptest.NewRequest("POST", "/login", strings.NewReader(`{"email":"email@domain.se","password":"password"}`))
	loginRecorder := httptest.NewRecorder()

	handleLogin(loginRecorder, req)

	if loginRecorder.Result().StatusCode != 204 {
		t.Fatalf("got status %d, want %d", loginRecorder.Code, 204)
		return
	}

	req = httptest.NewRequest("POST", "/logout", nil)
	req.Header.Set("Authorization", loginRecorder.Header().Get("Authorization"))
	logoutRecorder := httptest.NewRecorder()

	handleLogout(logoutRecorder, req)

	if logoutRecorder.Result().StatusCode != 204 {
		t.Fatalf("got status %d, want %d", logoutRecorder.Code, 204)
		return
	}

	req = httptest.NewRequest("GET", "/accounts/email@domain.se", nil)
	req.Header.Set("Authorization", loginRecorder.Header().Get("Authorization"))
	recorder := httptest.NewRecorder()
	handleAccountGet(recorder, req)

	if recorder.Result().StatusCode != 403 {
		t.Fatalf("got status %d, want %d", recorder.Code, 403)
		return
	}
}
