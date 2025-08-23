package main

import (
	"context"
	"errors"
	"flag"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/trebent/envparser"
	"github.com/trebent/zerologr"
)

var (
	//nolint:gochecknoglobals // Global vars are fine for env vars.
	addr = envparser.Register(&envparser.Opts[string]{
		Value: ":8080",
		Name:  "ADDR",
		Desc:  "Address to listen on",
	})
	//nolint:gochecknoglobals // Global vars are fine for env vars.
	logToConsole = envparser.Register(&envparser.Opts[bool]{
		Value: true,
		Name:  "LOG_TO_CONSOLE",
		Desc:  "Log to console instead of JSON",
	})
	//nolint:gochecknoglobals // Global vars are fine for env vars.
	logLevel = envparser.Register(&envparser.Opts[int]{
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
)

const (
	readTimeout     = 5 * time.Second
	writeTimeout    = 5 * time.Second
	shutdownTimeout = 5 * time.Second
)

func main() {
	//nolint:reassign // This is intended
	flag.Usage = func() {
		_, _ = fmt.Fprintf(flag.CommandLine.Output(), "Usage of %s:\n", os.Args[0])
		_, _ = fmt.Fprintf(flag.CommandLine.Output(), "%s:\n", envparser.Help())
		flag.PrintDefaults()
	}

	// ExitOnError is set to true, so this will exit on error.
	_ = envparser.Parse()

	zerologr.Set(zerologr.New(&zerologr.Opts{
		Console: logToConsole.Value(),
		Caller:  true,
		V:       logLevel.Value(),
	}))

	signalCtx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()

	httpServer := &http.Server{
		Addr:         addr.Value(),
		Handler:      http.DefaultServeMux,
		ReadTimeout:  readTimeout,
		WriteTimeout: writeTimeout,
	}

	go func() {
		if err := httpServer.ListenAndServe(); !errors.Is(err, http.ErrServerClosed) && err != nil {
			zerologr.Error(err, "server start failed")
			//nolint:gocritic // I know.
			os.Exit(1)
		}
		zerologr.Info("server stopped gracefully")
	}()

	<-signalCtx.Done()
	zerologr.Info("server shutting down")
	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), shutdownTimeout)
	defer shutdownCancel()
	if err := httpServer.Shutdown(shutdownCtx); err != nil {
		zerologr.Error(err, "server shutdown failed")
		//nolint:gocritic // I know.
		os.Exit(1)
	}
}
