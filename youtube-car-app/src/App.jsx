import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import VideoPlayer from './components/VideoPlayer';
import VideoGrid from './components/VideoGrid';
import DrivingModeView from './components/DrivingModeView';
import { getTrendingVideos, searchVideos, getVideoDetails } from './services/youtubeApi';

export default function App() {
  const [videos, setVideos] = useState([]);
  const [currentVideo, setCurrentVideo] = useState(null);
  const [activeCategory, setActiveCategory] = useState('trending');
  const [isDrivingMode, setIsDrivingMode] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // Initial load
  useEffect(() => {
    loadTrending();
  }, []);

  const loadTrending = async () => {
    setIsLoading(true);
    const data = await getTrendingVideos('ID');
    setVideos(data);
    if (data.length > 0 && !currentVideo) {
      handleSelectVideo(data[0]);
    }
    setIsLoading(false);
  };

  const handleSearch = async (query) => {
    setIsLoading(true);
    const data = await searchVideos(query);
    setVideos(data);
    if (data.length > 0) {
      handleSelectVideo(data[0]);
    }
    setIsLoading(false);
  };

  const handleSelectVideo = async (video) => {
    setCurrentVideo(video);
    // Fetch direct ad-free streams in background
    const details = await getVideoDetails(video.id);
    setCurrentVideo((prev) => ({ ...prev, ...details }));
  };

  // Next Track Logic
  const handleNextTrack = () => {
    if (!videos || videos.length === 0) return;
    const currentIndex = videos.findIndex(v => v.id === currentVideo?.id);
    const nextIndex = (currentIndex + 1) % videos.length;
    handleSelectVideo(videos[nextIndex]);
  };

  // Previous Track Logic
  const handlePreviousTrack = () => {
    if (!videos || videos.length === 0) return;
    const currentIndex = videos.findIndex(v => v.id === currentVideo?.id);
    const prevIndex = (currentIndex - 1 + videos.length) % videos.length;
    handleSelectVideo(videos[prevIndex]);
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Navigation Header */}
      <Header
        onSearch={handleSearch}
        activeCategory={activeCategory}
        setActiveCategory={setActiveCategory}
      />

      {/* Main Body */}
      <main style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '24px', maxWidth: '1400px', width: '100%', margin: '0 auto' }}>
        {/* Active Player */}
        {currentVideo && (
          <div style={{ padding: '24px 24px 0 24px' }}>
            <VideoPlayer
              video={currentVideo}
              onNext={handleNextTrack}
              onPrevious={handlePreviousTrack}
              isDrivingMode={isDrivingMode}
              setIsDrivingMode={setIsDrivingMode}
            />
          </div>
        )}

        {/* Video Feed / Grid */}
        <div style={{ flex: 1 }}>
          <h2 style={{ padding: '0 24px', fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-main)' }}>
            {activeCategory === 'trending' ? '🚗 Rekomendasi Musik & Video Perjalanan Mobil' : '🔍 Hasil Pencarian / Kategori'}
          </h2>

          {isLoading ? (
            <div style={{ padding: '60px', textAlign: 'center', color: 'var(--text-muted)' }}>
              <h3>Memuat video ad-free...</h3>
            </div>
          ) : (
            <VideoGrid
              videos={videos}
              onSelectVideo={handleSelectVideo}
              currentVideoId={currentVideo?.id}
            />
          )}
        </div>
      </main>

      {/* Driving Mode Overlay */}
      {isDrivingMode && (
        <DrivingModeView
          video={currentVideo}
          isPlaying={true}
          onTogglePlay={() => {}}
          onNext={handleNextTrack}
          onPrevious={handlePreviousTrack}
          onCloseDrivingMode={() => setIsDrivingMode(false)}
        />
      )}
    </div>
  );
}
