//nolint:gochecknoglobals
package env

import (
	"fmt"

	"github.com/trebent/envparser"
)

var (
	//nolint:gochecknoglobals // Global vars are fine for env vars.
	Addr = envparser.Register(&envparser.Opts[string]{
		Value: ":8080",
		Name:  "ADDR",
		Desc:  "Address to listen on",
	})
	//nolint:gochecknoglobals // Global vars are fine for env vars.
	LogToConsole = envparser.Register(&envparser.Opts[bool]{
		Value: true,
		Name:  "LOG_TO_CONSOLE",
		Desc:  "Log to console instead of JSON",
	})
	//nolint:gochecknoglobals // Global vars are fine for env vars.
	LogLevel = envparser.Register(&envparser.Opts[int]{
		Value: 0,
		Name:  "LOG_LEVEL",
		Desc:  "Log level (higher is more verbose), negative values are invalid",
		Validate: func(v int) error {
			if v < 0 {
				return fmt.Errorf("value is negative: %d", v)
			}
			return nil
		},
	})
	FileSystem = envparser.Register(&envparser.Opts[string]{
		Value: "/tmp/tapp",
		Name:  "FILE_SYSTEM",
		Desc:  "File path",
	})

	AdminKey = envparser.Register(&envparser.Opts[string]{
		Value: "adminkey",
		Name:  "ADMIN_KEY",
		Desc:  "Administrator key to read data",
	})
)

func Parse() error {
	return envparser.Parse()
}
