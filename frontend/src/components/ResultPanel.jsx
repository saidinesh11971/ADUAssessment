export default function ResultPanel({ result }) {
  if (result.flavor === "failure") {
    return (
      <section className="result result--failure">
        <div className="result__header">
          <div className="result__icon" aria-hidden="true">
            <FlavorGlyph flavor="failure" />
          </div>
          <div className="result__head-text">
            <h2 className="result__title">Import failed</h2>
            <p className="result__subtitle">{result.message}</p>
          </div>
        </div>
        {result.details?.length > 0 && (
          <div className="result__section">
            <p className="section-title">Details</p>
            <ul className="issue-list">
              {result.details.map((d, i) => (
                <li key={i} className="issue-item issue-item--plain">
                  {d}
                </li>
              ))}
            </ul>
          </div>
        )}
      </section>
    );
  }

  const { payload, flavor } = result;
  const imported = payload.importedCount ?? 0;
  const processed = payload.processedSheets || [];
  const skipped = payload.skippedSheets || [];
  const errors = payload.errors || [];

  const headTitle =
    flavor === "success" ? "Import complete" : "Imported with issues";
  const headSub =
    flavor === "success"
      ? `${imported} record${imported === 1 ? "" : "s"} saved to avb_closeout.`
      : `${imported} record${imported === 1 ? "" : "s"} saved · ${errors.length} issue${errors.length === 1 ? "" : "s"} flagged.`;

  return (
    <section className={`result result--${flavor}`}>
      <div className="result__header">
        <div className="result__icon" aria-hidden="true">
          <FlavorGlyph flavor={flavor} />
        </div>
        <div className="result__head-text">
          <h2 className="result__title">{headTitle}</h2>
          <p className="result__subtitle">{headSub}</p>
        </div>
      </div>

      <div className="result__stats">
        <Stat value={imported} label="Imported" />
        <Stat value={processed.length} label={processed.length === 1 ? "Sheet" : "Sheets"} />
        <Stat
          value={errors.length}
          label={errors.length === 1 ? "Issue" : "Issues"}
          tone={errors.length > 0 ? "warn" : "muted"}
        />
      </div>

      {processed.length > 0 && (
        <div className="result__section">
          <p className="section-title">Processed</p>
          <div className="chip-row">
            {processed.map((p, i) => (
              <span key={i} className="chip chip--ok">{p}</span>
            ))}
          </div>
        </div>
      )}

      {skipped.length > 0 && (
        <div className="result__section">
          <p className="section-title">Skipped</p>
          <div className="chip-row">
            {skipped.map((s, i) => (
              <span key={i} className="chip chip--muted">{s}</span>
            ))}
          </div>
        </div>
      )}

      {errors.length > 0 && (
        <div className="result__section">
          <p className="section-title">Issues ({errors.length})</p>
          <ul className="issue-list">
            {errors.map((issue, i) => (
              <IssueRow key={i} issue={issue} />
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

function Stat({ value, label, tone = "default" }) {
  return (
    <div className={`stat stat--${tone}`}>
      <span className="stat__value">{value}</span>
      <span className="stat__label">{label}</span>
    </div>
  );
}

function FlavorGlyph({ flavor }) {
  if (flavor === "success") {
    return (
      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
        <polyline points="20 6 9 17 4 12" />
      </svg>
    );
  }
  if (flavor === "partial") {
    return (
      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
        <line x1="12" y1="9" x2="12" y2="13" />
        <line x1="12" y1="17" x2="12.01" y2="17" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <line x1="15" y1="9" x2="9" y2="15" />
      <line x1="9" y1="9" x2="15" y2="15" />
    </svg>
  );
}

function IssueRow({ issue }) {
  return (
    <li className="issue-item">
      <div className="issue-item__meta">
        {issue.sheet && <span className="badge"><b>sheet</b>{issue.sheet}</span>}
        {issue.row != null && <span className="badge"><b>row</b>{issue.row}</span>}
        {issue.column && <span className="badge"><b>col</b>{issue.column}</span>}
      </div>
      {issue.message && <p className="issue-item__message">{issue.message}</p>}
    </li>
  );
}
