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

type Model interface {
	Key() string
}

const entityLimit = 2500

var (
	//nolint:gochecknoglobals
	idMap = sync.Map{} // map[string]int
	//nolint:gochecknoglobals
	tableLock = sync.Map{} // map[string]*sync.Mutex

	//nolint:gochecknoglobals,mnd
	logger = zerologr.V(10)
)

func AquireTableLock[T Model]() {
	tableName := getTableName[T]()
	logger.Info("acquiring table lock", "table", tableName)
	val, _ := tableLock.LoadOrStore(tableName, &sync.Mutex{})
	//nolint:errcheck
	mutex := val.(*sync.Mutex)
	mutex.Lock()
}

func ReleaseTableLock[T Model]() {
	tableName := getTableName[T]()
	logger.Info("releasing table lock", "table", tableName)
	val, _ := tableLock.Load(tableName)
	//nolint:errcheck
	mutex := val.(*sync.Mutex)
	mutex.Unlock()
}

func NextID[T Model]() int {
	tableName := getTableName[T]()
	val, _ := idMap.LoadOrStore(tableName, 0)

	if val == 0 {
		//nolint:errcheck
		es, _ := ReadAll[T]()
		val = len(es)
	}

	//nolint:errcheck
	nextID := val.(int) + 1
	idMap.Store(tableName, nextID)
	return nextID
}

func Exists[T Model](entity T) bool {
	all, err := ReadAll[T]()
	if err != nil {
		panic("failed to read all entities: " + err.Error())
	}

	for _, item := range all {
		if item.Key() == entity.Key() {
			return true
		}
	}

	return false
}

func Save[T Model](entity T) error {
	logger.Info("saving entity", "entity", entity, "table", getTableName[T]())

	all, err := ReadAll[T]()
	if err != nil {
		return err
	}
	//nolint:mnd
	logger.V(100).Info("all entities", "entities", all, "table", getTableName[T]())

	if existingEntity := find(all, entity); existingEntity != nil {
		*existingEntity = entity
	} else {
		if len(all) >= entityLimit {
			return fmt.Errorf("entity limit reached (%d) for table %s", entityLimit, getTableName[T]())
		}
		all = append(all, entity)
	}

	data, err := json.MarshalIndent(all, "", "  ")
	if err != nil {
		return err
	}

	//nolint:gosec,govet
	if err := os.WriteFile(getTablePath[T](), data, 0o644); err != nil {
		return err
	}

	logger.Info("entity saved", "entity", entity, "table", getTableName[T](), "count", len(all))

	return nil
}

func Read[T Model](entity T) (T, error) {
	logger.Info("reading entity", "table", getTableName[T]())

	var target T
	entities, err := ReadAll[T]()
	if err != nil {
		return target, err
	}

	targetPtr := find(entities, entity)
	if targetPtr == nil {
		return target, fmt.Errorf("entity with key %s not found in table %s",
			entity.Key(), getTableName[T]())
	}

	target = *targetPtr

	return target, nil
}

func ReadAll[T Model]() ([]T, error) {
	logger.Info("reading all", "table", getTableName[T]())

	if err := tableCheck[T](); err != nil {
		return nil, err
	}
	data, err := os.ReadFile(getTablePath[T]())
	if err != nil {
		return nil, err
	}

	entities := make([]T, 0)
	//nolint:gosec,govet
	if err := json.Unmarshal(data, &entities); err != nil {
		logger.Error(err, "failed to unmarshal data", "data", string(data))
		return nil, err
	}
	logger.Info("data read", "count", len(entities), "table", getTableName[T]())

	return entities, nil
}

func Delete[T Model](entity T) error {
	logger.Info("deleting entity", "entity", entity, "table", getTableName[T]())

	all, err := ReadAll[T]()
	if err != nil {
		return err
	}

	index := -1
	for i, item := range all {
		if item.Key() == entity.Key() {
			index = i
			break
		}
	}

	if index == -1 {
		return fmt.Errorf("entity with key %s not found in table %s",
			entity.Key(), getTableName[T]())
	}

	all = append(all[:index], all[index+1:]...)
	data, err := json.MarshalIndent(all, "", "  ")
	if err != nil {
		return err
	}

	//nolint:gosec,govet
	if err := os.WriteFile(getTablePath[T](), data, 0o644); err != nil {
		return err
	}

	return nil
}

func Clear[T Model]() error {
	logger.Info("clearing table", "table", getTableName[T]())
	if err := os.Remove(getTablePath[T]()); err != nil {
		return err
	}
	return nil
}

func tableCheck[T Model]() error {
	logger.Info("running table check", "table", getTableName[T]())

	if !tableExists[T]() {
		logger.Info("table does not exist", "table", getTableName[T]())
		if err := createTable[T](); err != nil {
			return err
		}
	}
	return nil
}

func tableExists[T Model]() bool {
	logger.Info("checking if table exists", "table", getTableName[T]())
	_, err := os.Stat(getTablePath[T]())
	logger.Info("table exists?", "exists", err == nil, "table", getTableName[T]())
	return err == nil
}

func createTable[T Model]() error {
	logger.Info("creating table", "table", getTableName[T]())

	//nolint:gosec
	if err := os.MkdirAll(env.FileSystem.Value(), 0o755); err != nil {
		return err
	}

	var (
		f   *os.File
		err error
	)
	if f, err = os.Create(getTablePath[T]()); err != nil {
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

func getTableName[T Model]() string {
	var entity T
	return fmt.Sprintf("%s.json", strings.ReplaceAll(reflect.TypeOf(entity).String(), "*", ""))
}

func getTablePath[T Model]() string {
	return filepath.Join(env.FileSystem.Value(), getTableName[T]())
}

func find[T Model](entities []T, target T) *T {
	for i, entity := range entities {
		if entity.Key() == target.Key() {
			return &entities[i]
		}
	}
	return nil
}
