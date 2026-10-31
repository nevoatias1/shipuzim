import { Component } from 'react'

export default class AppErrorBoundary extends Component {
  state = { failed: false }

  static getDerivedStateFromError() {
    return { failed: true }
  }

  render() {
    if (this.state.failed) {
      return (
        <main className="placeholder-page wrap">
          <div className="placeholder-panel">
            <h1>אירעה שגיאה בטעינת האתר</h1>
            <p role="alert">אפשר לנסות לטעון את הדף מחדש.</p>
            <button className="button button-primary" type="button" onClick={() => window.location.reload()}>טעינה מחדש</button>
          </div>
        </main>
      )
    }
    return this.props.children
  }
}
