package firebase

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strconv"
	"sync"

	firebase "firebase.google.com/go"
	"firebase.google.com/go/messaging"
	"github.com/trebent/tapp-backend/env"
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

	// Path to the JSON key you downloaded
	opt := option.WithCredentialsFile(env.FirebaseSvcKeyPath.Value())

	app, err := firebase.NewApp(ctx, nil, opt)
	if err != nil {
		log.Fatalf("Error initializing Firebase: %v", err)
	}

	client, err := app.Messaging(ctx)
	if err != nil {
		log.Fatalf("Error getting Firebase Messaging client: %v", err)
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

func Send(groupName string, groupID int) {
	_, err := c.SendMulticast(context.Background(), &messaging.MulticastMessage{
		Tokens: getFCMS(),
		Data: map[string]string{
			"group_id": strconv.Itoa(groupID),
		},
		Notification: &messaging.Notification{
			Title: fmt.Sprintf("Group %s was tapped!", groupName),
			Body:  "Open the Tapp app to check it out!",
		},
	})
	if err != nil {
		zerologr.Error(err, "failed to send multicast message")
		return
	}
}

func getFCMS() []string {
	return []string{}
}

func writeBlob() {
	data, err := os.ReadFile(filepath.Join(env.FileSystem.Value(), "fcm-blob.json"))
	if err != nil {
		zerologr.Error(err, "failed to read from FCM blob")
	} else {
		err := json.Unmarshal(data, &fcmBlob)
		if err != nil {
			zerologr.Error(err, "failed to unmarshal FCM blob")
		}
	}
}

func readBlob() {
	data, err := json.Marshal(fcmBlob)
	if err != nil {
		zerologr.Error(err, "failed to serialize FCM update")
	} else {
		err := os.WriteFile(filepath.Join(env.FileSystem.Value(), "fcm-blob.json"), data, 0o755)
		if err != nil {
			zerologr.Error(err, "failed to write file")
		}
	}
}
