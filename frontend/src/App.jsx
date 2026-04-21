import { useState, useRef, useCallback } from "react";
import { importWorkbook } from "./api.js";
import Welcome from "./Welcome.jsx";
import BrandHeader from "./components/BrandHeader.jsx";
import Dropzone from "./components/Dropzone.jsx";
import SubmitButton from "./components/SubmitButton.jsx";
import ResultPanel from "./components/ResultPanel.jsx";
import Toast from "./components/Toast.jsx";
import { formatSize } from "./utils/formatSize.js";
import {
  TOAST_DURATION,
  MAX_FILE_SIZE_MB,
  MAX_FILE_SIZE_BYTES,
} from "./constants.js";

export default function App() {
  const [showWelcome, setShowWelcome] = useState(true);
  const [file, setFile] = useState(null);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [toasts, setToasts] = useState([]);
  const inputRef = useRef(null);
  const dragCounter = useRef(0);

  const pushToast = useCallback((toast) => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { ...toast, id }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, TOAST_DURATION);
  }, []);

  const dismissToast = (id) =>
    setToasts((prev) => prev.filter((t) => t.id !== id));

  function selectFile(selected) {
    if (!selected) return;
    if (!selected.name.toLowerCase().endsWith(".xlsx")) {
      pushToast({
        flavor: "failure",
        title: "Unsupported file",
        message: "Please choose an .xlsx workbook.",
      });
      return;
    }
    if (selected.size > MAX_FILE_SIZE_BYTES) {
      pushToast({
        flavor: "failure",
        title: "File too large",
        message: `${formatSize(selected.size)} exceeds the ${MAX_FILE_SIZE_MB} MB limit.`,
      });
      return;
    }
    setFile(selected);
    setResult(null);
  }

  function handleDrop(e) {
    e.preventDefault();
    dragCounter.current = 0;
    setIsDragging(false);
    const dropped = e.dataTransfer.files?.[0];
    if (dropped) selectFile(dropped);
  }

  function handleDragEnter(e) {
    e.preventDefault();
    dragCounter.current += 1;
    setIsDragging(true);
  }

  function handleDragLeave(e) {
    e.preventDefault();
    dragCounter.current -= 1;
    if (dragCounter.current <= 0) {
      dragCounter.current = 0;
      setIsDragging(false);
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!file || busy) return;

    setBusy(true);
    setResult(null);

    try {
      const { status, ok, payload } = await importWorkbook(file);
      if (ok) {
        const count = payload.importedCount ?? 0;
        pushToast({
          flavor: "success",
          title: "Import successful",
          message: `${count} record${count === 1 ? "" : "s"} imported into avb_closeout.`,
        });
        setResult({ flavor: "success", payload });
      } else if (status === 207) {
        const count = payload.importedCount ?? 0;
        const issues = (payload.errors || []).length;
        pushToast({
          flavor: "partial",
          title: "Imported with issues",
          message: `${count} record${count === 1 ? "" : "s"} saved, ${issues} issue${issues === 1 ? "" : "s"} flagged.`,
        });
        setResult({ flavor: "partial", payload });
      } else {
        const message = payload.message || `Request failed with status ${status}`;
        pushToast({
          flavor: "failure",
          title: "Import failed",
          message,
        });
        setResult({
          flavor: "failure",
          message,
          details: payload.details || [],
        });
      }
    } catch (err) {
      pushToast({
        flavor: "failure",
        title: "Server unreachable",
        message: "Could not reach the server.",
      });
      setResult({
        flavor: "failure",
        message: "Could not reach the server.",
        details: [err.message],
      });
    } finally {
      setBusy(false);
    }
  }

  function clearFile(e) {
    e?.stopPropagation();
    setFile(null);
    if (inputRef.current) inputRef.current.value = "";
  }

  return (
    <>
      {showWelcome && <Welcome onDone={() => setShowWelcome(false)} />}

      <main
        className={`container ${showWelcome ? "container--hidden" : "container--enter"}`}
      >
        <BrandHeader />

        <form className="upload-card" onSubmit={handleSubmit}>
          <Dropzone
            file={file}
            busy={busy}
            isDragging={isDragging}
            inputRef={inputRef}
            onDragEnter={handleDragEnter}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onSelectFile={selectFile}
            onClearFile={clearFile}
          />

          <SubmitButton busy={busy} disabled={busy || !file} />
        </form>

        {result && <ResultPanel result={result} />}
      </main>

      <div className="toast-stack" aria-live="polite" aria-atomic="true">
        {toasts.map((t) => (
          <Toast key={t.id} toast={t} onDismiss={() => dismissToast(t.id)} />
        ))}
      </div>
    </>
  );
}
