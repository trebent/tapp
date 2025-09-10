package model

import (
	"encoding/json"
	"fmt"
	"io"
	"strconv"
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
		Emoji   string     `json:"emoji"`
		Desc    string     `json:"description,omitempty"`
		Owner   string     `json:"owner,omitempty"`
		Members []*Account `json:"members,omitempty"`
		Invites []*Account `json:"invites,omitempty"`
	}
	Invitation struct {
		GroupID   int    `json:"group_id"`
		GroupName string `json:"group_name"`
		Email     string `json:"email"`
	}
	Tapp struct {
		Time    int64    `json:"time"`
		GroupID int      `json:"group_id"`
		User    *Account `json:"user"`
	}
)

func (a *Account) Key() string {
	return a.Email
}

func (g *Group) Key() string {
	return strconv.Itoa(g.ID)
}

func (i *Invitation) Key() string {
	return fmt.Sprintf("%d-%s", i.GroupID, i.Email)
}

func (t *Tapp) TableKey() string {
	return strconv.Itoa(t.GroupID)
}

func (a *Account) UserIdentifier() string {
	if a.Tag != "" {
		return a.Tag
	}
	return a.Email
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
