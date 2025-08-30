package handler

import (
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/trebent/tapp-backend/db"
	"github.com/trebent/tapp-backend/env"
	"github.com/trebent/tapp-backend/model"
)

func TestAccountCreate(t *testing.T) {
	defer db.Clear[*model.Account]()
	env.Parse()

	tests := []tc{
		{
			name:       "Create account successfully",
			method:     "POST",
			url:        "/accounts",
			body:       `{"tag":"tag","email":"email@domain.se","password":"password"}`,
			wantStatus: 201,
		},
		{
			name:       "Create account failure, wrong email format",
			method:     "POST",
			url:        "/accounts",
			body:       `{"tag":"tagggg","email":"notemail","password":"password"}`,
			wantStatus: 400,
		},
		{
			name:       "Create account failure, password too short",
			method:     "POST",
			url:        "/accounts",
			body:       `{"tag":"tagggg","email":"email2@domain.se","password":"passw"}`,
			wantStatus: 400,
		},
		{
			name:       "Create account failure, email already exists",
			method:     "POST",
			url:        "/accounts",
			body:       `{"tag":"tagggg","email":"email@domain.se","password":"passwowrd"}`,
			wantStatus: 409,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			req := httptest.NewRequest(tt.method, tt.url, strings.NewReader(tt.body))
			recorder := httptest.NewRecorder()

			handleAccountCreate(recorder, req)

			if recorder.Result().StatusCode != tt.wantStatus {
				t.Errorf("got status %d, want %d", recorder.Code, tt.wantStatus)
			}
		})
	}
}

func TestAccountGet(t *testing.T) {
	defer db.Clear[*model.Account]()
	env.Parse()

	db.Save(&model.Account{
		Tag:      "tag",
		Email:    "email@domain.se",
		Password: "password",
	})

	tests := []tc{
		{
			name:   "Get account failure, user not authenticated",
			method: "GET",
			url:    "/accounts/email@domain.se",
			headers: map[string]string{
				"Authorization": "invalid",
			},
			wantStatus: 403,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			req := httptest.NewRequest(tt.method, tt.url, strings.NewReader(tt.body))
			recorder := httptest.NewRecorder()

			handleAccountGet(recorder, req)

			if recorder.Result().StatusCode != tt.wantStatus {
				t.Errorf("got status %d, want %d", recorder.Code, tt.wantStatus)
			}
		})
	}
}
