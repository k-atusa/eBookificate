import React, { useState, useRef, useEffect } from 'react';
import './RadioPlayer.css';

interface RadioPlayerProps {
  streamUrl: string;
  stationName: string;
}

const RadioPlayer: React.FC<RadioPlayerProps> = ({ streamUrl, stationName }) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [volume, setVolume] = useState(70);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const audioRef = useRef<HTMLAudioElement>(null);

  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.volume = volume / 100;
    }
  }, [volume]);

  const handlePlay = async () => {
    if (audioRef.current) {
      setIsLoading(true);
      setError(null);
      try {
        await audioRef.current.play();
        setIsPlaying(true);
      } catch (error) {
        console.error('Failed to play audio:', error);
        setError('재생 버튼을 눌러 라디오를 시작하세요');
      } finally {
        setIsLoading(false);
      }
    }
  };

  const handlePause = () => {
    if (audioRef.current) {
      audioRef.current.pause();
      setIsPlaying(false);
    }
  };

  const togglePlay = () => {
    if (isPlaying) {
      handlePause();
    } else {
      handlePlay();
    }
  };

  return (
    <div className="radio-player">
      <audio
        ref={audioRef}
        src={streamUrl}
        preload="none"
        onCanPlay={() => setIsLoading(false)}
        onWaiting={() => setIsLoading(true)}
        onPlaying={() => {
          setIsPlaying(true);
          setIsLoading(false);
        }}
        onPause={() => setIsPlaying(false)}
        onError={(e) => {
          console.error('Audio error:', e);
          setIsLoading(false);
          setIsPlaying(false);
        }}
      />

      <div className="player-header">
        <div className="station-info">
          <h2 className="station-name">{stationName}</h2>
          <div className="live-indicator">
            <span className="live-dot"></span>
            LIVE
          </div>
        </div>
        {error && (
          <div className="error-message">
            ℹ️ {error}
          </div>
        )}
      </div>

      <div className="player-controls">
        <button
          className={`play-button ${isPlaying ? 'playing' : ''}`}
          onClick={togglePlay}
          disabled={isLoading}
        >
          {isLoading ? (
            <div className="spinner"></div>
          ) : isPlaying ? (
            <svg viewBox="0 0 24 24" width="48" height="48">
              <rect x="6" y="4" width="4" height="16" fill="currentColor" />
              <rect x="14" y="4" width="4" height="16" fill="currentColor" />
            </svg>
          ) : (
            <svg viewBox="0 0 24 24" width="48" height="48">
              <path d="M8 5v14l11-7z" fill="currentColor" />
            </svg>
          )}
        </button>
      </div>

      <div className="volume-control">
        <svg viewBox="0 0 24 24" width="24" height="24" className="volume-icon">
          <path
            d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"
            fill="currentColor"
          />
        </svg>
        <input
          type="range"
          min="0"
          max="100"
          value={volume}
          onChange={(e) => setVolume(Number(e.target.value))}
          className="volume-slider"
        />
        <span className="volume-value">{volume}%</span>
      </div>
    </div>
  );
};

export default RadioPlayer;
