export function RecommendationPanel() {
  return (
    <article className="panel">
      <h2>Recommendation</h2>
      <dl className="metric-list">
        <div>
          <dt>Current result</dt>
          <dd className="muted">Not generated</dd>
        </div>
        <div>
          <dt>Worn state</dt>
          <dd className="muted">No result selected</dd>
        </div>
      </dl>
    </article>
  );
}
