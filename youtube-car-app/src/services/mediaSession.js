// MediaSession API Integration for Bluetooth Car Controls & Steering Wheel Buttons

export function setupMediaSession({ title, artist, artworkUrl, onPlay, onPause, onNext, onPrevious, onSeek }) {
  if ('mediaSession' in navigator) {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: title || 'YouTube Car Edition',
      artist: artist || 'YouTube Channel',
      album: 'Ad-Free Car Player',
      artwork: artworkUrl ? [
        { src: artworkUrl, sizes: '96x96', type: 'image/jpeg' },
        { src: artworkUrl, sizes: '128x128', type: 'image/jpeg' },
        { src: artworkUrl, sizes: '192x192', type: 'image/jpeg' },
        { src: artworkUrl, sizes: '512x512', type: 'image/jpeg' }
      ] : []
    });

    // Handle Play / Pause from Setir / Car Bluetooth
    try {
      navigator.mediaSession.setActionHandler('play', () => {
        if (onPlay) onPlay();
        navigator.mediaSession.playbackState = 'playing';
      });

      navigator.mediaSession.setActionHandler('pause', () => {
        if (onPause) onPause();
        navigator.mediaSession.playbackState = 'paused';
      });

      if (onNext) {
        navigator.mediaSession.setActionHandler('nexttrack', () => {
          onNext();
        });
      }

      if (onPrevious) {
        navigator.mediaSession.setActionHandler('previoustrack', () => {
          onPrevious();
        });
      }

      navigator.mediaSession.setActionHandler('seekbackward', (details) => {
        if (onSeek) onSeek(-10);
      });

      navigator.mediaSession.setActionHandler('seekforward', (details) => {
        if (onSeek) onSeek(10);
      });

    } catch (error) {
      console.warn('MediaSession handler setup issue:', error);
    }
  }
}

export function updatePlaybackState(state) {
  if ('mediaSession' in navigator) {
    navigator.mediaSession.playbackState = state; // 'playing' | 'paused' | 'none'
  }
}
