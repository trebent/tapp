package db

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"reflect"
	"strings"
	"sync"

	"github.com/trebent/tapp-backend/env"
	"github.com/trebent/zerologr"
)

type Simple interface {
	TableKey() string
}

//nolint:gochecknoglobals,mnd
var simpleLogger = zerologr.V(10).WithName("simple")

func SimpleAcquire[T Simple](e T) {
	tableName := getSimpleTableName[T](e)
	simpleLogger.Info("acquiring table lock", "table", tableName)
	val, _ := tableLock.LoadOrStore(tableName, &sync.Mutex{})
	//nolint:errcheck
	mutex := val.(*sync.Mutex)
	mutex.Lock()
}
func SimpleRelease[T Simple](e T) {
	tableName := getSimpleTableName(e)
	simpleLogger.Info("releasing table lock", "table", tableName)
	val, _ := tableLock.Load(tableName)
	//nolint:errcheck
	mutex := val.(*sync.Mutex)
	mutex.Unlock()
}

func SimpleRead[T Simple](e T) ([]T, error) {
	simpleLogger.Info("reading all", "table", getSimpleTableName(e))

	if err := simpleTableCheck(e); err != nil {
		return nil, err
	}
	data, err := os.ReadFile(getSimpleTablePath(e))
	if err != nil {
		return nil, err
	}

	entities := make([]T, 0)
	//nolint:gosec,govet
	if err := json.Unmarshal(data, &entities); err != nil {
		simpleLogger.Error(err, "failed to unmarshal data", "data", string(data))
		return nil, err
	}
	simpleLogger.Info("data read", "count", len(entities), "table", getSimpleTableName(e))

	return entities, nil
}

func SimpleAppend[T Simple](entity T) error {
	es, err := SimpleRead(entity)
	if err != nil {
		return err
	}

	es = append(es, entity)

	data, err := json.Marshal(es)
	if err != nil {
		return err
	}

	//nolint:gosec,govet
	if err := os.WriteFile(getSimpleTablePath(entity), data, 0o644); err != nil {
		return err
	}

	simpleLogger.Info(
		"entity saved", "entity", entity, "table", getSimpleTableName(entity), "count", len(es),
	)

	return nil
}

func SimpleClear[T Simple](e T) error {
	simpleLogger.Info("clearing table", "table", getSimpleTableName(e))
	if err := os.Remove(getSimpleTablePath(e)); err != nil {
		if os.IsNotExist(err) {
			return nil
		}
		return err
	}
	return nil
}

func getSimpleTableName[T Simple](e T) string {
	return fmt.Sprintf(
		"%s-%s.json", strings.ReplaceAll(reflect.TypeOf(e).String(), "*", ""), e.TableKey(),
	)
}

func getSimpleTablePath[T Simple](e T) string {
	return filepath.Join(env.FileSystem.Value(), getSimpleTableName(e))
}

func simpleTableCheck[T Simple](e T) error {
	simpleLogger.Info("running table check", "table", getSimpleTableName(e))

	if !simpleTableExists(e) {
		simpleLogger.Info("table does not exist", "table", getSimpleTableName(e))
		if err := simpleCreateTable(e); err != nil {
			return err
		}
	}
	return nil
}

func simpleTableExists[T Simple](e T) bool {
	simpleLogger.Info("checking if table exists", "table", getSimpleTableName(e))
	_, err := os.Stat(getSimpleTablePath(e))
	simpleLogger.Info("table exists?", "exists", err == nil, "table", getSimpleTableName(e))
	return err == nil
}

func simpleCreateTable[T Simple](e T) error {
	simpleLogger.Info("creating table", "table", getSimpleTableName(e))

	//nolint:gosec
	if err := os.MkdirAll(env.FileSystem.Value(), 0o755); err != nil {
		return err
	}

	var (
		f   *os.File
		err error
	)
	if f, err = os.Create(getSimpleTablePath(e)); err != nil {
		return err
	}
	defer f.Close()

	data := []byte("[]")
	//nolint:gosec,govet
	if _, err := f.Write(data); err != nil {
		return err
	}

	return nil
}
