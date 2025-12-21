import React from 'react';
import RadioPlayer from './components/RadioPlayer';
import Schedule from './components/Schedule';
import './App.css';

const App: React.FC = () => {
  const STREAM_URL = 'http://server.katusa.xyz:7000/stream';
  const STATION_NAME = 'KATUSA Radio Station';

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-content">
          <div className="logo">
            <span className="logo-icon">📻</span>
            <h1 className="logo-text">{STATION_NAME}</h1>
          </div>
          <p className="tagline">당신의 하루를 함께하는 특별한 라디오</p>
        </div>
      </header>

      <main className="app-main">
        <section className="player-section">
          <RadioPlayer streamUrl={STREAM_URL} stationName={STATION_NAME} />
        </section>

        <section className="schedule-section">
          <Schedule />
        </section>
      </main>

      <footer className="app-footer">
        <p>&copy; 2025 KATUSA Radio Station. All rights reserved.</p>
        <p className="footer-info">
          🎵 24시간 논스톱 방송 | 📡 Live Streaming
        </p>
      </footer>
    </div>
  );
};

export default App;
