package firebase

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"slices"
	"strconv"
	"sync"

	firebase "firebase.google.com/go/v4"
	"firebase.google.com/go/v4/messaging"
	"github.com/trebent/tapp-backend/env"
	"github.com/trebent/tapp-backend/model"
	"github.com/trebent/zerologr"
	"google.golang.org/api/option"
)

var (
	//nolint:gochecknoglobals
	fcmLock = sync.Mutex{}
	//nolint:gochecknoglobals
	fcmBlob = map[string]string{}
	//nolint:gochecknoglobals
	c *messaging.Client
)

func Initialize() {
	ctx := context.Background()

	readBlob()
	zerologr.Info("loaded FCM blob", "blob", fcmBlob)

	data, _ := os.ReadFile(env.FirebaseSvcKeyPath.Value())
	zerologr.Info(string(data))

	// Path to the JSON key you downloaded
	opt := option.WithCredentialsFile(env.FirebaseSvcKeyPath.Value())

	app, err := firebase.NewApp(ctx, nil, opt)
	if err != nil {
		zerologr.Error(err, "failed to create new app")
		os.Exit(1)
	}

	client, err := app.Messaging(ctx)
	if err != nil {
		zerologr.Error(err, "failed to create messaging app")
		os.Exit(1)
	}
	c = client
}

func Add(email, fcm string) {
	zerologr.Info("adding email " + email + " to blob")
	fcmLock.Lock()
	defer fcmLock.Unlock()

	fcmBlob[email] = fcm

	writeBlob()
	zerologr.Info("wrote to FCM blob", "blob", fcmBlob)
}

func Remove(email string) {
	zerologr.Info("removing email " + email + " from blob")
	fcmLock.Lock()
	defer fcmLock.Unlock()

	delete(fcmBlob, email)

	writeBlob()
	zerologr.Info("wrote to FCM blob", "blob", fcmBlob)
}

/*
Expected notification DATA:

	title = data["title"]!!
	body = data["body"]!!
	sender = data["sender"]!!
	senderTag = data["sender_tag"]!!
	time = data["time"]!!
	groupId = data["group_id"]!!
	individual = data["individual"]
	type = data["type"]!!

NO NOTIFICATION DATA TO PREVENT SYSTEM TRAY HANDLING.
*/

type TappNotification struct {
	Title   string
	Body    string
	Time    int64
	Group   *model.Group
	Account *model.Account
	Type    string
}

// SendIndividual, for send invividual, the account is the receiver, and sender.
func SendIndividual(n *TappNotification) {
	zerologr.Info(
		fmt.Sprintf(
			"notifying individual %s with id %d", n.Account.Email, n.Group.ID,
		),
	)

	_, err := c.Send(context.Background(), &messaging.Message{
		Token: getFCM(n.Account),
		Data: map[string]string{
			"title":      n.Title,
			"body":       n.Body,
			"sender":     n.Account.Email,
			"sender_tag": n.Account.Tag,
			"type":       n.Type,
			// This is used to display targetted notifications on the client side.
			"individual": "true",
			"time":       strconv.Itoa(int(n.Time)),
			"group_id":   strconv.Itoa(n.Group.ID),
		},
	})
	if err != nil {
		zerologr.Error(err, "failed to send message")
		return
	}
}

// SendMulticast, for send multicast, the account is the sender.
func SendMulticast(n *TappNotification) {
	zerologr.Info(
		fmt.Sprintf(
			"%s is notifying group %s with id %d", n.Account.Email, n.Group.Name, n.Group.ID,
		),
	)

	_, err := c.SendEachForMulticast(context.Background(), &messaging.MulticastMessage{
		Tokens: getFCMS(n.Group),
		Data: map[string]string{
			"title":      n.Title,
			"body":       n.Body,
			"sender":     n.Account.Email,
			"sender_tag": n.Account.Tag,
			"time":       strconv.Itoa(int(n.Time)),
			"group_id":   strconv.Itoa(n.Group.ID),
		},
	})
	if err != nil {
		zerologr.Error(err, "failed to send multicast message")
		return
	}
}

func getFCM(account *model.Account) string {
	fcmLock.Lock()
	defer fcmLock.Unlock()

	for email, fcm := range fcmBlob {
		if email == account.Email {
			return fcm
		}
	}

	return ""
}

func getFCMS(group *model.Group) []string {
	fcmLock.Lock()
	defer fcmLock.Unlock()

	fcms := []string{}
	for email, fcm := range fcmBlob {
		if slices.ContainsFunc(
			group.Members,
			func(a *model.Account) bool { return a.Email == email },
		) || group.Owner == email {
			fcms = append(fcms, fcm)
		}
	}

	zerologr.Info("collected FCMs for broadcast", "fcms", fcms)

	return fcms
}

func readBlob() {
	data, err := os.ReadFile(filepath.Join(env.FileSystem.Value(), "fcm-blob.json"))
	if err != nil {
		zerologr.Error(err, "failed to read from FCM blob")
	} else {
		//nolint:govet,gosec
		err := json.Unmarshal(data, &fcmBlob)
		if err != nil {
			zerologr.Error(err, "failed to unmarshal FCM blob")
		}
	}
}

func writeBlob() {
	data, err := json.Marshal(fcmBlob)
	if err != nil {
		zerologr.Error(err, "failed to serialize FCM update")
	} else {
		//nolint:govet,gosec
		err := os.WriteFile(filepath.Join(env.FileSystem.Value(), "fcm-blob.json"), data, 0o755)
		if err != nil {
			zerologr.Error(err, "failed to write file")
		}
	}
}
