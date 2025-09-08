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

	"github.com/rs/zerolog"
	"github.com/trebent/envparser"
	"github.com/trebent/tapp-backend/env"
	"github.com/trebent/tapp-backend/firebase"
	"github.com/trebent/tapp-backend/handler"
	"github.com/trebent/zerologr"
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
	_ = env.Parse()

	zerologr.SetMessageFieldName("message")
	zerolog.LevelFieldName = "severity"
	zerologr.Set(zerologr.New(&zerologr.Opts{
		Console: env.LogToConsole.Value(),
		Caller:  true,
		V:       env.LogLevel.Value(),
	}))

	handler.Initialize()
	firebase.Initialize()

	signalCtx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()

	httpServer := &http.Server{
		Addr:         env.Addr.Value(),
		Handler:      handler.Handler(),
		ReadTimeout:  readTimeout,
		WriteTimeout: writeTimeout,
	}

	go func() {
		zerologr.Info("starting tapp cloud service on port " + env.Addr.Value())
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
