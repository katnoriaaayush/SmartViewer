"""
Centralised logging configuration for the SmartAI server.

Importing this module configures the root logger exactly once. Other modules
simply do `logging.getLogger("smartai.<area>")` and inherit this setup.

Log level is controlled by the LOG_LEVEL env var (default INFO). Logs go to
stdout and to a rotating file at logs/server.log.
"""
import logging
import os
from logging.handlers import RotatingFileHandler
from pathlib import Path

_CONFIGURED = False


def configure() -> None:
    global _CONFIGURED
    if _CONFIGURED:
        return

    level_name = os.environ.get("LOG_LEVEL", "INFO").upper()
    level = getattr(logging, level_name, logging.INFO)

    fmt = logging.Formatter(
        fmt="%(asctime)s %(levelname)-7s [%(name)s] %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    root = logging.getLogger()
    root.setLevel(level)

    # Console
    console = logging.StreamHandler()
    console.setFormatter(fmt)
    root.addHandler(console)

    # Rotating file — 5 files of 2 MB each
    log_dir = Path(__file__).resolve().parent / "logs"
    log_dir.mkdir(exist_ok=True)
    file_handler = RotatingFileHandler(
        log_dir / "server.log", maxBytes=2 * 1024 * 1024, backupCount=5, encoding="utf-8"
    )
    file_handler.setFormatter(fmt)
    root.addHandler(file_handler)

    # Quiet down noisy third-party loggers a touch
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)

    _CONFIGURED = True
    logging.getLogger("smartai").info("Logging configured at level %s", level_name)
