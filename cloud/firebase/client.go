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

	msg := &messaging.Message{
		Token: "test-token",
		Notification: &messaging.Notification{
			Title: "Hello",
			Body:  "From Go",
		},
	}

	_, err = client.Send(ctx, msg)
	if err != nil {
		zerologr.Error(err, "failed to send test message on start")
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

func Send(sender string, group *model.Group) {
	zerologr.Info("notifying group " + group.Name + " with id " + strconv.Itoa(group.ID))

	_, err := c.SendEachForMulticast(context.Background(), &messaging.MulticastMessage{
		Tokens: getFCMS(group),
		Data: map[string]string{
			"sender":   sender,
			"group_id": strconv.Itoa(group.ID),
		},
		Notification: &messaging.Notification{
			Title: fmt.Sprintf("Group %s was tapped!", group.Name),
			Body:  "Open the Tapp app to check it out!",
		},
	})
	if err != nil {
		zerologr.Error(err, "failed to send multicast message")
		return
	}
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
