package model

import (
	"testing"

	"github.com/trebent/tapp-backend/db"
	"github.com/trebent/tapp-backend/env"
)

func TestAccount(t *testing.T) {
	defer db.Clear[*Account]()
	env.Parse()

	if err := db.Save(&Account{Tag: "tag", Email: "email", Password: "password"}); err != nil {
		t.Fatal(err)
	}

	entity, err := db.Read(&Account{Email: "email"})
	if err != nil {
		t.Fatal(err)
	}

	if entity.Email != "email" {
		t.Fatalf("expected email to be 'email', got '%s'", entity.Email)
	}

	if err := db.Save(&Account{Tag: "taggggggg", Email: "email", Password: "password2"}); err != nil {
		t.Fatal(err)
	}

	accounts, err := db.ReadAll[*Account]()
	if err != nil {
		t.Fatal(err)
	}
	if len(accounts) != 1 {
		t.Fatalf("expected 1 account, got %d", len(accounts))
	}

	if err := db.Save(&Account{Tag: "taggggggg", Email: "email2", Password: "password2"}); err != nil {
		t.Fatal(err)
	}

	accounts, err = db.ReadAll[*Account]()
	if err != nil {
		t.Fatal(err)
	}
	if len(accounts) != 2 {
		t.Fatalf("expected 1 account, got %d", len(accounts))
	}

	entity, err = db.Read(&Account{Email: "email2"})
	if err != nil {
		t.Fatal(err)
	}

	if entity.Email != "email2" {
		t.Fatalf("expected email to be 'email2', got '%s'", entity.Email)
	}

	if err := db.Delete(&Account{Email: "email2"}); err != nil {
		t.Fatal(err)
	}

	_, err = db.Read(&Account{Email: "email2"})
	if err == nil {
		t.Fatal("expected error, got nil")
	}
}
