package handler

import (
	"encoding/json"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/trebent/tapp-backend/db"
	"github.com/trebent/tapp-backend/env"
	"github.com/trebent/tapp-backend/model"
)

func TestGroupCreate(t *testing.T) {
	defer db.Clear[*model.Account]()
	defer db.Clear[*model.Group]()
	env.Parse()

	req := httptest.NewRequest("POST", "/accounts", strings.NewReader(`{"email":"email@domain.se","password":"password"}`))
	recorder := httptest.NewRecorder()
	handleLogin(recorder, req)

	tests := []tc{
		{
			name:       "Create group successfully",
			method:     "POST",
			url:        "/groups",
			body:       `{"name":"My Group"}`,
			token:      recorder.Header().Get("Authorization"),
			wantStatus: 201,
		},
		{
			name:       "Create group failed, empty name",
			method:     "POST",
			url:        "/groups",
			body:       `{"name":""}`,
			token:      recorder.Header().Get("Authorization"),
			wantStatus: 400,
		},
		{
			name:       "Create group failed, name too long",
			method:     "POST",
			url:        "/groups",
			body:       `{"name":"12345123451234512345123451234512345123451234512345123451234512345123451234512345"}`,
			token:      recorder.Header().Get("Authorization"),
			wantStatus: 400,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			req := httptest.NewRequest(tt.method, tt.url, strings.NewReader(tt.body))
			if tt.token != "" {
				req.Header.Set("Authorization", tt.token)
			}
			recorder := httptest.NewRecorder()

			handleGroupCreate(recorder, req)

			if recorder.Result().StatusCode != tt.wantStatus {
				t.Errorf("got status %d, want %d", recorder.Code, tt.wantStatus)
			}
		})
	}
}

type groupListTC struct {
	name           string
	method         string
	url            string
	headers        map[string]string
	token          string
	expectedGroups []*model.Group
	wantStatus     int
}

func TestGroupList(t *testing.T) {
	defer db.Clear[*model.Group]()
	defer db.Clear[*model.Account]()
	env.Parse()

	if err := db.Save(&model.Account{Email: "email@domain.se", Password: "password"}); err != nil {
		t.Fatalf("failed to create account: %v", err)
		return
	}

	if err := db.Save(&model.Account{Email: "email3@domain.se", Password: "password"}); err != nil {
		t.Fatalf("failed to create account: %v", err)
		return
	}

	if err := db.Save(&model.Group{ID: 1, Name: "Group 1", Owner: "email@domain.se"}); err != nil {
		t.Fatalf("failed to create group: %v", err)
		return
	}

	if err := db.Save(&model.Group{ID: 2, Name: "Group 2", Owner: "email2@domain.se", Members: []*model.Account{{Email: "email@domain.se"}}}); err != nil {
		t.Fatalf("failed to create group: %v", err)
		return
	}

	req1 := httptest.NewRequest("POST", "/accounts", strings.NewReader(`{"email":"email@domain.se","password":"password"}`))
	recorder1 := httptest.NewRecorder()
	handleLogin(recorder1, req1)

	if recorder1.Result().StatusCode != 204 {
		t.Fatalf("got status %d, want %d", recorder1.Code, 204)
		return
	}

	req2 := httptest.NewRequest("POST", "/accounts", strings.NewReader(`{"email":"email3@domain.se","password":"password"}`))
	recorder2 := httptest.NewRecorder()
	handleLogin(recorder2, req2)

	if recorder2.Result().StatusCode != 204 {
		t.Fatalf("got status %d, want %d", recorder2.Code, 204)
		return
	}

	tests := []groupListTC{
		{
			name:   "List groups, owner and member",
			method: "GET",
			url:    "/groups",
			token:  recorder1.Header().Get("Authorization"),
			expectedGroups: []*model.Group{
				{Name: "Group 1"}, {Name: "Group 2"},
			},
			wantStatus: 200,
		},
		{
			name:           "Empty list",
			method:         "GET",
			url:            "/groups",
			token:          recorder2.Header().Get("Authorization"),
			expectedGroups: []*model.Group{},
			wantStatus:     200,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			req := httptest.NewRequest(tt.method, tt.url, nil)
			if tt.token != "" {
				req.Header.Set("Authorization", tt.token)
			}
			recorder := httptest.NewRecorder()

			handleGroupList(recorder, req)

			groups := []*model.Group{}
			err := json.Unmarshal(recorder.Body.Bytes(), &groups)
			if err != nil {
				t.Fatalf("failed to unmarshal response: %v", err)
				return
			}

			t.Log(recorder.Body.String())

			for _, expectedGroup := range tt.expectedGroups {
				found := false
				for _, group := range groups {
					if group.Name == expectedGroup.Name {
						found = true
						break
					}
				}
				if !found {
					t.Errorf("expected group %s not found in response", expectedGroup.Name)
				}
			}

			if recorder.Result().StatusCode != tt.wantStatus {
				t.Errorf("got status %d, want %d", recorder.Code, tt.wantStatus)
			}
		})
	}
}
