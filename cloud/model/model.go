package model

import (
	"encoding/json"
	"io"
	"strconv"
	"time"
)

type (
	Account struct {
		Tag      string `json:"tag,omitempty"`
		Email    string `json:"email"`
		Password string `json:"password,omitempty"`
	}
	Group struct {
		ID      int        `json:"id,omitempty"`
		Name    string     `json:"name"`
		Owner   string     `json:"owner"`
		Members []*Account `json:"members,omitempty"`
		Invites []*Account `json:"invites,omitempty"`
	}
	Tapp struct {
		ID      int        `json:"id"`
		Time    *time.Time `json:"time"`
		GroupID int        `json:"group_id"`
		User    *Account   `json:"user"`
	}
)

func (a *Account) Key() string {
	return a.Email
}

func (g *Group) Key() string {
	return strconv.Itoa(g.ID)
}

func (t *Tapp) Key() string {
	return strconv.Itoa(t.ID)
}

func Deserialize[T any](reader io.Reader, target T) (T, error) {
	data, err := io.ReadAll(reader)
	if err != nil {
		return target, err
	}
	//nolint:gosec,govet
	if err := json.Unmarshal(data, target); err != nil {
		return target, err
	}

	return target, nil
}

func WriteJSON(writer io.Writer, data any) error {
	encoded, err := json.MarshalIndent(data, "", "  ")
	if err != nil {
		return err
	}

	_, err = writer.Write(encoded)
	return err
}
