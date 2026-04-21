export default function SubmitButton({ busy, disabled }) {
  return (
    <button
      type="submit"
      className="submit-btn"
      disabled={disabled}
    >
      <span className="submit-btn__label">
        {busy ? "Importing" : "Import"}
      </span>
      {busy ? (
        <span className="submit-btn__dots" aria-hidden="true">
          <i />
          <i />
          <i />
        </span>
      ) : (
        <svg
          viewBox="0 0 24 24"
          width="16"
          height="16"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <line x1="5" y1="12" x2="19" y2="12" />
          <polyline points="12 5 19 12 12 19" />
        </svg>
      )}
    </button>
  );
}
