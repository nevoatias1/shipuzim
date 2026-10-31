import siteContent from '../content/siteContent.js'

const formatter = new Intl.DateTimeFormat('he-IL', { dateStyle: 'medium' })

export default function ReviewCard({ item, children }) {
  const date = item.createdAt && new Date(/(?:Z|[+-]\d\d:\d\d)$/.test(item.createdAt) ? item.createdAt : `${item.createdAt}Z`)
  const validDate = date && !Number.isNaN(date.getTime())
  return (
    <article className="review-card live-review-card">
      <header className="review-card-heading">
        <h3>{item.displayName}</h3>
        {validDate && <time dateTime={date.toISOString()}>{formatter.format(date)}</time>}
      </header>
      <span className="review-stars" role="img" aria-label={siteContent.reviews.starsLabel.replace('{rating}', item.rating)}>
        <span aria-hidden="true">{'★'.repeat(item.rating)}{'☆'.repeat(5 - item.rating)}</span>
      </span>
      {item.text && <p className="review-text" dir="auto">{item.text}</p>}
      {children}
    </article>
  )
}
