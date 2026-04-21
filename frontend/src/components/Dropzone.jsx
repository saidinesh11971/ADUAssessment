import { MAX_FILE_SIZE_MB } from "../constants.js";
import { formatSize } from "../utils/formatSize.js";

export default function Dropzone({
  file,
  busy,
  isDragging,
  inputRef,
  onDragEnter,
  onDragLeave,
  onDrop,
  onSelectFile,
  onClearFile,
}) {
  return (
    <div
      className={[
        "dropzone",
        isDragging ? "dropzone--drag" : "",
        file ? "dropzone--filled" : "",
        busy ? "dropzone--busy" : "",
      ]
        .filter(Boolean)
        .join(" ")}
      onDragEnter={onDragEnter}
      onDragOver={(e) => e.preventDefault()}
      onDragLeave={onDragLeave}
      onDrop={onDrop}
      onClick={() => !busy && inputRef.current?.click()}
      onKeyDown={(e) => {
        if ((e.key === "Enter" || e.key === " ") && !busy) {
          e.preventDefault();
          inputRef.current?.click();
        }
      }}
      role="button"
      tabIndex={0}
      aria-label="Choose a spreadsheet to upload"
    >
      <input
        ref={inputRef}
        type="file"
        accept=".xlsx"
        hidden
        onChange={(e) => onSelectFile(e.target.files?.[0] ?? null)}
      />

      {!file ? (
        <div className="dropzone__prompt">
          <div className="dropzone__icon">
            <svg
              viewBox="0 0 24 24"
              width="30"
              height="30"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.6"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="17 8 12 3 7 8" />
              <line x1="12" y1="3" x2="12" y2="15" />
            </svg>
          </div>
          <p className="dropzone__title">
            Drop your spreadsheet here
          </p>
          <p className="dropzone__hint">
            or <span className="dropzone__link">click to browse</span>
            <span className="dropzone__sep">·</span>
            <span className="dropzone__muted">.xlsx only</span>
            <span className="dropzone__sep">·</span>
            <span className="dropzone__muted">max {MAX_FILE_SIZE_MB} MB</span>
          </p>
        </div>
      ) : (
        <div className="dropzone__file">
          <div className="dropzone__file-icon" aria-hidden="true">
            <svg
              viewBox="0 0 24 24"
              width="22"
              height="22"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.7"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
              <line x1="8" y1="13" x2="16" y2="13" />
              <line x1="8" y1="17" x2="16" y2="17" />
            </svg>
          </div>
          <div className="dropzone__file-meta">
            <p className="dropzone__file-name">{file.name}</p>
            <p className="dropzone__file-size">
              {formatSize(file.size)}
              <span className="dropzone__dot">·</span>
              Ready to import
            </p>
          </div>
          {!busy && (
            <button
              type="button"
              className="dropzone__remove"
              onClick={onClearFile}
              aria-label="Remove file"
            >
              <svg
                viewBox="0 0 24 24"
                width="16"
                height="16"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          )}
        </div>
      )}

      <div className="dropzone__bar" aria-hidden="true">
        <div className="dropzone__bar-fill" />
      </div>
    </div>
  );
}
